package com.mafe.incidentesia.repository;

import com.mafe.incidentesia.domain.entity.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {
    Optional<Calificacion> findByRespuestaIAId(Long respuestaIaId);
}

