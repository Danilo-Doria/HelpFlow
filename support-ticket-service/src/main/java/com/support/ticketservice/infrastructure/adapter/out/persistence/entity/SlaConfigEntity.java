package com.support.ticketservice.infrastructure.adapter.out.persistence.entity;

import com.support.ticketservice.domain.model.enums.Prioridad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "sla_config")
@Getter
@Setter
@NoArgsConstructor
public class SlaConfigEntity {
    @Id
    @Enumerated(EnumType.STRING)
    private Prioridad prioridad;
    private int horas;
    private String actualizadoPor;
    private Instant fechaActualizacion;
}
