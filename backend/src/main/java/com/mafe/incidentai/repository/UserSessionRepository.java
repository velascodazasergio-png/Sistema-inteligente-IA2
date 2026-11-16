package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

// Repository for the login sessions
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    // true if the session exists, was not closed and has not expired
    boolean existsByIdAndRevokedAtIsNullAndExpiresAtAfter(String id, LocalDateTime now);
}
