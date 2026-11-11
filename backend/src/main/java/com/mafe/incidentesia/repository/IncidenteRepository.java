package com.mafe.incidentesia.repository;

import com.mafe.incidentesia.domain.entity.Incidente;
import com.mafe.incidentesia.domain.enums.EstadoIncidente;
import com.mafe.incidentesia.domain.enums.Prioridad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidenteRepository extends JpaRepository<Incidente, Long> {
    List<Incidente> findByEstado(EstadoIncidente estado);
    List<Incidente> findByCategoriaIdAndPrioridad(Long categoriaId, Prioridad prioridad);
    List<Incidente> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);
}

