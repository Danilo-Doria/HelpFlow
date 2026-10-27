package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.in.ReasignarTicketUseCase;
import com.support.ticketservice.application.port.out.AsignacionRepositoryPort;
import com.support.ticketservice.application.port.out.HistorialRepositoryPort;
import com.support.ticketservice.application.port.out.TecnicoRepositoryPort;
import com.support.ticketservice.application.port.out.TicketRepositoryPort;
import com.support.ticketservice.domain.exception.AsignacionInvalidaException;
import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.model.*;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReasignarTicketService implements ReasignarTicketUseCase {

    private final TicketRepositoryPort tickets;
    private final TecnicoRepositoryPort tecnicos;
    private final AsignacionRepositoryPort asignaciones;
    private final HistorialRepositoryPort historial;
    private final Clock clock;

    @Override
    @Transactional
    public Ticket ejecutar(Long ticketId, Long tecnicoId, String observacion, Actor actor) {
        Ticket ticket = tickets.buscarPorId(ticketId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ticket no encontrado: " + ticketId));
        Tecnico nuevo = tecnicos.buscarPorId(tecnicoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Técnico no encontrado: " + tecnicoId));

        Tecnico anterior = ticket.getTecnicoAsignado();
        if (anterior != null && anterior.id().equals(nuevo.id())) {
            throw new AsignacionInvalidaException("El ticket ya está asignado a ese técnico");
        }

        EstadoTicket estadoAnterior = ticket.getEstado();
        ticket.asignar(nuevo);   // valida transición, técnico ACTIVO y especialidad

        if (!nuevo.tieneCapacidad(tickets.contarActivosDeTecnico(nuevo.id()))) {
            throw new AsignacionInvalidaException(
                    "El técnico alcanzó su carga máxima de " + nuevo.cargaMaxima() + " tickets activos");
        }

        Instant ahora = clock.instant();
        ticket = tickets.guardar(ticket);

        asignaciones.desactivarActivas(ticket.getId());
        asignaciones.guardar(new Asignacion(null, ticket.getId(), nuevo.id(), null, null, null,
                ahora, true, actor.username()));

        String detalle = (anterior == null
                ? "Asignado manualmente a " + nuevo.nombre()
                : "Reasignado de " + anterior.nombre() + " a " + nuevo.nombre())
                + (observacion == null || observacion.isBlank() ? "" : ". " + observacion.trim());
        historial.guardar(new HistorialTicket(null, ticket.getId(), estadoAnterior,
                EstadoTicket.ASIGNADO, nuevo.id(), detalle, actor.username(), ahora));

        return ticket;
    }
}
