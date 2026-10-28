package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.out.SlaConfigPort;
import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.domain.service.SlaPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static com.support.ticketservice.support.DomainFixtures.AHORA;
import static com.support.ticketservice.support.DomainFixtures.admin;
import static com.support.ticketservice.support.DomainFixtures.sla;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigurarSlaServiceTest {

    @Mock private SlaConfigPort slaConfig;
    @Mock private Clock clock;
    @InjectMocks private ConfigurarSlaService service;

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void obtieneLaPoliticaActual() {
        SlaPolicy policy = sla();
        when(slaConfig.obtener()).thenReturn(policy);

        assertThat(service.obtener()).isSameAs(policy);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void actualizaUnPlazoYLoPersisteConAuditoria() {
        when(slaConfig.obtener()).thenReturn(sla());
        when(clock.instant()).thenReturn(AHORA);

        SlaPolicy nueva = service.actualizar(Prioridad.ALTA, 6, admin());

        assertThat(nueva.plazoDe(Prioridad.ALTA)).isEqualTo(Duration.ofHours(6));
        assertThat(nueva.plazoDe(Prioridad.CRITICA)).isEqualTo(Duration.ofHours(4));
        verify(slaConfig).guardar(Prioridad.ALTA, 6, "admin@helpflow.com", AHORA);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void usaElRelojParaLaFechaDeActualizacion() {
        Instant fecha = Instant.parse("2026-05-01T12:00:00Z");
        when(slaConfig.obtener()).thenReturn(sla());
        when(clock.instant()).thenReturn(fecha);

        service.actualizar(Prioridad.BAJA, 96, admin());

        verify(slaConfig).guardar(Prioridad.BAJA, 96, "admin@helpflow.com", fecha);
    }
}
