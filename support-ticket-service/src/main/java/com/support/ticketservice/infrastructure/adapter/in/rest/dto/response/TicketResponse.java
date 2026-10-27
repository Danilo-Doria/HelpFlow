package com.support.ticketservice.infrastructure.adapter.in.rest.dto.response;

import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.domain.model.enums.Prioridad;

import java.time.Instant;

public record TicketResponse(
        Long id,
        String titulo,
        String descripcion,
        Categoria categoria,
        Prioridad prioridad,
        String sede,
        String activoAfectado,
        Instant fechaCreacion,
        Instant fechaLimiteSla,
        EstadoTicket estado,
        boolean escalado,
        Long usuarioId,
        Long tecnicoId,
        String tecnicoNombre,
        Instant fechaPrimeraRespuesta,
        Instant fechaResolucion) {
}
