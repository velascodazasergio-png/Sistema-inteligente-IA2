package com.mafe.incidentai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Entidad que vincula una respuesta de la IA con las fuentes o articulos de conocimiento y su puntaje de similitud
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

    // Puntaje de similitud vectorial/semantica (entre 0.0 y 1.0) calculado por el motor RAG en n8n
    @Column(name = "similarity_score")
    private Double similarityScore;

    // Dueno de la relacion N:1 con AiResponse
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_response_id", nullable = false)
    private AiResponse aiResponse;

    // Relacion opcional N:1 con KnowledgeArticle cuando la fuente proviene de la base de conocimiento interna
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_article_id")
    private KnowledgeArticle knowledgeArticle;
}
