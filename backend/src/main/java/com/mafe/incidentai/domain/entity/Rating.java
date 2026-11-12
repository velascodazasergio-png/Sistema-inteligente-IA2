package com.mafe.incidentai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Entidad para registrar la calificacion y retroalimentacion humana sobre la utilidad de la respuesta de IA
@Entity
@Table(name = "ratings")
@Getter
@Setter
@NoArgsConstructor
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Puntuacion en escala de 1 a 5 estrellas
    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "is_helpful", nullable = false)
    private Boolean helpful;

    @Column(name = "feedback_comment", length = 500)
    private String feedbackComment;

    @Column(name = "rated_at", nullable = false, updatable = false)
    private LocalDateTime ratedAt;

    // Dueno de la relacion 1:1 con AiResponse
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_response_id", nullable = false, unique = true)
    private AiResponse aiResponse;

    // Dueno de la relacion N:1 con User (usuario o tecnico que evalua la respuesta)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluator_user_id", nullable = false)
    private User evaluatorUser;

    @PrePersist
    protected void onCreate() {
        this.ratedAt = LocalDateTime.now();
    }
}

