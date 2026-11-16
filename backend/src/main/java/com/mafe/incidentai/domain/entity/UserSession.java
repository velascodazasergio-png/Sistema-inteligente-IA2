package com.mafe.incidentai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Login session stored in the database. The JWT only carries its id (jti): every request checks
// that the session still exists, has not expired and was not closed (logout).
@Entity
@Table(
    name = "user_sessions",
    indexes = {
        @Index(name = "idx_user_sessions_user_id", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class UserSession {

    // Random UUID, also used as the jti claim of the JWT
    @Id
    @Column(name = "id", length = 36)
    private String id;

    // Owning side of the N:1 relationship with User
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Set when the user logs out; a revoked session no longer authenticates requests
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    // Browser or client that opened the session (for auditing)
    @Column(name = "user_agent", length = 255)
    private String userAgent;
}
