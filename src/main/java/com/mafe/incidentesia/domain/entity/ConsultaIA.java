package com.mafe.incidentesia.domain.entity;

import com.mafe.incidentesia.domain.enums.EstadoConsulta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "consultas_ia")
@Getter
@Setter
@NoArgsConstructor
public class ConsultaIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String promptEnviado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoConsulta estadoConsulta;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaConsulta;

    // Dueno de la relacion N:1 con Incidente
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incidente_id", nullable = false)
    private Incidente incidente;

    // Lado inverso 1:1 con RespuestaIA
    @OneToOne(mappedBy = "consultaIA", cascade = CascadeType.ALL, orphanRemoval = true)
    private RespuestaIA respuestaIA;

    @PrePersist
    protected void onCreate() {
        this.fechaConsulta = LocalDateTime.now();
        if (this.estadoConsulta == null) {
            this.estadoConsulta = EstadoConsulta.PENDIENTE;
        }
    }
}

