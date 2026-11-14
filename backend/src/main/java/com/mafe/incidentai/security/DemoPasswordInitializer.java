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

// Al arrancar, asigna la contrasena de demostracion (DEMO_PASSWORD) a los usuarios que aun no tienen.
// Asi los usuarios semilla de data.sql, y los de una base PostgreSQL ya existente, pueden iniciar sesion.
// No cambia contrasenas ya asignadas. Con DEMO_PASSWORD vacio no hace nada.
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
        // Un solo hash BCrypt (con su sal) para todos: calcularlo es lento a proposito
        String hash = passwordEncoder.encode(demoPassword);
        withoutPassword.forEach(u -> u.setPasswordHash(hash));
        userRepository.saveAll(withoutPassword);
        log.info("Contrasena de demostracion asignada a {} usuario(s) sin contrasena.", withoutPassword.size());
    }
}
