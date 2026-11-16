package com.mafe.incidentai.domain.entity;

import com.mafe.incidentai.domain.enums.QueryStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Entity that records each analysis request (prompt) sent to n8n for an incident
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

    // Failure code and message when the query ends in COMMUNICATION_ERROR or FAILED
    @Column(name = "failure_code", length = 60)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    // Owning side of the N:1 relationship with Incident
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    // Inverse side of the 1:1 relationship with AiResponse
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
