package com.support.ticketservice.infrastructure.adapter.out.persistence.entity;

import com.support.ticketservice.domain.model.enums.Disponibilidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "asignaciones")
@Getter
@Setter
@NoArgsConstructor
public class AsignacionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long ticketId;
    private Long tecnicoId;
    private String tecnicoSugeridoExterno;

    @Enumerated(EnumType.STRING)
    private Disponibilidad disponibilidad;

    private Integer tiempoEstimadoHoras;
    private Instant fechaAsignacion;
    private boolean activa;
    private String asignadoPor;
}
