package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repositorio para operaciones CRUD y busquedas sobre la entidad User
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

