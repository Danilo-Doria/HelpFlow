package com.support.ticketservice.infrastructure.adapter.out.metrics;

import com.support.ticketservice.application.port.out.TicketMetricsPort;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.Prioridad;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class MicrometerTicketMetrics implements TicketMetricsPort {

    private final MeterRegistry registry;

    @Override
    public void ticketCreado(Categoria categoria, Prioridad prioridad) {
        registry.counter("helpflow.tickets.created",
                "categoria", categoria.name(), "prioridad", prioridad.name()).increment();
    }

    @Override
    public void primeraRespuesta(Categoria categoria, Prioridad prioridad, Duration tiempo) {
        Timer.builder("helpflow.tickets.first.response")
                .description("Tiempo desde la creación hasta el primer paso a EN_PROGRESO")
                .tags("categoria", categoria.name(), "prioridad", prioridad.name())
                .register(registry)
                .record(tiempo);
    }

    @Override
    public void resolucion(Categoria categoria, Prioridad prioridad, Duration tiempo) {
        Timer.builder("helpflow.tickets.resolution")
                .description("Tiempo desde la creación hasta RESUELTO")
                .tags("categoria", categoria.name(), "prioridad", prioridad.name())
                .register(registry)
                .record(tiempo);
    }

    @Override
    public void ticketEscalado(Prioridad prioridadResultante) {
        registry.counter("helpflow.tickets.escalated",
                "prioridad", prioridadResultante.name()).increment();
    }
}
