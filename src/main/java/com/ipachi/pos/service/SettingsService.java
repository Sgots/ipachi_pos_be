// SettingsService.java (updated)
package com.ipachi.pos.service;

import com.ipachi.pos.model.*;
import com.ipachi.pos.repo.*;
import com.ipachi.pos.security.CurrentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SettingsService {
    @Autowired
    private SettingsRepository settingsRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CurrentRequest currentRequest;

    public Settings getSettings(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;
        return settingsRepository.findByUser(user).orElse(null);
    }

    public void updateSettings(Long userId, Settings updatedSettings) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && updatedSettings != null) {
            Settings settings = settingsRepository.findByUser(user).orElseGet(() -> {
                Settings newSettings = new Settings();
                newSettings.setUser(user);
                return newSettings;
            });

            settings.setCurrency(updatedSettings.getCurrency());
            settings.setAbbreviation(updatedSettings.getAbbreviation());
            settings.setVat(updatedSettings.getVat());
            settings.setApplyVat(updatedSettings.isApplyVat());

            settingsRepository.save(settings);
        }
    }

    public Settings getCurrentSettings() {
        Long userId = currentRequest.getUserId();
        return userId != null ? getSettings(userId) : null;
    }
}