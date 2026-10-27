package com.support.ticketservice.application.port.in;

import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.domain.model.Ticket;

public interface ReasignarTicketUseCase {
    Ticket ejecutar(Long ticketId, Long tecnicoId, String observacion, Actor actor);
}
