package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repository for CRUD operations and lookups on the User entity
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

