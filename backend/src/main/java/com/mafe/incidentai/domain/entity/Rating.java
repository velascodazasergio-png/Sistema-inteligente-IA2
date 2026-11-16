package com.mafe.incidentai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Entity that records the human rating and feedback on how useful an AI response was
@Entity
@Table(name = "ratings")
@Getter
@Setter
@NoArgsConstructor
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Score on a 1 to 5 star scale
    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "is_helpful", nullable = false)
    private Boolean helpful;

    @Column(name = "feedback_comment", length = 500)
    private String feedbackComment;

    @Column(name = "rated_at", nullable = false, updatable = false)
    private LocalDateTime ratedAt;

    // Owning side of the 1:1 relationship with AiResponse
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_response_id", nullable = false, unique = true)
    private AiResponse aiResponse;

    // Owning side of the N:1 relationship with User (the user or technician who rates the response)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluator_user_id", nullable = false)
    private User evaluatorUser;

    @PrePersist
    protected void onCreate() {
        this.ratedAt = LocalDateTime.now();
    }
}

