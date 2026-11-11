package com.mafe.incidentesia.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fuentes_respuesta")
@Getter
@Setter
@NoArgsConstructor
public class FuenteRespuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String tituloFuente;

    @Column(length = 500)
    private String referenciaUrl;

    @Column(columnDefinition = "TEXT")
    private String fragmentoCitado;

    // Dueno de la relacion N:1 con RespuestaIA
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "respuesta_ia_id", nullable = false)
    private RespuestaIA respuestaIA;
}

