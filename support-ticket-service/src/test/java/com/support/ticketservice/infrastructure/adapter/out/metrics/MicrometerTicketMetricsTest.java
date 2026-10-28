package com.support.ticketservice.infrastructure.adapter.out.metrics;

import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.Prioridad;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerTicketMetricsTest {

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void registraContadorDeTicketsCreados() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerTicketMetrics metrics = new MicrometerTicketMetrics(registry);

        metrics.ticketCreado(Categoria.HARDWARE, Prioridad.ALTA);

        assertThat(registry.get("helpflow.tickets.opened")
                .tags("categoria", "HARDWARE", "prioridad", "ALTA")
                .counter().count()).isEqualTo(1.0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void registraTimerDePrimeraRespuesta() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerTicketMetrics metrics = new MicrometerTicketMetrics(registry);

        metrics.primeraRespuesta(Categoria.RED, Prioridad.MEDIA, Duration.ofMinutes(30));

        assertThat(registry.get("helpflow.tickets.first.response")
                .tags("categoria", "RED", "prioridad", "MEDIA")
                .timer().count()).isEqualTo(1);
        assertThat(registry.get("helpflow.tickets.first.response")
                .tags("categoria", "RED", "prioridad", "MEDIA")
                .timer().totalTime(java.util.concurrent.TimeUnit.MINUTES)).isCloseTo(30.0,
                org.assertj.core.data.Offset.offset(0.001));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void registraTimerDeResolucion() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerTicketMetrics metrics = new MicrometerTicketMetrics(registry);

        metrics.resolucion(Categoria.SOFTWARE, Prioridad.CRITICA, Duration.ofHours(2));

        assertThat(registry.get("helpflow.tickets.resolution")
                .tags("categoria", "SOFTWARE", "prioridad", "CRITICA")
                .timer().count()).isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void registraContadorDeEscalamientos() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerTicketMetrics metrics = new MicrometerTicketMetrics(registry);

        metrics.ticketEscalado(Prioridad.CRITICA);

        assertThat(registry.get("helpflow.tickets.escalated")
                .tag("prioridad", "CRITICA")
                .counter().count()).isEqualTo(1.0);
    }
}
