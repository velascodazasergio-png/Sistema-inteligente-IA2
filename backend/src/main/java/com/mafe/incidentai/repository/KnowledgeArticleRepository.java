package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// Repository to read knowledge base articles
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long>, JpaSpecificationExecutor<KnowledgeArticle> {

    // Explicit JPQL query that returns only active and verified articles (ready to feed the n8n RAG)
    @Query("""
            SELECT ka
            FROM KnowledgeArticle ka
            WHERE ka.active = true
              AND ka.verified = true
            ORDER BY ka.updatedAt DESC
            """)
    List<KnowledgeArticle> findActiveAndVerifiedArticles();

    // Equivalent Spring Data JPA derived query
    List<KnowledgeArticle> findByActiveTrueAndVerifiedTrue();

    // Active and verified articles, also filtered by a specific category
    List<KnowledgeArticle> findByCategoryIdAndActiveTrueAndVerifiedTrue(Long categoryId);

    // Keyword search in the title, only among active and verified articles
    List<KnowledgeArticle> findByTitleContainingIgnoreCaseAndActiveTrueAndVerifiedTrue(String keyword);
}
