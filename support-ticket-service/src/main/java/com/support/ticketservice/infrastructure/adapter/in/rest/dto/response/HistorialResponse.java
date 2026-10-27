package com.support.ticketservice.infrastructure.adapter.in.rest.dto.response;

import com.support.ticketservice.domain.model.enums.EstadoTicket;

import java.time.Instant;

public record HistorialResponse(
        Long id,
        EstadoTicket estadoAnterior,
        EstadoTicket estadoNuevo,
        Long tecnicoId,
        String observacion,
        String realizadoPor,
        Instant fecha) {
}
