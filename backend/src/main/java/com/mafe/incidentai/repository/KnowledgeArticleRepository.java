package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repositorio para consultar articulos de la base de conocimiento
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {

    // Consulta derivada que retorna unicamente los articulos activos y verificados (listos para usar por la IA)
    List<KnowledgeArticle> findByActiveTrueAndVerifiedTrue();

    // Consulta articulos activos y verificados filtrando ademas por una categoria especifica
    List<KnowledgeArticle> findByCategoryIdAndActiveTrueAndVerifiedTrue(Long categoryId);

    // Busqueda por palabra clave en el titulo solo entre articulos activos y verificados
    List<KnowledgeArticle> findByTitleContainingIgnoreCaseAndActiveTrueAndVerifiedTrue(String keyword);
}
