package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.in.ConsultarTicketsUseCase;
import com.support.ticketservice.application.port.out.HistorialRepositoryPort;
import com.support.ticketservice.application.port.out.TicketRepositoryPort;
import com.support.ticketservice.domain.exception.OperacionNoPermitidaException;
import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.domain.model.HistorialTicket;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.service.TicketAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultarTicketsService implements ConsultarTicketsUseCase {

    private final TicketRepositoryPort tickets;
    private final HistorialRepositoryPort historial;
    private final TicketAccessPolicy policy;

    @Override
    @Transactional(readOnly = true)
    public Ticket obtener(Long ticketId, Actor actor) {
        return cargarConAcceso(ticketId, actor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ticket> listar(Actor actor) {
        return switch (actor.rol()) {
            case ROLE_ADMIN -> tickets.listarTodos();
            case ROLE_USUARIO -> tickets.listarPorSolicitante(actor.usuarioId());
            case ROLE_TECNICO -> tickets.listarPorTecnico(actor.tecnicoId());
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialTicket> historial(Long ticketId, Actor actor) {
        cargarConAcceso(ticketId, actor);
        return historial.listarPorTicket(ticketId);
    }

    private Ticket cargarConAcceso(Long ticketId, Actor actor) {
        Ticket ticket = tickets.buscarPorId(ticketId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ticket no encontrado: " + ticketId));
        if (!policy.puedeVer(ticket, actor)) {
            throw new OperacionNoPermitidaException("No tienes acceso a este ticket");
        }
        return ticket;
    }
}
