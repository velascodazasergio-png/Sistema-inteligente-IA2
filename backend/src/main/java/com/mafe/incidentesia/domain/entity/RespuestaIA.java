package com.mafe.incidentesia.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "respuestas_ia")
@Getter
@Setter
@NoArgsConstructor
public class RespuestaIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String diagnosticoSugerido;

    @Column(columnDefinition = "TEXT")
    private String pasosSolucion;

    private Double nivelConfianza; // Ej: 0.92 (92% de certeza del agente en n8n)

    private Long tiempoProcesamientoMs;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRespuesta;

    // Dueno de la relacion 1:1 con ConsultaIA
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consulta_ia_id", nullable = false, unique = true)
    private ConsultaIA consultaIA;

    // Relacion 1:N con FuenteRespuesta (documentos o tickets usados por el RAG en n8n)
    @OneToMany(mappedBy = "respuestaIA", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FuenteRespuesta> fuentes = new ArrayList<>();

    // Lado inverso 1:1 con Calificacion
    @OneToOne(mappedBy = "respuestaIA", cascade = CascadeType.ALL, orphanRemoval = true)
    private Calificacion calificacion;

    @PrePersist
    protected void onCreate() {
        this.fechaRespuesta = LocalDateTime.now();
    }

    // Metodo helper para sincronizar la relacion bidireccional con FuenteRespuesta
    public void agregarFuente(FuenteRespuesta fuente) {
        this.fuentes.add(fuente);
        fuente.setRespuestaIA(this);
    }
}

