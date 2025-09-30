package com.ipachi.pos.service;

import com.ipachi.pos.model.OtpEntry;
import com.ipachi.pos.repo.OtpRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository repo;
    private final RestTemplate http;

    @Value("${otp.ttl-seconds:300}")
    private long ttlSeconds;

    @Value("${otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${otp.sms.base-url}")
    private String smsBase;

    @Value("${otp.sms.username}")
    private String smsUser;

    @Value("${otp.sms.password}")
    private String smsPass;

    @Value("${otp.sms.sender:}")
    private String originator; // optional

    @Value("${otp.sms.template:Your Ipachi registration OTP is {code}}")
    private String template;

    private static final SecureRandom RND = new SecureRandom();

    /** Generate a random 4-digit OTP (change to 6 if you prefer). */
    public String generateCode() {
        return String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
    }

    /** Create + store a new OTP, then send over SMS. */
    @Transactional
    public void request(String phone) {
        final String normalized = normalizePhone(phone);
        final String code = generateCode();
        final String hash = BCrypt.hashpw(code, BCrypt.gensalt());

        OtpEntry entry = new OtpEntry();
        entry.setPhone(normalized);
        entry.setCodeHash(hash);
        entry.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        entry.setAttempts(0);
        entry.setVerified(false);
        repo.save(entry);

        final String body = template.replace("{code}", code);
        sendSms(normalized, body);
    }

    /** Strict verification endpoint (throws on failure). */
    @Transactional
    public void verify(String phone, String code) {
        final String normalized = normalizePhone(phone);

        OtpEntry entry = repo.findTopByPhoneOrderByCreatedAtDesc(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No OTP requested"));

        if (entry.isVerified())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already verified");

        if (Instant.now().isAfter(entry.getExpiresAt())) {
            bumpAttempts(entry);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP expired");
        }

        if (entry.getAttempts() >= maxAttempts)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts");

        entry.setAttempts(entry.getAttempts() + 1);

        if (!BCrypt.checkpw(code, entry.getCodeHash())) {
            repo.save(entry);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect OTP");
        }

        entry.setVerified(true);
        repo.save(entry);
    }

    /** Boolean-style verification for registration gate. */
    @Transactional
    public boolean verifyAndConsume(String phone, String code) {
        final String normalized = normalizePhone(phone);

        Optional<OtpEntry> opt = repo.findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(normalized);
        if (opt.isEmpty()) return false;

        OtpEntry entry = opt.get();

        if (entry.getAttempts() >= maxAttempts) return false;

        if (entry.getExpiresAt() != null && entry.getExpiresAt().isBefore(Instant.now())) {
            bumpAttempts(entry);
            return false;
        }

        if (!BCrypt.checkpw(code, entry.getCodeHash())) {
            bumpAttempts(entry);
            return false;
        }

        entry.setVerified(true);
        entry.setAttempts(entry.getAttempts() + 1);
        repo.save(entry);
        return true;
    }

    private void bumpAttempts(OtpEntry e) {
        e.setAttempts(e.getAttempts() + 1);
        repo.save(e);
    }

    /**
     * Build the GET URL using plain values and let Spring do the encoding:
     * - Spaces → %20
     * - '+' in MSISDN → %2B
     * Gateway will decode them back to human-readable text.
     */
    private void sendSms(String recipient, String message) {
        try {
            // Build the base URL with required parameters, encoding them individually
            UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(smsBase)
                    .queryParam("action", "sendmessage")
                    .queryParam("username", smsUser)
                    .queryParam("password", smsPass)
                    .queryParam("recipient", recipient)
                    .queryParam("messagetype", "SMS:TEXT");

            if (originator != null && !originator.isBlank()) {
                b.queryParam("originator", originator);
            }

            // Encode the URL without messagedata first
            String baseUrl = b.build().encode().toUriString();

            // Append messagedata without encoding spaces
            // Use a simple replacement to ensure spaces remain as spaces
            String finalUrl = baseUrl + "&messagedata=" + message.replace(" ", "+");

            ResponseEntity<String> resp = http.getForEntity(finalUrl, String.class);
            log.info("SMS gateway HTTP {}. Body: {}", resp.getStatusCode(), resp.getBody());

            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "SMS API HTTP status: " + resp.getStatusCode());
            }

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to send OTP SMS: " + e.getMessage(), e);
        }
    }
    /** Normalize to E.164 (+XXXXXXXXX). */
    private static String normalizePhone(String phone) {
        if (phone == null) return null;
        String p = phone.trim().replace(" ", "");
        if (!p.startsWith("+")) p = "+" + p;
        return p;
    }

    /** Convenience if you collect area code + local separately. */
    public static String normalizePhone(String areaCode, String local) {
        String ac = (areaCode == null ? "" : areaCode.trim()).replaceAll("[^\\d+]", "");
        String lp = (local == null ? "" : local.trim()).replaceAll("[^\\d]", "");
        if (!ac.startsWith("+")) ac = "+" + ac;
        return ac + lp;
    }
}
