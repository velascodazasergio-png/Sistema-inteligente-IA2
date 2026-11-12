package com.mafe.incidentai.domain.entity;

import com.mafe.incidentai.domain.enums.QueryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Entidad que registra cada solicitud de analisis (prompt) enviada hacia n8n para un incidente
@Entity
@Table(
    name = "ai_queries",
    indexes = {
        @Index(name = "idx_ai_queries_incident_id", columnList = "incident_id"),
        @Index(name = "idx_ai_queries_status", columnList = "query_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class AiQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sent_prompt", nullable = false, columnDefinition = "TEXT")
    private String sentPrompt;

    @Enumerated(EnumType.STRING)
    @Column(name = "query_status", nullable = false, length = 30)
    private QueryStatus queryStatus;

    @Column(name = "queried_at", nullable = false, updatable = false)
    private LocalDateTime queriedAt;

    // Dueno de la relacion N:1 con Incident
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    // Lado inverso de la relacion 1:1 con AiResponse
    @OneToOne(mappedBy = "aiQuery", cascade = CascadeType.ALL, orphanRemoval = true)
    private AiResponse aiResponse;

    @PrePersist
    protected void onCreate() {
        this.queriedAt = LocalDateTime.now();
        if (this.queryStatus == null) {
            this.queryStatus = QueryStatus.PENDING;
        }
    }
}
