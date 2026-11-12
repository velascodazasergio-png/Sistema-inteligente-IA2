package com.mafe.incidentai.service;

import com.mafe.incidentai.domain.entity.Category;
import com.mafe.incidentai.domain.entity.KnowledgeArticle;
import com.mafe.incidentai.domain.entity.User;
import com.mafe.incidentai.dto.KnowledgeArticleRequestDTO;
import com.mafe.incidentai.dto.KnowledgeArticleResponseDTO;
import com.mafe.incidentai.dto.UpdateKnowledgeArticleStatusDTO;
import com.mafe.incidentai.exception.BusinessRuleException;
import com.mafe.incidentai.exception.ResourceNotFoundException;
import com.mafe.incidentai.repository.CategoryRepository;
import com.mafe.incidentai.repository.KnowledgeArticleRepository;
import com.mafe.incidentai.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Servicio de la base de conocimiento (RF02): solo los articulos activos y verificados alimentan al RAG en n8n
@Service
@RequiredArgsConstructor
public class KnowledgeArticleService {

    // Inyeccion de dependencias por constructor generada automaticamente por @RequiredArgsConstructor de Lombok
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // Lista articulos con filtros opcionales: texto en titulo o contenido (q), categoria, active y verified
    @Transactional(readOnly = true)
    public List<KnowledgeArticleResponseDTO> searchArticles(String q, Long categoryId, Boolean active, Boolean verified) {
        Specification<KnowledgeArticle> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("content")), like)
                ));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (verified != null) {
                predicates.add(cb.equal(root.get("verified"), verified));
            }
            if (query != null) {
                query.orderBy(cb.desc(root.get("updatedAt")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return knowledgeArticleRepository.findAll(spec).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeArticleResponseDTO getArticle(Long articleId) {
        return toDTO(findArticle(articleId));
    }

    // Crea un articulo nuevo: nace activo (true) y sin verificar (false) hasta que un especialista lo valide
    @Transactional
    public KnowledgeArticleResponseDTO createArticle(KnowledgeArticleRequestDTO request) {
        KnowledgeArticle article = new KnowledgeArticle();
        apply(article, request);
        article.setActive(true);
        article.setVerified(false);
        return toDTO(knowledgeArticleRepository.save(article));
    }

    // Edita un articulo existente: si cambia el titulo o el contenido, vuelve a quedar sin verificar (verified = false)
    @Transactional
    public KnowledgeArticleResponseDTO updateArticle(Long articleId, KnowledgeArticleRequestDTO request) {
        KnowledgeArticle article = findArticle(articleId);
        boolean contentChanged = !Objects.equals(article.getTitle(), request.title().trim())
                || !Objects.equals(article.getContent(), request.content().trim());
        apply(article, request);
        if (contentChanged) {
            article.setVerified(false);
        }
        return toDTO(knowledgeArticleRepository.save(article));
    }

    // Activa/desactiva o verifica un articulo de conocimiento
    @Transactional
    public KnowledgeArticleResponseDTO updateStatus(Long articleId, UpdateKnowledgeArticleStatusDTO request) {
        if (request.active() == null && request.verified() == null) {
            throw new BusinessRuleException("Send at least one field to update: active or verified");
        }
        KnowledgeArticle article = findArticle(articleId);
        if (request.active() != null) {
            article.setActive(request.active());
        }
        if (request.verified() != null) {
            article.setVerified(request.verified());
        }
        return toDTO(knowledgeArticleRepository.save(article));
    }

    private void apply(KnowledgeArticle article, KnowledgeArticleRequestDTO request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.categoryId()));
        boolean categoryChanged = article.getCategory() == null || !category.getId().equals(article.getCategory().getId());
        // Regla de negocio: no se asignan articulos a una categoria inactiva
        if (categoryChanged && Boolean.FALSE.equals(category.getActive())) {
            throw new BusinessRuleException("Cannot assign an article to an inactive category: " + category.getName());
        }
        if (request.authorUserId() != null) {
            User author = userRepository.findById(request.authorUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.authorUserId()));
            article.setAuthorUser(author);
        }
        article.setTitle(request.title().trim());
        article.setContent(request.content().trim());
        article.setReferenceUrl(request.referenceUrl() == null || request.referenceUrl().isBlank()
                ? null
                : request.referenceUrl().trim());
        article.setCategory(category);
    }

    private KnowledgeArticle findArticle(Long articleId) {
        return knowledgeArticleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge article not found with ID: " + articleId));
    }

    private KnowledgeArticleResponseDTO toDTO(KnowledgeArticle a) {
        User author = a.getAuthorUser();
        return new KnowledgeArticleResponseDTO(
                a.getId(),
                a.getTitle(),
                a.getContent(),
                a.getReferenceUrl(),
                a.getActive(),
                a.getVerified(),
                a.getCategory().getId(),
                a.getCategory().getName(),
                author != null ? author.getId() : null,
                author != null ? author.getFullName() : null,
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}

