package com.mafe.incidentai.service;

import com.mafe.incidentai.domain.entity.Category;
import com.mafe.incidentai.domain.entity.Incident;
import com.mafe.incidentai.domain.entity.User;
import com.mafe.incidentai.domain.enums.IncidentStatus;
import com.mafe.incidentai.domain.enums.Priority;
import com.mafe.incidentai.dto.CreateIncidentRequestDTO;
import com.mafe.incidentai.dto.IncidentResponseDTO;
import com.mafe.incidentai.exception.BusinessRuleException;
import com.mafe.incidentai.exception.ResourceNotFoundException;
import com.mafe.incidentai.repository.CategoryRepository;
import com.mafe.incidentai.repository.IncidentRepository;
import com.mafe.incidentai.repository.UserRepository;
import com.mafe.incidentai.repository.specification.IncidentSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Business service to register, update, change the status of and filter technology incidents (RF01)
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    // 1. Registers a new incident after checking that the user and the category exist and are active
    @Transactional
    public IncidentResponseDTO createIncident(CreateIncidentRequestDTO request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.userId()));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.categoryId()));

        // Business rule: incidents cannot be assigned to an inactive category
        if (Boolean.FALSE.equals(category.getActive())) {
            throw new BusinessRuleException("Cannot register an incident under an inactive category: " + category.getName());
        }

        Incident incident = new Incident();
        incident.setTitle(request.title().trim());
        incident.setDescription(request.description().trim());
        incident.setPriority(request.priority());
        incident.setStatus(IncidentStatus.OPEN);
        incident.setUser(user);
        incident.setCategory(category);

        Incident savedIncident = incidentRepository.save(incident);
        return mapToResponseDTO(savedIncident);
    }

    // 2. Updates the general information of an existing incident (RF01)
    @Transactional
    public IncidentResponseDTO updateIncident(Long incidentId, CreateIncidentRequestDTO request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with ID: " + incidentId));

        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new BusinessRuleException("Cannot modify an already CLOSED incident");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.userId()));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.categoryId()));

        if (Boolean.FALSE.equals(category.getActive())) {
            throw new BusinessRuleException("Cannot assign an incident to an inactive category: " + category.getName());
        }

        incident.setTitle(request.title().trim());
        incident.setDescription(request.description().trim());
        incident.setPriority(request.priority());
        incident.setUser(user);
        incident.setCategory(category);

        return mapToResponseDTO(incidentRepository.save(incident));
    }

    // 3. Updates the status of an incident, validating the transition and recording the resolution date
    @Transactional
    public IncidentResponseDTO updateIncidentStatus(Long incidentId, IncidentStatus newStatus) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with ID: " + incidentId));

        // Business rule: an incident that is already closed (CLOSED) cannot be modified
        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new BusinessRuleException("Cannot change the status of an already CLOSED incident");
        }

        incident.setStatus(newStatus);
        if (newStatus == IncidentStatus.RESOLVED || newStatus == IncidentStatus.CLOSED) {
            if (incident.getResolvedAt() == null) {
                incident.setResolvedAt(LocalDateTime.now());
            }
        } else {
            incident.setResolvedAt(null);
        }

        Incident updatedIncident = incidentRepository.save(incident);
        return mapToResponseDTO(updatedIncident);
    }

    // 4. Filters incidents dynamically by category, priority, status, user and date range
    @Transactional(readOnly = true)
    public List<IncidentResponseDTO> filterIncidents(
            Long categoryId,
            Priority priority,
            IncidentStatus status,
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        // Check that the date range is consistent when both parameters were sent
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessRuleException("Start date cannot be after end date");
        }

        Specification<Incident> spec = IncidentSpecification.withDynamicFilters(
                categoryId, priority, status, userId, startDate, endDate
        );

        return incidentRepository.findAll(spec)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    // 5. Returns the detail of an incident by its ID
    @Transactional(readOnly = true)
    public IncidentResponseDTO getIncident(Long incidentId) {
        return incidentRepository.findById(incidentId)
                .map(this::mapToResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with ID: " + incidentId));
    }

    // Maps the JPA Incident entity to the IncidentResponseDTO output contract
    private IncidentResponseDTO mapToResponseDTO(Incident incident) {
        LocalDateTime effectiveResolvedAt = incident.getResolvedAt();
        if (effectiveResolvedAt == null
                && (incident.getStatus() == IncidentStatus.RESOLVED || incident.getStatus() == IncidentStatus.CLOSED)) {
            effectiveResolvedAt = incident.getUpdatedAt();
        }

        return new IncidentResponseDTO(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getPriority(),
                incident.getStatus(),
                incident.getUser().getId(),
                incident.getUser().getFullName(),
                incident.getCategory().getId(),
                incident.getCategory().getName(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                effectiveResolvedAt
        );
    }
}
