package com.mafe.incidentai.security;

import com.mafe.incidentai.domain.entity.User;
import com.mafe.incidentai.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// On startup, assigns the demo password (DEMO_PASSWORD) to the users that do not have one yet.
// This lets the seed users of data.sql, and those of an existing PostgreSQL database, log in.
// Passwords already set are never changed. With an empty DEMO_PASSWORD it does nothing.
@Slf4j
@Component
public class DemoPasswordInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DemoPasswordInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                   @Value("${app.security.demo-password:}") String demoPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (demoPassword == null || demoPassword.isBlank()) return;
        List<User> withoutPassword = userRepository.findAll().stream()
                .filter(u -> u.getPasswordHash() == null || u.getPasswordHash().isBlank())
                .toList();
        if (withoutPassword.isEmpty()) return;
        // A single BCrypt hash (with its salt) for everyone: computing it is slow on purpose
        String hash = passwordEncoder.encode(demoPassword);
        withoutPassword.forEach(u -> u.setPasswordHash(hash));
        userRepository.saveAll(withoutPassword);
        log.info("Demo password assigned to {} user(s) without a password.", withoutPassword.size());
    }
}
