package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.in.CambiarEstadoUseCase;
import com.support.ticketservice.application.port.out.HistorialRepositoryPort;
import com.support.ticketservice.application.port.out.TicketMetricsPort;
import com.support.ticketservice.application.port.out.TicketRepositoryPort;
import com.support.ticketservice.domain.exception.OperacionNoPermitidaException;
import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.domain.model.HistorialTicket;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.domain.service.TicketAccessPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class CambiarEstadoService implements CambiarEstadoUseCase {

    private final TicketRepositoryPort tickets;
    private final HistorialRepositoryPort historial;
    private final TicketAccessPolicy policy;
    private final Clock clock;
    private final TicketMetricsPort metrics;

    @Override
    @Transactional
    public Ticket ejecutar(Long ticketId, EstadoTicket nuevo, String observacion, Actor actor) {
        Ticket ticket = tickets.buscarPorId(ticketId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ticket no encontrado: " + ticketId));

        if (!policy.puedeVer(ticket, actor)) {
            throw new OperacionNoPermitidaException("No tienes acceso a este ticket");
        }
        if (!policy.puedeCambiarA(ticket, actor, nuevo)) {
            throw new OperacionNoPermitidaException("Tu rol no puede pasar el ticket a " + nuevo);
        }

        EstadoTicket anterior = ticket.getEstado();
        boolean sinRespuestaPrevia = ticket.getFechaPrimeraRespuesta() == null;
        Instant ahora = clock.instant();

        ticket.cambiarEstado(nuevo, ahora);     // valida la transición en el dominio
        ticket = tickets.guardar(ticket);

        Long tecnicoId = ticket.getTecnicoAsignado() == null ? null : ticket.getTecnicoAsignado().id();
        historial.guardar(new HistorialTicket(null, ticket.getId(), anterior, nuevo,
                tecnicoId, observacion, actor.username(), ahora));

        if (sinRespuestaPrevia && ticket.getFechaPrimeraRespuesta() != null) {
            metrics.primeraRespuesta(ticket.getCategoria(), ticket.getPrioridad(),
                    Duration.between(ticket.getFechaCreacion(), ticket.getFechaPrimeraRespuesta()));
        }
        if (nuevo == EstadoTicket.RESUELTO) {
            metrics.resolucion(ticket.getCategoria(), ticket.getPrioridad(),
                    Duration.between(ticket.getFechaCreacion(), ticket.getFechaResolucion()));
        }
        log.info("Ticket {} cambió de {} a {} por {}", ticket.getId(), anterior, nuevo, actor.username());
        return ticket;
    }
}
