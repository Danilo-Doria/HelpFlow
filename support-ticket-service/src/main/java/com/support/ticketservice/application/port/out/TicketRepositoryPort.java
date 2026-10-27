package com.support.ticketservice.application.port.out;

import com.support.ticketservice.domain.model.Ticket;

import java.util.Optional;

public interface TicketRepositoryPort {
    Ticket guardar(Ticket ticket);
    Optional<Ticket> buscarPorId(Long id);
}
