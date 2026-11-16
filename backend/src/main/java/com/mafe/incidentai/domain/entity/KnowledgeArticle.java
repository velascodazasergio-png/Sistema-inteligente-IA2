package com.mafe.incidentai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Entity that stores knowledge base articles (manuals, guides or documented solutions for the RAG)
@Entity
@Table(
    name = "knowledge_articles",
    indexes = {
        @Index(name = "idx_kb_category_id", columnList = "category_id"),
        @Index(name = "idx_kb_active_verified", columnList = "is_active, is_verified")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    // Full text of the article. The n8n RAG workflow uses it to index and cite.
    // For new articles it is built from problem, symptoms, cause and solution.
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // Structured sections of the article (RF02). Optional for older articles.
    @Column(name = "problem", columnDefinition = "TEXT")
    private String problem;

    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "cause", columnDefinition = "TEXT")
    private String cause;

    @Column(name = "solution", columnDefinition = "TEXT")
    private String solution;

    @Column(name = "reference_url", length = 500)
    private String referenceUrl;

    // Whether the article is still current and available
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    // Whether a specialist or technical lead has confirmed that the solution is correct
    @Column(name = "is_verified", nullable = false)
    private Boolean verified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Owning side of the N:1 relationship: each article belongs to a technical category
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Optional N:1 relationship with the user (technician or administrator) who wrote or verified the article
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private User authorUser;

    // Sets the creation and update dates before the first insert
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Refreshes the update date on every change
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
