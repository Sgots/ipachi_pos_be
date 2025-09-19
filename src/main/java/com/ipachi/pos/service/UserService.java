// UserService.java - Updated with FileAsset handling
package com.ipachi.pos.service;

import com.ipachi.pos.model.*;
import com.ipachi.pos.repo.*;
import com.ipachi.pos.security.CurrentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private FileAssetRepository fileAssetRepository; // Add this
    @Autowired
    private CurrentRequest currentRequest;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public User getUser(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public UserProfile getUserProfile(Long userId) {
        User user = getUser(userId);
        if (user == null) return null;
        return userProfileRepository.findByUser(user).orElse(null);
    }

    public void updateUserProfile(Long userId, UserProfile updatedProfile) {
        User user = getUser(userId);
        if (user != null && updatedProfile != null) {
            UserProfile profile = userProfileRepository.findByUser(user).orElseGet(() -> {
                UserProfile newProfile = new UserProfile();
                newProfile.setUser(user);
                return newProfile;
            });

            // Update basic fields
            profile.setTitle(updatedProfile.getTitle());
            profile.setGender(updatedProfile.getGender());
            profile.setDob(updatedProfile.getDob());
            profile.setIdType(updatedProfile.getIdType());
            profile.setIdNumber(updatedProfile.getIdNumber());
            profile.setPostalAddress(updatedProfile.getPostalAddress());
            profile.setPhysicalAddress(updatedProfile.getPhysicalAddress());
            profile.setCity(updatedProfile.getCity());
            profile.setCountry(updatedProfile.getCountry());
            profile.setAreaCode(updatedProfile.getAreaCode());
            profile.setPhone(updatedProfile.getPhone());

            // Handle file assets if provided
            if (updatedProfile.getPictureAsset() != null) {
                // Save the asset first to get proper ID
                FileAsset savedAsset = fileAssetRepository.save(updatedProfile.getPictureAsset());
                profile.setPictureAsset(savedAsset);
            }
            if (updatedProfile.getIdDocAsset() != null) {
                FileAsset savedAsset = fileAssetRepository.save(updatedProfile.getIdDocAsset());
                profile.setIdDocAsset(savedAsset);
            }

            userProfileRepository.save(profile);
        }
    }

    // NEW: Create and associate picture asset
    public FileAsset createAndAssociatePictureAsset(Long userId, FileAsset asset) {
        User user = getUser(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // Save the asset
        FileAsset savedAsset = fileAssetRepository.save(asset);

        // Associate with user profile
        UserProfile profile = userProfileRepository.findByUser(user).orElseGet(() -> {
            UserProfile newProfile = new UserProfile();
            newProfile.setUser(user);
            return newProfile;
        });

        profile.setPictureAsset(savedAsset);
        userProfileRepository.save(profile);

        return savedAsset;
    }

    // NEW: Create and associate ID document asset
    public FileAsset createAndAssociateIdDocAsset(Long userId, FileAsset asset) {
        User user = getUser(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // Save the asset
        FileAsset savedAsset = fileAssetRepository.save(asset);

        // Associate with user profile
        UserProfile profile = userProfileRepository.findByUser(user).orElseGet(() -> {
            UserProfile newProfile = new UserProfile();
            newProfile.setUser(user);
            return newProfile;
        });

        profile.setIdDocAsset(savedAsset);
        userProfileRepository.save(profile);

        return savedAsset;
    }

    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUser(userId);
        if (user != null && oldPassword != null && newPassword != null) {
            if (encoder.matches(oldPassword, user.getPasswordHash())) {
                user.setPasswordHash(encoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public void deactivate(Long userId) {
        User user = getUser(userId);
        if (user != null) {
            userRepository.delete(user);
            // Cascade will handle related entities or add cleanup logic
        }
    }

    public User findByEmail(String email) {
        return userRepository.findByUsername(email).orElse(null);
    }

    // Convenience method for current user
    public User getCurrentUser() {
        Long userId = currentRequest.getUserId();
        return userId != null ? getUser(userId) : null;
    }
}