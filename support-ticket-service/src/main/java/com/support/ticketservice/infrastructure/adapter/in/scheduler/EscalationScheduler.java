package com.support.ticketservice.infrastructure.adapter.in.scheduler;

import com.support.ticketservice.application.port.in.EscalarTicketsUseCase;
import com.support.ticketservice.infrastructure.config.TraceIdFilter;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EscalationScheduler {

    private final EscalarTicketsUseCase escalar;

    @Scheduled(fixedDelayString = "${app.escalation.interval-ms}",
            initialDelayString = "${app.escalation.initial-delay-ms}")
    public void ejecutar() {
        MDC.put(TraceIdFilter.MDC_KEY, "job-" + UUID.randomUUID().toString().substring(0, 8));
        try {
            escalar.ejecutar();
        } finally {
            MDC.remove(TraceIdFilter.MDC_KEY);
        }
    }
}
