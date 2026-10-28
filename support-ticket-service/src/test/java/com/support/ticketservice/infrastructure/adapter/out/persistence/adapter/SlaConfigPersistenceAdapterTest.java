package com.support.ticketservice.infrastructure.adapter.out.persistence.adapter;

import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.domain.service.SlaPolicy;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.SlaConfigEntity;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.SlaConfigJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlaConfigPersistenceAdapterTest {

    @Mock private SlaConfigJpaRepository jpa;
    private SlaConfigPersistenceAdapter adapter;

    @BeforeEach
    void preparar() {
        adapter = new SlaConfigPersistenceAdapter(jpa);
    }

    private SlaConfigEntity entidad(Prioridad prioridad, int horas) {
        SlaConfigEntity e = new SlaConfigEntity();
        e.setPrioridad(prioridad);
        e.setHoras(horas);
        return e;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void obtieneLaPoliticaDesdeLaBaseDeDatos() {
        when(jpa.findAll()).thenReturn(List.of(
                entidad(Prioridad.BAJA, 72),
                entidad(Prioridad.MEDIA, 24),
                entidad(Prioridad.ALTA, 8),
                entidad(Prioridad.CRITICA, 4)));

        SlaPolicy policy = adapter.obtener();

        assertThat(policy.plazoDe(Prioridad.ALTA)).isEqualTo(Duration.ofHours(8));
        assertThat(policy.plazoDe(Prioridad.CRITICA)).isEqualTo(Duration.ofHours(4));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void actualizaUnaConfiguracionExistente() {
        SlaConfigEntity existente = entidad(Prioridad.ALTA, 8);
        when(jpa.findById(Prioridad.ALTA)).thenReturn(Optional.of(existente));

        adapter.guardar(Prioridad.ALTA, 6, "admin", Instant.parse("2026-01-01T10:00:00Z"));

        ArgumentCaptor<SlaConfigEntity> captor = ArgumentCaptor.forClass(SlaConfigEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getHoras()).isEqualTo(6);
        assertThat(captor.getValue().getActualizadoPor()).isEqualTo("admin");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void creaUnaConfiguracionNueva() {
        when(jpa.findById(Prioridad.CRITICA)).thenReturn(Optional.empty());
        when(jpa.save(any(SlaConfigEntity.class))).thenAnswer(i -> i.getArgument(0));

        adapter.guardar(Prioridad.CRITICA, 3, "admin", Instant.parse("2026-01-01T10:00:00Z"));

        ArgumentCaptor<SlaConfigEntity> captor = ArgumentCaptor.forClass(SlaConfigEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getPrioridad()).isEqualTo(Prioridad.CRITICA);
        assertThat(captor.getValue().getHoras()).isEqualTo(3);
    }
}
