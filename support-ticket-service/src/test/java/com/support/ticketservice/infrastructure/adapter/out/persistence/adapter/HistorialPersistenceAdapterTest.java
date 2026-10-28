package com.support.ticketservice.infrastructure.adapter.out.persistence.adapter;

import com.support.ticketservice.domain.model.HistorialTicket;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.HistorialTicketEntity;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.TicketEntity;
import com.support.ticketservice.infrastructure.adapter.out.persistence.mapper.HistorialPersistenceMapper;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.HistorialJpaRepository;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.TicketJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistorialPersistenceAdapterTest {

    @Mock private HistorialJpaRepository jpa;
    @Mock private TicketJpaRepository ticketJpa;
    private HistorialPersistenceAdapter adapter;

    @BeforeEach
    void preparar() {
        adapter = new HistorialPersistenceAdapter(jpa, ticketJpa,
                Mappers.getMapper(HistorialPersistenceMapper.class));
    }

    private HistorialTicket historial(Long id) {
        return new HistorialTicket(id, 1L, null, EstadoTicket.ABIERTO,
                null, "Creado", "ana@helpflow.com", Instant.parse("2026-01-01T10:00:00Z"));
    }

    private HistorialTicketEntity entidad() {
        HistorialTicketEntity e = new HistorialTicketEntity();
        e.setId(1L);
        TicketEntity ticket = new TicketEntity();
        ticket.setId(1L);
        e.setTicket(ticket);
        e.setEstadoNuevo(EstadoTicket.ABIERTO);
        e.setObservacion("Creado");
        e.setRealizadoPor("ana@helpflow.com");
        e.setFecha(Instant.parse("2026-01-01T10:00:00Z"));
        return e;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void guardaHistorialResolviendoElTicket() {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(1L);
        when(ticketJpa.getReferenceById(1L)).thenReturn(ticket);
        when(jpa.save(any(HistorialTicketEntity.class))).thenReturn(entidad());

        HistorialTicket resultado = adapter.guardar(historial(null));

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.ticketId()).isEqualTo(1L);
        assertThat(resultado.observacion()).isEqualTo("Creado");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void listaHistorialPorTicket() {
        when(jpa.findByTicketIdOrderByFechaAsc(1L)).thenReturn(List.of(entidad()));

        assertThat(adapter.listarPorTicket(1L)).hasSize(1);
    }
}
