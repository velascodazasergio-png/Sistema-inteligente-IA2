package com.mafe.incidentai.domain.entity;

import com.mafe.incidentai.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Entidad que representa a los usuarios del sistema (empleados, tecnicos o administradores)
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

    // Usamos EnumType.STRING para almacenar el nombre del rol en texto y evitar corrupcion si cambia el orden
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private UserRole role;

    // Hash BCrypt de la contrasena (nunca la contrasena en claro). Nulo = aun sin contrasena asignada.
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Lado inverso (1:N): Un usuario puede reportar multiples incidentes
    @OneToMany(mappedBy = "user")
    private List<Incident> reportedIncidents = new ArrayList<>();

    // Se ejecuta automaticamente antes de insertar el registro en la base de datos
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

