package com.mafe.incidentai.controller;

import com.mafe.incidentai.security.AccessPolicy;
import com.mafe.incidentai.dto.KnowledgeArticleRequestDTO;
import com.mafe.incidentai.dto.KnowledgeArticleResponseDTO;
import com.mafe.incidentai.dto.UpdateKnowledgeArticleStatusDTO;
import com.mafe.incidentai.service.KnowledgeArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// REST controller for the knowledge base (RF02)
@RestController
@RequestMapping("/api/v1/knowledge-articles")
@RequiredArgsConstructor
@Tag(name = "Knowledge Base (RF02)", description = "Technical articles. Only active and verified ones are used as sources by the AI assistant.")
public class KnowledgeArticleController {

    private final KnowledgeArticleService knowledgeArticleService;
    private final AccessPolicy accessPolicy;

    // GET /knowledge-articles: search with optional filters
    @Operation(summary = "Search knowledge articles", description = "Optional filters: q (text in the title or content), categoryId, active and verified.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Articles that match the filters, newest first"),
        @ApiResponse(responseCode = "400", description = "Filter with an invalid format")
    })
    @GetMapping
    public ResponseEntity<List<KnowledgeArticleResponseDTO>> searchArticles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean verified) {
        // An employee only sees published articles (active and verified). n8n (no user) is not filtered.
        if (accessPolicy.isAuthenticatedUser() && accessPolicy.isEmployee()) {
            active = true;
            verified = true;
        }
        return ResponseEntity.ok(knowledgeArticleService.searchArticles(q, categoryId, active, verified));
    }

    // GET /knowledge-articles/{id}: one article (employees only see published ones)
    @Operation(summary = "Get a knowledge article by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Article detail"),
        @ApiResponse(responseCode = "404", description = "Article not found")
    })
    @GetMapping("/{articleId}")
    public ResponseEntity<KnowledgeArticleResponseDTO> getArticle(@PathVariable Long articleId) {
        KnowledgeArticleResponseDTO article = knowledgeArticleService.getArticle(articleId);
        if (accessPolicy.isAuthenticatedUser()) accessPolicy.checkArticle(article);
        return ResponseEntity.ok(article);
    }

    // POST /knowledge-articles: creates an article (active, unverified)
    @Operation(summary = "Create a knowledge article", description = "The article starts active and unverified.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Article created"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Category or author not found"),
        @ApiResponse(responseCode = "409", description = "The category is inactive")
    })
    @PostMapping
    public ResponseEntity<KnowledgeArticleResponseDTO> createArticle(@Valid @RequestBody KnowledgeArticleRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(knowledgeArticleService.createArticle(request));
    }

    // PUT /knowledge-articles/{id}: edits an article
    @Operation(summary = "Update a knowledge article", description = "If the title or the content changes, the article becomes unverified again.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Article updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Article, category or author not found"),
        @ApiResponse(responseCode = "409", description = "The new category is inactive")
    })
    @PutMapping("/{articleId}")
    public ResponseEntity<KnowledgeArticleResponseDTO> updateArticle(
            @PathVariable Long articleId,
            @Valid @RequestBody KnowledgeArticleRequestDTO request) {
        return ResponseEntity.ok(knowledgeArticleService.updateArticle(articleId, request));
    }

    // PATCH /knowledge-articles/{id}/status: activates, deactivates or verifies an article
    @Operation(summary = "Activate, deactivate or verify a knowledge article", description = "Omitted fields do not change.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "404", description = "Article not found"),
        @ApiResponse(responseCode = "409", description = "Neither active nor verified was sent")
    })
    @PatchMapping("/{articleId}/status")
    public ResponseEntity<KnowledgeArticleResponseDTO> updateStatus(
            @PathVariable Long articleId,
            @RequestBody UpdateKnowledgeArticleStatusDTO request) {
        return ResponseEntity.ok(knowledgeArticleService.updateStatus(articleId, request));
    }
}
