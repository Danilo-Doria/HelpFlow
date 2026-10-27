package com.support.ticketservice.application.port.in;

import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.Prioridad;

public interface CrearTicketUseCase {

    Ticket ejecutar(CrearTicketCommand command, String actor);

    record CrearTicketCommand(
            Long usuarioId,
            String titulo,
            String descripcion,
            Categoria categoria,
            Prioridad prioridad,
            String sede,
            String activoAfectado) {
    }
}
