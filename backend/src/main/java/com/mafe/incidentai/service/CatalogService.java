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

// Read-only service for the catalogs used by the frontend forms
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // Lists the categories; with activeOnly=true, only those that accept new incidents
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

    // Lists the users, optionally filtered by role
    @Transactional(readOnly = true)
    public List<UserSummaryDTO> listUsers(UserRole role) {
        return userRepository.findAll(Sort.by("fullName"))
                .stream()
                .filter(u -> role == null || u.getRole() == role)
                .map(UserSummaryDTO::from)
                .toList();
    }
}
