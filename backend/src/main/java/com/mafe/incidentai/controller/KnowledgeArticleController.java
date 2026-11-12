package com.mafe.incidentai.controller;

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
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controlador REST de la base de conocimiento (RF02)
@RestController
@RequestMapping("/api/v1/knowledge-articles")
@RequiredArgsConstructor
@Tag(name = "Knowledge Base (RF02)", description = "Articulos tecnicos. Solo los activos y verificados se usan como fuentes del asistente IA.")
public class KnowledgeArticleController {

    private final KnowledgeArticleService knowledgeArticleService;

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
        return ResponseEntity.ok(knowledgeArticleService.searchArticles(q, categoryId, active, verified));
    }

    @Operation(summary = "Get a knowledge article by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle del articulo"),
        @ApiResponse(responseCode = "404", description = "Articulo no encontrado")
    })
    @GetMapping("/{articleId}")
    public ResponseEntity<KnowledgeArticleResponseDTO> getArticle(@PathVariable Long articleId) {
        return ResponseEntity.ok(knowledgeArticleService.getArticle(articleId));
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

