package com.ipachi.pos.repo;



import com.ipachi.pos.model.User;
import com.ipachi.pos.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUser(User user);
    boolean existsByUser(User user);
}
