package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repositorio para consultar articulos de la base de conocimiento por categoria o palabra clave
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {
    List<KnowledgeArticle> findByCategoryId(Long categoryId);
    List<KnowledgeArticle> findByTitleContainingIgnoreCase(String keyword);
}

