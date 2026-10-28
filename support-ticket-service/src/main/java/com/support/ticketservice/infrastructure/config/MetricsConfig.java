package com.support.ticketservice.infrastructure.config;

import com.support.ticketservice.application.port.out.TicketRepositoryPort;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@Slf4j
public class MetricsConfig {

    @Bean
    MeterBinder ticketsVencidosGauge(TicketRepositoryPort tickets, Clock clock) {
        return registry -> Gauge.builder("helpflow.tickets.vencidos", () -> {
                    try {
                        return tickets.contarVencidos(clock.instant());
                    } catch (Exception e) {
                        log.warn("No se pudo calcular tickets vencidos: {}", e.getMessage());
                        return Double.NaN;
                    }
                })
                .description("Tickets activos cuya fecha límite de SLA ya pasó")
                .register(registry);
    }
}
