package com.ipachi.pos.repo;

import java.util.Optional;

import com.ipachi.pos.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // Added for case-insensitive username checks
    boolean existsByUsernameIgnoreCase(String username);
}
