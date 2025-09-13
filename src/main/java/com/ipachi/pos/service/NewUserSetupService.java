package com.ipachi.pos.service;


import com.ipachi.pos.dto.NewUserSetupRequest;
import com.ipachi.pos.dto.NewUserSetupResponse;
import com.ipachi.pos.model.BusinessProfile;
import com.ipachi.pos.model.FileAsset;
import com.ipachi.pos.model.User;
import com.ipachi.pos.model.UserProfile;
import com.ipachi.pos.repo.*;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class NewUserSetupService {
    private final FileStorageDbService storage;

    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final BusinessProfileRepository businesses;

// ...

    @Transactional
    public NewUserSetupResponse submit(String username,
                                       NewUserSetupRequest req,
                                       MultipartFile picture,
                                       MultipartFile idDoc,
                                       MultipartFile bizLogo) throws Exception {
        // 1) Load user and upload files (if any)
        User user = users.findByUsername(username).orElseThrow();

        FileAsset pic  = storage.save(picture);
        FileAsset idd  = storage.save(idDoc);
        FileAsset logo = storage.save(bizLogo);

        // 2) Upsert profile and copy scalars from req
        UserProfile profile = profiles.findByUser(user)
                .orElse(UserProfile.builder().user(user).build());

        profile.setTitle(req.title());
        profile.setGender(req.gender());
        profile.setDob(req.dob());                  // LocalDate if your record uses LocalDate
        profile.setIdType(req.idType());            // enum IdType
        profile.setIdNumber(trim(req.idNumber()));
        profile.setPostalAddress(trim(req.postalAddress()));
        profile.setPhysicalAddress(trim(req.physicalAddress()));
        profile.setCity(trim(req.city()));
        profile.setCountry(trim(req.country()));
        profile.setAreaCode(trim(req.areaCode()));
        profile.setPhone(trim(req.phone()));

        if (pic != null) profile.setPictureAsset(pic);
        if (idd != null) profile.setIdDocAsset(idd);
        profiles.save(profile);

        // 3) Upsert business and copy scalars from req (guard required fields)
        if (!StringUtils.hasText(req.bizName())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Business name is required");
        }
        if (!StringUtils.hasText(req.bizLocation())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Business location is required");
        }

        BusinessProfile biz = businesses.findByUser(user)
                .orElse(BusinessProfile.builder().user(user).build());

        biz.setName(req.bizName().trim());
        biz.setLocation(req.bizLocation().trim());
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

    // tiny helper
    private static String trim(String s) { return s == null ? null : s.trim(); }


    @Transactional
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

            if (b != null) {
                if (b.getLogoAsset() != null) {
                    logoUrl = "/files/" + b.getLogoAsset().getId();

                }
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

                /**
                 * Tiny helper to avoid compile errors if legacy getters were dropped.
                 * Remove this (and the hasMethod(...) calls) once legacy URL fields are gone.
                 */

    }
