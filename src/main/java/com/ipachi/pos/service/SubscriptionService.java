// SubscriptionService.java (updated)
package com.ipachi.pos.service;

import com.ipachi.pos.model.*;
import com.ipachi.pos.repo.*;
import com.ipachi.pos.security.CurrentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubscriptionService {
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CurrentRequest currentRequest;

    public Subscription getSubscription(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;
        return subscriptionRepository.findByUser(user).orElse(null);
    }

    public void subscribe(Long userId, String plan) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && plan != null) {
            Subscription sub = subscriptionRepository.findByUser(user).orElseGet(() -> {
                Subscription newSub = new Subscription();
                newSub.setUser(user);
                return newSub;
            });

            sub.setPlan(plan);
            subscriptionRepository.save(sub);

            // TODO: Integrate with payment gateway
            System.out.println("User " + userId + " subscribed to plan: " + plan);
        }
    }

    public Subscription getCurrentSubscription() {
        Long userId = currentRequest.getUserId();
        return userId != null ? getSubscription(userId) : null;
    }
}