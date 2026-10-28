package com.support.ticketservice.infrastructure.adapter.in.scheduler;

import com.support.ticketservice.application.port.in.EscalarTicketsUseCase;
import com.support.ticketservice.infrastructure.config.TraceIdFilter;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscalationSchedulerTest {

    @Mock private EscalarTicketsUseCase escalar;
    @InjectMocks private EscalationScheduler scheduler;

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void ejecutaElEscalamientoConUnTraceIdDeJob() {
        when(escalar.ejecutar()).thenAnswer(invocation -> {
            assertThat(MDC.get(TraceIdFilter.MDC_KEY).toString())
                    .startsWith("job-")
                    .hasSize(12);
            return 2;
        });

        scheduler.ejecutar();

        verify(escalar).ejecutar();
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void limpiaElMdcAunqueElEscalamientoFalle() {
        when(escalar.ejecutar()).thenThrow(new IllegalStateException("boom"));

        org.assertj.core.api.Assertions.assertThatThrownBy(scheduler::ejecutar)
                .isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }
}
