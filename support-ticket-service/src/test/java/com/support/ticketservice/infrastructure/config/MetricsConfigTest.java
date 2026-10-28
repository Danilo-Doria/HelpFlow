package com.support.ticketservice.infrastructure.config;

import com.support.ticketservice.application.port.out.TicketRepositoryPort;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;

import static com.support.ticketservice.support.DomainFixtures.AHORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsConfigTest {

    @Mock private TicketRepositoryPort tickets;
    @Mock private Clock clock;

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void registraElGaugeDeTicketsVencidos() {
        when(clock.instant()).thenReturn(AHORA);
        when(tickets.contarVencidos(AHORA)).thenReturn(4L);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MeterBinder binder = new MetricsConfig().ticketsVencidosGauge(tickets, clock);

        binder.bindTo(registry);

        assertThat(registry.get("helpflow.tickets.vencidos").gauge().value()).isEqualTo(4.0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void devuelveNaNcuandoFallaElConteo() {
        when(clock.instant()).thenReturn(AHORA);
        when(tickets.contarVencidos(AHORA)).thenThrow(new IllegalStateException("db"));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MeterBinder binder = new MetricsConfig().ticketsVencidosGauge(tickets, clock);

        binder.bindTo(registry);

        assertThat(registry.get("helpflow.tickets.vencidos").gauge().value()).isNaN();
    }
}
