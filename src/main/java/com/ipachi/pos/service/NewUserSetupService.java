package com.ipachi.pos.service;

import com.ipachi.pos.dto.NewUserSetupRequest;
import com.ipachi.pos.dto.NewUserSetupResponse;
import com.ipachi.pos.model.BusinessProfile;
import com.ipachi.pos.model.FileAsset;
import com.ipachi.pos.model.User;
import com.ipachi.pos.model.UserProfile;
import com.ipachi.pos.repo.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Fixed import
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NewUserSetupService {
    private static final Logger log = LoggerFactory.getLogger(NewUserSetupService.class);

    // Move EntityManager declaration to the top with other fields
    @PersistenceContext
    private EntityManager entityManager;

    private final FileStorageDbService storage;
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final BusinessProfileRepository businesses;

    @Transactional
    public NewUserSetupResponse submit(String username,
                                       NewUserSetupRequest req,
                                       MultipartFile picture,
                                       MultipartFile idDoc,
                                       MultipartFile bizLogo) throws Exception {
        // 1) Load user and ensure it's managed
        User user = users.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        log.info("Loaded user with ID: {}", user.getId());

        if (user.getId() == null) {
            throw new IllegalStateException("User ID is null. Ensure the user is persisted.");
        }

        // Check if user is managed and reattach if necessary
        if (!entityManager.contains(user)) {
            log.info("User is detached, merging...");
            user = entityManager.merge(user);
            log.info("User reattached, ID: {}", user.getId());
        }

        entityManager.flush(); // Ensure User is synchronized


        FileAsset pic = storage.save(picture, user.getId());
        FileAsset idd = storage.save(idDoc, user.getId());
        FileAsset logo = storage.save(bizLogo, user.getId());

        // 2) Upsert profile and copy scalars from req
        User finalUser = user;
        UserProfile profile = profiles.findByUser(user)
                .orElseGet(() -> {
                    log.info("Creating new UserProfile for user ID: {}", finalUser.getId());
                    return UserProfile.builder().user(finalUser).build();
                });

        // Ensure user is set (redundant but safe)
        if (profile.getUser() == null) {
            profile.setUser(user);
            log.info("Explicitly set user in existing profile");
        }

        profile.setTitle(req.title());
        profile.setGender(req.gender());
        profile.setDob(req.dob());
        profile.setIdType(req.idType());
        profile.setIdNumber(trim(req.idNumber()));
        profile.setPostalAddress(trim(req.postalAddress()));
        profile.setPhysicalAddress(trim(req.physicalAddress()));
        profile.setCity(trim(req.city()));
        profile.setCountry(trim(req.country()));
        profile.setAreaCode(trim(req.areaCode()));
        profile.setPhone(trim(req.phone()));

        if (pic != null) profile.setPictureAsset(pic);
        if (idd != null) profile.setIdDocAsset(idd);

        log.info("UserProfile User ID before save: {}", profile.getUser() != null ? profile.getUser().getId() : "null");

        // Use saveAndFlush for immediate persistence and error detection
        try {
            UserProfile savedProfile = profiles.saveAndFlush(profile);
            log.info("Successfully saved UserProfile with ID: {}", savedProfile.getId());
        } catch (Exception e) {
            log.error("Failed to save UserProfile: {}", e.getMessage(), e);
            throw e;
        }

        // 3) Upsert business and copy scalars from req (guard required fields)
        if (!StringUtils.hasText(req.bizName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business name is required");
        }
        if (!StringUtils.hasText(req.bizLocation())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business location is required");
        }

        User finalUser1 = user;
        BusinessProfile biz = businesses.findByUser(user)
                .orElseGet(() -> {
                    log.info("Creating new BusinessProfile for user ID: {}", finalUser1.getId());
                    return BusinessProfile.builder().user(finalUser1).build();
                });

        biz.setName(req.bizName().trim());
        biz.setLocation(req.bizLocation().trim());
        biz.setUser(user);
        if (logo != null) biz.setLogoAsset(logo);
        businesses.save(biz);

        // 4) Build response
        return new NewUserSetupResponse(
                new NewUserSetupResponse.Profile(
                        profile.getTitle(),
                        profile.getGender(),
                        profile.getDob() == null ? null : profile.getDob().toString(),
                        profile.getIdType() == null ? null : profile.getIdType().name(),
                        profile.getIdNumber(),
                        profile.getPostalAddress(),
                        profile.getPhysicalAddress(),
                        profile.getCity(),
                        profile.getCountry(),
                        profile.getAreaCode(),
                        profile.getPhone(),
                        profile.getPictureAsset() == null ? null : ("/files/" + profile.getPictureAsset().getId()),
                        profile.getIdDocAsset() == null ? null : ("/files/" + profile.getIdDocAsset().getId())
                ),
                new NewUserSetupResponse.Business(
                        biz.getName(),
                        biz.getLocation(),
                        biz.getLogoAsset() == null ? null : ("/files/" + biz.getLogoAsset().getId())
                )
        );
    }

    // Fixed read method - missing closing brace
    @Transactional(readOnly = true)
    public NewUserSetupResponse read(String username) {
        User user = users.findByUsername(username).orElseThrow();
        var p = profiles.findByUser(user).orElse(null);
        var b = businesses.findByUser(user).orElse(null);

        // Resolve file URLs from DB assets first, fall back to legacy URL strings if present
        String pictureUrl = null;
        String idDocUrl = null;
        String logoUrl = null;

        if (p != null) {
            if (p.getPictureAsset() != null) {
                pictureUrl = "/files/" + p.getPictureAsset().getId();
            }

            if (p.getIdDocAsset() != null) {
                idDocUrl = "/files/" + p.getIdDocAsset().getId();
            }
        }

        if (b != null) {
            if (b.getLogoAsset() != null) {
                logoUrl = "/files/" + b.getLogoAsset().getId();
            }
        }

        return new NewUserSetupResponse(
                p == null ? null : new NewUserSetupResponse.Profile(
                        p.getTitle(),
                        p.getGender(),
                        p.getDob() == null ? null : p.getDob().toString(),
                        p.getIdType() == null ? null : p.getIdType().name(),
                        p.getIdNumber(),
                        p.getPostalAddress(),
                        p.getPhysicalAddress(),
                        p.getCity(),
                        p.getCountry(),
                        p.getAreaCode(),
                        p.getPhone(),
                        pictureUrl,
                        idDocUrl
                ),
                b == null ? null : new NewUserSetupResponse.Business(
                        b.getName(),
                        b.getLocation(),
                        logoUrl
                )
        );
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}