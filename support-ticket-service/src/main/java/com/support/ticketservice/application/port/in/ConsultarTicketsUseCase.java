package com.support.ticketservice.application.port.in;

import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.domain.model.HistorialTicket;
import com.support.ticketservice.domain.model.Ticket;

import java.util.List;

public interface ConsultarTicketsUseCase {
    Ticket obtener(Long ticketId, Actor actor);

    List<Ticket> listar(Actor actor);

    List<HistorialTicket> historial(Long ticketId, Actor actor);
}
