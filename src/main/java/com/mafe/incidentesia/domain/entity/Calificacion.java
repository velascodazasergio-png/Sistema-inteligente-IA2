package com.mafe.incidentesia.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "calificaciones")
@Getter
@Setter
@NoArgsConstructor
public class Calificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer puntuacion; // 1 a 5

    @Column(nullable = false)
    private Boolean fueUtil;

    @Column(length = 500)
    private String comentarioRetroalimentacion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCalificacion;

    // Dueno de la relacion 1:1 con RespuestaIA
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "respuesta_ia_id", nullable = false, unique = true)
    private RespuestaIA respuestaIA;

    // Dueno de la relacion N:1 con Usuario (quien evalua la respuesta de la IA)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuarioEvaluador;

    @PrePersist
    protected void onCreate() {
        this.fechaCalificacion = LocalDateTime.now();
    }
}

