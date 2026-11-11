package com.mafe.incidentesia.repository;

import com.mafe.incidentesia.domain.entity.RespuestaIA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RespuestaIARepository extends JpaRepository<RespuestaIA, Long> {
    Optional<RespuestaIA> findByConsultaIAId(Long consultaIaId);
}

