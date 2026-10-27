package com.support.ticketservice.application.port.out;

import com.support.ticketservice.domain.model.Ticket;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TicketRepositoryPort {
    Ticket guardar(Ticket ticket);

    Optional<Ticket> buscarPorId(Long id);

    long contarActivosDeTecnico(Long tecnicoId);

    List<Long> buscarIdsEscalables(Instant limite);

    List<Ticket> listarTodos();

    List<Ticket> listarPorSolicitante(Long usuarioId);

    List<Ticket> listarPorTecnico(Long tecnicoId);
}
