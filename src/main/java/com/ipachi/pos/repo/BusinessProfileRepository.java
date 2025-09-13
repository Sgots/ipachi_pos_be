package com.ipachi.pos.repo;


import com.ipachi.pos.model.BusinessProfile;
import com.ipachi.pos.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {
    Optional<BusinessProfile> findByUser(User user);
    boolean existsByUser(User user);
}
