package com.mafe.incidentai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Entidad que almacena el diagnostico y los pasos de solucion devueltos por el flujo de IA en n8n
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

    // Nivel de certeza del modelo (ej. 0.92 equivale a 92% de confianza)
    @Column(name = "confidence_score")
    private Double confidenceScore;

    // Tiempo total de procesamiento del flujo en n8n expresado en milisegundos
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "responded_at", nullable = false, updatable = false)
    private LocalDateTime respondedAt;

    // Dueno de la relacion 1:1 con AiQuery (unique = true garantiza la relacion 1 a 1 en base de datos)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_query_id", nullable = false, unique = true)
    private AiQuery aiQuery;

    // Relacion 1:N con ResponseSource (fuentes documentales o articulos citados por el RAG en n8n)
    @OneToMany(mappedBy = "aiResponse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResponseSource> sources = new ArrayList<>();

    // Lado inverso de la relacion 1:1 con Rating
    @OneToOne(mappedBy = "aiResponse", cascade = CascadeType.ALL, orphanRemoval = true)
    private Rating rating;

    @PrePersist
    protected void onCreate() {
        this.respondedAt = LocalDateTime.now();
    }

    // Metodo helper para mantener sincronizados ambos lados de la relacion bidireccional en memoria
    public void addSource(ResponseSource source) {
        this.sources.add(source);
        source.setAiResponse(this);
    }
}

