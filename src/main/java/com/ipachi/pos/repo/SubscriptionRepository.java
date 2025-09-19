// SubscriptionRepository.java
package com.ipachi.pos.repo;

import com.ipachi.pos.model.Subscription;
import com.ipachi.pos.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUser(User user);
}