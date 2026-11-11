package com.mafe.incidentesia.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre; // Ej: "Redes", "Base de Datos", "Accesos / Directorio Activo"

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activa = true;

    // Lado inverso (1:N): Una categoria agrupa multiples incidentes
    @OneToMany(mappedBy = "categoria")
    private List<Incidente> incidentes = new ArrayList<>();
}

