package com.support.ticketservice.infrastructure.adapter.out.persistence.entity;

import com.support.ticketservice.domain.model.enums.EstadoTicket;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "historial_ticket")
@Getter
@Setter
@NoArgsConstructor
public class HistorialTicketEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id")
    private TicketEntity ticket;

    @Enumerated(EnumType.STRING)
    private EstadoTicket estadoAnterior;

    @Enumerated(EnumType.STRING)
    private EstadoTicket estadoNuevo;

    private Long tecnicoId;
    private String observacion;
    private String realizadoPor;
    private Instant fecha;
}
