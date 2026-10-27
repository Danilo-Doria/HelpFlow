package com.support.ticketservice.application.port.in;

import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.model.enums.EstadoTicket;

public interface CambiarEstadoUseCase {
    Ticket ejecutar(Long ticketId, EstadoTicket nuevoEstado, String observacion, Actor actor);
}
