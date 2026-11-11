package com.mafe.incidentesia.repository;

import com.mafe.incidentesia.domain.entity.ConsultaIA;
import com.mafe.incidentesia.domain.enums.EstadoConsulta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultaIARepository extends JpaRepository<ConsultaIA, Long> {
    List<ConsultaIA> findByIncidenteIdOrderByFechaConsultaDesc(Long incidenteId);
    List<ConsultaIA> findByEstadoConsulta(EstadoConsulta estadoConsulta);
}

