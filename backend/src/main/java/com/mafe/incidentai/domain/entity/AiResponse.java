package com.mafe.incidentai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Entity that stores the diagnosis and solution steps returned by the AI workflow in n8n
@Entity
@Table(name = "ai_responses")
@Getter
@Setter
@NoArgsConstructor
public class AiResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "suggested_diagnosis", nullable = false, columnDefinition = "TEXT")
    private String suggestedDiagnosis;

    @Column(name = "solution_steps", columnDefinition = "TEXT")
    private String solutionSteps;

    // Model confidence level (e.g. 0.92 means 92% confidence)
    @Column(name = "confidence_score")
    private Double confidenceScore;

    // Total processing time of the n8n workflow, in milliseconds
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "responded_at", nullable = false, updatable = false)
    private LocalDateTime respondedAt;

    // Owning side of the 1:1 relationship with AiQuery (unique = true enforces 1:1 in the database)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_query_id", nullable = false, unique = true)
    private AiQuery aiQuery;

    // 1:N relationship with ResponseSource (documents or articles cited by the RAG in n8n)
    @OneToMany(mappedBy = "aiResponse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResponseSource> sources = new ArrayList<>();

    // Inverse side of the 1:1 relationship with Rating
    @OneToOne(mappedBy = "aiResponse", cascade = CascadeType.ALL, orphanRemoval = true)
    private Rating rating;

    @PrePersist
    protected void onCreate() {
        this.respondedAt = LocalDateTime.now();
    }

    // Helper that keeps both sides of the bidirectional relationship in sync in memory
    public void addSource(ResponseSource source) {
        this.sources.add(source);
        source.setAiResponse(this);
    }
}

