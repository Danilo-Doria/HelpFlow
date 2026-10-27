package com.support.ticketservice.domain.model;

import com.support.ticketservice.domain.model.enums.EstadoTicket;

import java.time.Instant;

public record HistorialTicket(
        Long id, Long ticketId,
        EstadoTicket estadoAnterior, EstadoTicket estadoNuevo,
        Long tecnicoId, String observacion,
        String realizadoPor, Instant fecha) {
}
