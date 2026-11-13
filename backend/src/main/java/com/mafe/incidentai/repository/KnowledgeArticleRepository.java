package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// Repositorio para consultar articulos de la base de conocimiento
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long>, JpaSpecificationExecutor<KnowledgeArticle> {

    // Consulta JPQL explicita que trae solo los articulos activos y verificados (listos para alimentar el RAG de n8n)
    @Query("""
            SELECT ka
            FROM KnowledgeArticle ka
            WHERE ka.active = true
              AND ka.verified = true
            ORDER BY ka.updatedAt DESC
            """)
    List<KnowledgeArticle> findActiveAndVerifiedArticles();

    // Consulta derivada equivalente en Spring Data JPA
    List<KnowledgeArticle> findByActiveTrueAndVerifiedTrue();

    // Consulta articulos activos y verificados filtrando ademas por una categoria especifica
    List<KnowledgeArticle> findByCategoryIdAndActiveTrueAndVerifiedTrue(Long categoryId);

    // Busqueda por palabra clave en el titulo solo entre articulos activos y verificados
    List<KnowledgeArticle> findByTitleContainingIgnoreCaseAndActiveTrueAndVerifiedTrue(String keyword);
}
