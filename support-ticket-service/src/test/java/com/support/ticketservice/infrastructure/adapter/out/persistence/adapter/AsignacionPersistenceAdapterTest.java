package com.support.ticketservice.infrastructure.adapter.out.persistence.adapter;

import com.support.ticketservice.domain.model.Asignacion;
import com.support.ticketservice.domain.model.enums.Disponibilidad;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.AsignacionEntity;
import com.support.ticketservice.infrastructure.adapter.out.persistence.mapper.AsignacionPersistenceMapper;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.AsignacionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignacionPersistenceAdapterTest {

    @Mock private AsignacionJpaRepository jpa;
    private AsignacionPersistenceAdapter adapter;

    @BeforeEach
    void preparar() {
        adapter = new AsignacionPersistenceAdapter(jpa,
                Mappers.getMapper(AsignacionPersistenceMapper.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void guardaAsignacion() {
        AsignacionEntity entidad = new AsignacionEntity();
        entidad.setId(1L);
        entidad.setTicketId(1L);
        entidad.setTecnicoId(10L);
        entidad.setDisponibilidad(Disponibilidad.ALTA);
        entidad.setFechaAsignacion(Instant.parse("2026-01-01T10:00:00Z"));
        entidad.setActiva(true);
        when(jpa.save(any(AsignacionEntity.class))).thenReturn(entidad);

        Asignacion resultado = adapter.guardar(new Asignacion(null, 1L, 10L, "TEC-10",
                Disponibilidad.ALTA, 2, Instant.parse("2026-01-01T10:00:00Z"), true, "admin"));

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.tecnicoId()).isEqualTo(10L);
        ArgumentCaptor<AsignacionEntity> captor = ArgumentCaptor.forClass(AsignacionEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getTecnicoSugeridoExterno()).isEqualTo("TEC-10");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void desactivaAsignacionesActivas() {
        adapter.desactivarActivas(1L);

        verify(jpa).desactivarActivas(1L);
    }
}
