package com.mafe.incidentai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Entity that links an AI response to its sources or knowledge articles and their similarity score
@Entity
@Table(name = "response_sources")
@Getter
@Setter
@NoArgsConstructor
public class ResponseSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_title", nullable = false, length = 200)
    private String sourceTitle;

    @Column(name = "reference_url", length = 500)
    private String referenceUrl;

    @Column(name = "cited_snippet", columnDefinition = "TEXT")
    private String citedSnippet;

    // Vector/semantic similarity score (0.0 to 1.0) computed by the RAG engine in n8n
    @Column(name = "similarity_score")
    private Double similarityScore;

    // Owning side of the N:1 relationship with AiResponse
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_response_id", nullable = false)
    private AiResponse aiResponse;

    // Optional N:1 relationship with KnowledgeArticle when the source comes from the internal knowledge base
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_article_id")
    private KnowledgeArticle knowledgeArticle;
}
