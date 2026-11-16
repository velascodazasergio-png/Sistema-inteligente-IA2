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

// REST controller for catalogs: categories and users for forms and filters
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Catalogs", description = "Technical categories and users for forms and filters")
public class CatalogController {

    private final CatalogService catalogService;

    // GET /categories: all categories, or only the active ones
    @Operation(summary = "List technical categories", description = "With activeOnly=true it returns only the active categories.")
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponseDTO>> listCategories(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(catalogService.listCategories(activeOnly));
    }

    // GET /users: system users, optionally filtered by role
    @Operation(summary = "List system users", description = "Optional filter by role: EMPLOYEE, SUPPORT_TECHNICIAN or ADMIN.")
    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryDTO>> listUsers(@RequestParam(required = false) UserRole role) {
        return ResponseEntity.ok(catalogService.listUsers(role));
    }
}
