package com.mafe.incidentai.controller;

import com.mafe.incidentai.domain.enums.UserRole;
import com.mafe.incidentai.dto.CategoryResponseDTO;
import com.mafe.incidentai.dto.UserSummaryDTO;
import com.mafe.incidentai.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Controlador REST de catalogos: categorias y usuarios para formularios y filtros
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Catalogs", description = "Categorias tecnicas y usuarios para formularios y filtros")
public class CatalogController {

    private final CatalogService catalogService;

    @Operation(summary = "List technical categories", description = "Con activeOnly=true devuelve solo las categorias activas.")
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponseDTO>> listCategories(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(catalogService.listCategories(activeOnly));
    }

    @Operation(summary = "List system users", description = "Filtro opcional por rol: EMPLOYEE, SUPPORT_TECHNICIAN o ADMIN.")
    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryDTO>> listUsers(@RequestParam(required = false) UserRole role) {
        return ResponseEntity.ok(catalogService.listUsers(role));
    }
}
