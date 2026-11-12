package com.mafe.incidentai.service;

import com.mafe.incidentai.domain.enums.UserRole;
import com.mafe.incidentai.dto.CategoryResponseDTO;
import com.mafe.incidentai.dto.UserSummaryDTO;
import com.mafe.incidentai.repository.CategoryRepository;
import com.mafe.incidentai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Servicio de solo lectura para los catalogos que usan los formularios del frontend
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // Lista las categorias; con activeOnly=true solo las que admiten nuevos incidentes
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> listCategories(boolean activeOnly) {
        var categories = activeOnly
                ? categoryRepository.findByActiveTrue()
                : categoryRepository.findAll(Sort.by("name"));
        return categories.stream()
                .map(c -> new CategoryResponseDTO(c.getId(), c.getName(), c.getDescription(), c.getActive()))
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
    }

    // Lista los usuarios, opcionalmente filtrados por rol
    @Transactional(readOnly = true)
    public List<UserSummaryDTO> listUsers(UserRole role) {
        return userRepository.findAll(Sort.by("fullName"))
                .stream()
                .filter(u -> role == null || u.getRole() == role)
                .map(u -> new UserSummaryDTO(u.getId(), u.getFullName(), u.getEmail(), u.getRole()))
                .toList();
    }
}

