package com.mafe.incidentai.domain.entity;

import com.mafe.incidentai.domain.enums.IncidentStatus;
import com.mafe.incidentai.domain.enums.Priority;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Entidad central que representa un incidente tecnologico reportado en la organizacion
@Entity
@Table(
    name = "incidents",
    indexes = {
        @Index(name = "idx_incidents_category_id", columnList = "category_id"),
        @Index(name = "idx_incidents_user_id", columnList = "user_id"),
        @Index(name = "idx_incidents_priority", columnList = "priority"),
        @Index(name = "idx_incidents_status", columnList = "status"),
        @Index(name = "idx_incidents_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private IncidentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Fecha de resolucion del incidente (se registra cuando pasa a estado RESOLVED o CLOSED)
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // Dueno de la relacion N:1 con User (FetchType.LAZY evita el problema de consultas N+1)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Dueno de la relacion N:1 con Category
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Lado inverso 1:N: Un incidente puede generar varias consultas hacia el agente de IA en n8n
    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiQuery> aiQueries = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = IncidentStatus.OPEN;
        }
        if ((this.status == IncidentStatus.RESOLVED || this.status == IncidentStatus.CLOSED) && this.resolvedAt == null) {
            this.resolvedAt = this.updatedAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if ((this.status == IncidentStatus.RESOLVED || this.status == IncidentStatus.CLOSED) && this.resolvedAt == null) {
            this.resolvedAt = this.updatedAt;
        }
    }
}
