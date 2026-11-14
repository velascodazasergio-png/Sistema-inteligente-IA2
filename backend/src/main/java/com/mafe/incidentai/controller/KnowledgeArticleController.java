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

// Controlador REST de la base de conocimiento (RF02)
@RestController
@RequestMapping("/api/v1/knowledge-articles")
@RequiredArgsConstructor
@Tag(name = "Knowledge Base (RF02)", description = "Articulos tecnicos. Solo los activos y verificados se usan como fuentes del asistente IA.")
public class KnowledgeArticleController {

    private final KnowledgeArticleService knowledgeArticleService;
    private final AccessPolicy accessPolicy;

    @Operation(summary = "Search knowledge articles", description = "Filtros opcionales: q (texto en titulo o contenido), categoryId, active y verified.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Articulos que cumplen los filtros, del mas reciente al mas antiguo"),
        @ApiResponse(responseCode = "400", description = "Filtro con formato invalido")
    })
    @GetMapping
    public ResponseEntity<List<KnowledgeArticleResponseDTO>> searchArticles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean verified) {
        // Un empleado solo ve los articulos publicados (activos y verificados). n8n (sin usuario) no se filtra.
        if (accessPolicy.isAuthenticatedUser() && accessPolicy.isEmployee()) {
            active = true;
            verified = true;
        }
        return ResponseEntity.ok(knowledgeArticleService.searchArticles(q, categoryId, active, verified));
    }

    @Operation(summary = "Get a knowledge article by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle del articulo"),
        @ApiResponse(responseCode = "404", description = "Articulo no encontrado")
    })
    @GetMapping("/{articleId}")
    public ResponseEntity<KnowledgeArticleResponseDTO> getArticle(@PathVariable Long articleId) {
        KnowledgeArticleResponseDTO article = knowledgeArticleService.getArticle(articleId);
        if (accessPolicy.isAuthenticatedUser()) accessPolicy.checkArticle(article);
        return ResponseEntity.ok(article);
    }

    @Operation(summary = "Create a knowledge article", description = "El articulo nace activo y sin verificar.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Articulo creado"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
        @ApiResponse(responseCode = "404", description = "Categoria o autor no encontrados"),
        @ApiResponse(responseCode = "409", description = "La categoria esta inactiva")
    })
    @PostMapping
    public ResponseEntity<KnowledgeArticleResponseDTO> createArticle(@Valid @RequestBody KnowledgeArticleRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(knowledgeArticleService.createArticle(request));
    }

    @Operation(summary = "Update a knowledge article", description = "Si cambia el titulo o el contenido, el articulo vuelve a quedar sin verificar.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Articulo actualizado"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
        @ApiResponse(responseCode = "404", description = "Articulo, categoria o autor no encontrados"),
        @ApiResponse(responseCode = "409", description = "La nueva categoria esta inactiva")
    })
    @PutMapping("/{articleId}")
    public ResponseEntity<KnowledgeArticleResponseDTO> updateArticle(
            @PathVariable Long articleId,
            @Valid @RequestBody KnowledgeArticleRequestDTO request) {
        return ResponseEntity.ok(knowledgeArticleService.updateArticle(articleId, request));
    }

    @Operation(summary = "Activate, deactivate or verify a knowledge article", description = "Los campos omitidos no cambian.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado actualizado"),
        @ApiResponse(responseCode = "404", description = "Articulo no encontrado"),
        @ApiResponse(responseCode = "409", description = "No se envio active ni verified")
    })
    @PatchMapping("/{articleId}/status")
    public ResponseEntity<KnowledgeArticleResponseDTO> updateStatus(
            @PathVariable Long articleId,
            @RequestBody UpdateKnowledgeArticleStatusDTO request) {
        return ResponseEntity.ok(knowledgeArticleService.updateStatus(articleId, request));
    }
}
