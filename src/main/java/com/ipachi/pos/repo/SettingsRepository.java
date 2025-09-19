package com.ipachi.pos.repo;

import com.ipachi.pos.model.Settings;
import com.ipachi.pos.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SettingsRepository extends JpaRepository<Settings, Long> {
    Optional<Settings> findByUser(User user);
}