package com.mafe.incidentai.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Entidad que almacena articulos de la base de conocimiento (manuales, guias o soluciones documentadas para el RAG)
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

    // Contenido tecnico consolidado del articulo (incluye problema, sintomas, causa y procedimiento)
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // Campos estructurados especificos de RF02 (opcionales para desglosar el articulo tecnico)
    @Column(name = "problem_description", columnDefinition = "TEXT")
    private String problemDescription;

    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "solution_procedure", columnDefinition = "TEXT")
    private String solutionProcedure;

    @Column(name = "reference_url", length = 500)
    private String referenceUrl;

    // Indica si el articulo sigue vigente y disponible para consulta
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    // Indica si un especialista o lider tecnico ya valido que la solucion del articulo es correcta
    @Column(name = "is_verified", nullable = false)
    private Boolean verified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Dueno de la relacion N:1: Cada articulo pertenece a una categoria tecnica
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Relacion opcional N:1 con el usuario (tecnico o administrador) que redacto o verifico el articulo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private User authorUser;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
