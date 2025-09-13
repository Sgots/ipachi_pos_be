package com.ipachi.pos.sim;

import com.ipachi.pos.model.User;
import com.ipachi.pos.model.UserRole;
import com.ipachi.pos.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class Bootstrap implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Override public void run(String... args) {
        if (users.count() == 0) {
            users.save(User.builder()
                    .username("admin")
                    .email("admin@ipachi.local")
                    .passwordHash(encoder.encode("Admin@123"))
                    .role(UserRole.ADMIN)
                    .enabled(true)
                    .build());
        }
    }
}
