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

// Servicio de negocio para registrar, actualizar estados y filtrar incidentes tecnologicos
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    // Registra un nuevo incidente verificando primero que el usuario y la categoria existan y esten activos
    @Transactional
    public IncidentResponseDTO createIncident(CreateIncidentRequestDTO request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.userId()));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.categoryId()));

        // Regla de negocio: No se pueden asignar incidentes a una categoria inactiva
        if (Boolean.FALSE.equals(category.getActive())) {
            throw new BusinessRuleException("Cannot register an incident under an inactive category: " + category.getName());
        }

        Incident incident = new Incident();
        incident.setTitle(request.title());
        incident.setDescription(request.description());
        incident.setPriority(request.priority());
        incident.setStatus(IncidentStatus.OPEN);
        incident.setUser(user);
        incident.setCategory(category);

        Incident savedIncident = incidentRepository.save(incident);
        return mapToResponseDTO(savedIncident);
    }

    // Actualiza el estado de un incidente aplicando validaciones de transicion de estados
    @Transactional
    public IncidentResponseDTO updateIncidentStatus(Long incidentId, IncidentStatus newStatus) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with ID: " + incidentId));

        // Regla de negocio: Un incidente que ya fue cerrado (CLOSED) no puede modificarse
        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new BusinessRuleException("Cannot change the status of an already CLOSED incident");
        }

        incident.setStatus(newStatus);
        Incident updatedIncident = incidentRepository.save(incident);
        return mapToResponseDTO(updatedIncident);
    }

    // Filtra incidentes de forma dinamica combinando categoria, prioridad, usuario y rango de fechas
    @Transactional(readOnly = true)
    public List<IncidentResponseDTO> filterIncidents(
            Long categoryId,
            Priority priority,
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        // Validamos que el rango de fechas sea coherente si ambos parametros fueron enviados
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessRuleException("Start date cannot be after end date");
        }

        Specification<Incident> spec = IncidentSpecification.withDynamicFilters(
                categoryId, priority, userId, startDate, endDate
        );

        return incidentRepository.findAll(spec)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    // Convierte la entidad JPA Incident al contrato de salida IncidentResponseDTO
    private IncidentResponseDTO mapToResponseDTO(Incident incident) {
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
                incident.getUpdatedAt()
        );
    }
}

