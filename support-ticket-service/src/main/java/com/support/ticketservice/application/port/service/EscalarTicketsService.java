package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.in.EscalarTicketsUseCase;
import com.support.ticketservice.application.port.out.HistorialRepositoryPort;
import com.support.ticketservice.application.port.out.TicketRepositoryPort;
import com.support.ticketservice.domain.model.HistorialTicket;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.model.enums.Prioridad;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscalarTicketsService implements EscalarTicketsUseCase {

    private static final String SISTEMA = "sistema";

    private final TicketRepositoryPort tickets;
    private final HistorialRepositoryPort historial;
    private final TransactionTemplate tx;
    private final Clock clock;

    @Value("${app.escalation.threshold}")
    private Duration umbral;

    @Override
    public int ejecutar() {
        Instant ahora = clock.instant();
        int escalados = 0;

        for (Long id : tickets.buscarIdsEscalables(ahora.plus(umbral))) {
            try {
                if (Boolean.TRUE.equals(tx.execute(status -> escalar(id, ahora)))) {
                    escalados++;
                }
            } catch (Exception e) {
                log.error("No se pudo escalar el ticket {}", id, e);
            }
        }
        if (escalados > 0) {
            log.info("Escalamiento automático: {} ticket(s) escalados", escalados);
        }
        return escalados;
    }

    private boolean escalar(Long id, Instant ahora) {
        Ticket ticket = tickets.buscarPorId(id).orElse(null);
        // Se revalida: pudo cambiar de estado desde que se consultó la lista
        if (ticket == null || !ticket.necesitaEscalado(ahora, umbral)) {
            return false;
        }

        Prioridad antes = ticket.getPrioridad();
        boolean vencido = ticket.estaVencido(ahora);
        ticket.escalar();
        ticket = tickets.guardar(ticket);

        String motivo = vencido
                ? "SLA vencido"
                : "SLA vence en " + Duration.between(ahora, ticket.getFechaLimiteSla()).toMinutes() + " min";
        String cambio = antes == ticket.getPrioridad()
                ? "ya estaba en prioridad máxima (" + antes + ")"
                : "prioridad " + antes + " -> " + ticket.getPrioridad();

        Long tecnicoId = ticket.getTecnicoAsignado() == null ? null : ticket.getTecnicoAsignado().id();
        historial.guardar(new HistorialTicket(null, ticket.getId(), ticket.getEstado(),
                ticket.getEstado(), tecnicoId,
                "Escalado automático: " + motivo + "; " + cambio, SISTEMA, ahora));
        return true;
    }
}
