package com.mafe.incidentai.domain.entity;

import com.mafe.incidentai.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Entity that represents the system users (employees, technicians or administrators)
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 120)
    private String email;

    // EnumType.STRING stores the role name as text, so reordering the enum cannot corrupt data
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private UserRole role;

    // BCrypt hash of the password (never the plain password). Null = no password assigned yet.
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Inverse side (1:N): a user can report many incidents
    @OneToMany(mappedBy = "user")
    private List<Incident> reportedIncidents = new ArrayList<>();

    // Runs automatically before the record is inserted into the database
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

