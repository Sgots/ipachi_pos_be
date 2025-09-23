// BusinessService.java - Updated with FileAsset handling
package com.ipachi.pos.service;

import com.ipachi.pos.model.*;
import com.ipachi.pos.repo.*;
import com.ipachi.pos.security.CurrentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BusinessService {
    @Autowired
    private BusinessProfileRepository businessProfileRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FileAssetRepository fileAssetRepository;
    @Autowired
    private CurrentRequest currentRequest;

    public BusinessProfile getBusinessProfile(Long userId) {

        return businessProfileRepository.findById(userId).orElse(null);
    }

    public void updateBusinessProfile(Long userId, BusinessProfile updatedProfile) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && updatedProfile != null) {
            BusinessProfile profile = businessProfileRepository.findByUser(user).orElseGet(() -> {
                BusinessProfile newProfile = new BusinessProfile();
                newProfile.setUser(user);
                return newProfile;
            });

            profile.setName(updatedProfile.getName());
            profile.setLocation(updatedProfile.getLocation());

            // Handle logo asset if provided
            if (updatedProfile.getLogoAsset() != null) {
                FileAsset savedAsset = fileAssetRepository.save(updatedProfile.getLogoAsset());
                profile.setLogoAsset(savedAsset);
            }

            businessProfileRepository.save(profile);
        }
    }

    // NEW: Create and associate logo asset
    public FileAsset createAndAssociateLogoAsset(Long userId, FileAsset asset) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // Save the asset
        FileAsset savedAsset = fileAssetRepository.save(asset);

        // Associate with business profile
        BusinessProfile profile = businessProfileRepository.findByUser(user).orElseGet(() -> {
            BusinessProfile newProfile = new BusinessProfile();
            newProfile.setUser(user);
            return newProfile;
        });

        profile.setLogoAsset(savedAsset);
        businessProfileRepository.save(profile);

        return savedAsset;
    }

    public BusinessProfile getCurrentBusinessProfile() {
        Long userId = currentRequest.getUserId();
        return userId != null ? getBusinessProfile(userId) : null;
    }
}