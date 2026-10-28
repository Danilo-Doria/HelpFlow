package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.in.CrearTicketUseCase;
import com.support.ticketservice.application.port.out.*;
import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.model.*;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.domain.service.TecnicoSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrearTicketService implements CrearTicketUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final TecnicoRepositoryPort tecnicos;
    private final TicketRepositoryPort tickets;
    private final HistorialRepositoryPort historial;
    private final AsignacionRepositoryPort asignaciones;
    private final SlaConfigPort slaConfig;
    private final TechnicianDispatchPort dispatch;
    private final TecnicoSelector selector;
    private final Clock clock;
    private final TicketMetricsPort metrics;

    @Override
    @Transactional
    public Ticket ejecutar(CrearTicketCommand cmd, String actor) {
        Usuario usuario = usuarios.buscarPorId(cmd.usuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + cmd.usuarioId()));

        Instant ahora = clock.instant();

        // ABIERTO mas el SLA calculado (valida usuario ACTIVO)
        Ticket ticket = Ticket.abrir(cmd.titulo(), cmd.descripcion(), cmd.categoria(),
                cmd.prioridad(), cmd.sede(), cmd.activoAfectado(),
                usuario, slaConfig.obtener(), ahora);
        ticket = tickets.guardar(ticket);

        // Se registra aquí, antes de cualquier return anticipado, para contar TODOS los
        // tickets creados (también los que quedan ABIERTO) y con la prioridad solicitada.
        metrics.ticketCreado(ticket.getCategoria(), ticket.getPrioridad());
        log.info("Ticket creado id={} categoria={} prioridad={} solicitante={}",
                ticket.getId(), ticket.getCategoria(), ticket.getPrioridad(), actor);

        historial.guardar(new HistorialTicket(null, ticket.getId(), null,
                EstadoTicket.ABIERTO, null, "Ticket creado", actor, ahora));

        // Consulta al servicio externo
        Optional<SugerenciaDespacho> sugerencia =
                dispatch.consultar(ticket.getId(), ticket.getCategoria(), ticket.getPrioridad());
        if (sugerencia.isEmpty()) {
            log.warn("Sin sugerencia de despacho para ticket {}; queda ABIERTO", ticket.getId());
            return ticket;
        }

        // Reglas internas de asignación
        Optional<Tecnico> elegido = selector.seleccionar(ticket,
                tecnicos.buscarCandidatos(ticket.getCategoria()));
        if (elegido.isEmpty()) {
            log.warn("Sin técnico elegible para ticket {}; queda ABIERTO", ticket.getId());
            return ticket;
        }

        // Asignación mas el historial
        SugerenciaDespacho sug = sugerencia.get();
        Tecnico tecnico = elegido.get();
        ticket.asignar(tecnico);

        String observacion = "Asignado a " + tecnico.nombre();
        Duration restante = Duration.between(ahora, ticket.getFechaLimiteSla());
        if (sug.tiempoEstimadoHoras() > restante.toHours() && ticket.escalar()) {
            metrics.ticketEscalado(ticket.getPrioridad());
            observacion += ". Escalado: tiempo estimado (" + sug.tiempoEstimadoHoras()
                    + " h) supera el SLA restante";
        }

        ticket = tickets.guardar(ticket);
        asignaciones.guardar(new Asignacion(null, ticket.getId(), tecnico.id(),
                sug.tecnicoCodigo(), sug.disponibilidad(), sug.tiempoEstimadoHoras(),
                ahora, true, actor));
        historial.guardar(new HistorialTicket(null, ticket.getId(), EstadoTicket.ABIERTO,
                EstadoTicket.ASIGNADO, tecnico.id(), observacion, actor, ahora));

        return ticket;
    }
}