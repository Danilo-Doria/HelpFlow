package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.out.AsignacionRepositoryPort;
import com.support.ticketservice.application.port.out.HistorialRepositoryPort;
import com.support.ticketservice.application.port.out.TecnicoRepositoryPort;
import com.support.ticketservice.application.port.out.TicketRepositoryPort;
import com.support.ticketservice.domain.exception.AsignacionInvalidaException;
import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.model.Asignacion;
import com.support.ticketservice.domain.model.HistorialTicket;
import com.support.ticketservice.domain.model.Tecnico;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Optional;

import static com.support.ticketservice.support.DomainFixtures.AHORA;
import static com.support.ticketservice.support.DomainFixtures.admin;
import static com.support.ticketservice.support.DomainFixtures.tecnicoActivo;
import static com.support.ticketservice.support.DomainFixtures.tecnicoInactivo;
import static com.support.ticketservice.support.DomainFixtures.ticketAbierto;
import static com.support.ticketservice.support.DomainFixtures.ticketAsignado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReasignarTicketServiceTest {

    @Mock private TicketRepositoryPort tickets;
    @Mock private TecnicoRepositoryPort tecnicos;
    @Mock private AsignacionRepositoryPort asignaciones;
    @Mock private HistorialRepositoryPort historial;
    @Mock private Clock clock;
    @InjectMocks private ReasignarTicketService service;

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void asignaManualmenteUnTicketSinTecnico() {
        Ticket ticket = ticketAbierto(1L);
        Tecnico nuevo = tecnicoActivo(20L, Categoria.HARDWARE);
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticket));
        when(tecnicos.buscarPorId(20L)).thenReturn(Optional.of(nuevo));
        when(tickets.contarActivosDeTecnico(20L)).thenReturn(1L);
        when(clock.instant()).thenReturn(AHORA);
        when(tickets.guardar(ticket)).thenReturn(ticket);

        Ticket resultado = service.ejecutar(1L, 20L, null, admin());

        assertThat(resultado.getEstado()).isEqualTo(EstadoTicket.ASIGNADO);
        assertThat(resultado.getTecnicoAsignado()).isSameAs(nuevo);

        verify(asignaciones).desactivarActivas(1L);
        ArgumentCaptor<Asignacion> asignacion = ArgumentCaptor.forClass(Asignacion.class);
        verify(asignaciones).guardar(asignacion.capture());
        assertThat(asignacion.getValue().tecnicoId()).isEqualTo(20L);
        assertThat(asignacion.getValue().fechaAsignacion()).isEqualTo(AHORA);
        assertThat(asignacion.getValue().asignadoPor()).isEqualTo("admin@helpflow.com");

        ArgumentCaptor<HistorialTicket> entry = ArgumentCaptor.forClass(HistorialTicket.class);
        verify(historial).guardar(entry.capture());
        assertThat(entry.getValue().estadoAnterior()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(entry.getValue().estadoNuevo()).isEqualTo(EstadoTicket.ASIGNADO);
        assertThat(entry.getValue().observacion()).isEqualTo("Asignado manualmente a Tecnico 20");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void reasignaYAgregaObservacionAlHistorial() {
        Ticket ticket = ticketAsignado();
        Tecnico nuevo = tecnicoActivo(20L, Categoria.HARDWARE);
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticket));
        when(tecnicos.buscarPorId(20L)).thenReturn(Optional.of(nuevo));
        when(tickets.contarActivosDeTecnico(20L)).thenReturn(0L);
        when(clock.instant()).thenReturn(AHORA);
        when(tickets.guardar(ticket)).thenReturn(ticket);

        service.ejecutar(1L, 20L, "  urgente  ", admin());

        ArgumentCaptor<HistorialTicket> entry = ArgumentCaptor.forClass(HistorialTicket.class);
        verify(historial).guardar(entry.capture());
        assertThat(entry.getValue().observacion())
                .isEqualTo("Reasignado de Tecnico 10 a Tecnico 20. urgente");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void observacionEnBlancoNoAgregaSufijo() {
        Ticket ticket = ticketAsignado();
        Tecnico nuevo = tecnicoActivo(20L, Categoria.HARDWARE);
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticket));
        when(tecnicos.buscarPorId(20L)).thenReturn(Optional.of(nuevo));
        when(tickets.contarActivosDeTecnico(20L)).thenReturn(0L);
        when(clock.instant()).thenReturn(AHORA);
        when(tickets.guardar(ticket)).thenReturn(ticket);

        service.ejecutar(1L, 20L, "   ", admin());

        ArgumentCaptor<HistorialTicket> entry = ArgumentCaptor.forClass(HistorialTicket.class);
        verify(historial).guardar(entry.capture());
        assertThat(entry.getValue().observacion())
                .isEqualTo("Reasignado de Tecnico 10 a Tecnico 20");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void fallaSiElTicketNoExiste() {
        when(tickets.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ejecutar(99L, 20L, null, admin()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Ticket no encontrado");

        verifyNoInteractions(tecnicos, asignaciones, historial);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void fallaSiElTecnicoNoExiste() {
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticketAbierto(1L)));
        when(tecnicos.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ejecutar(1L, 99L, null, admin()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Técnico no encontrado");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaAsignarAlMismoTecnico() {
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticketAsignado()));
        when(tecnicos.buscarPorId(10L)).thenReturn(Optional.of(
                tecnicoActivo(10L, Categoria.HARDWARE)));

        assertThatThrownBy(() -> service.ejecutar(1L, 10L, null, admin()))
                .isInstanceOf(AsignacionInvalidaException.class)
                .hasMessageContaining("ya está asignado");

        verify(tickets, never()).guardar(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaTecnicoQueAlcanzoSuCargaMaxima() {
        Ticket ticket = ticketAbierto(1L);
        Tecnico nuevo = tecnicoActivo(20L, Categoria.HARDWARE);
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticket));
        when(tecnicos.buscarPorId(20L)).thenReturn(Optional.of(nuevo));
        when(tickets.contarActivosDeTecnico(20L)).thenReturn(5L);

        assertThatThrownBy(() -> service.ejecutar(1L, 20L, null, admin()))
                .isInstanceOf(AsignacionInvalidaException.class)
                .hasMessageContaining("carga máxima");

        verify(tickets, never()).guardar(any());
        verifyNoInteractions(asignaciones, historial);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaTecnicoInactivoODeOtraEspecialidad() {
        Ticket ticket = ticketAbierto(1L);
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticket));
        when(tecnicos.buscarPorId(30L)).thenReturn(Optional.of(
                tecnicoInactivo(30L, Categoria.HARDWARE)));

        assertThatThrownBy(() -> service.ejecutar(1L, 30L, null, admin()))
                .isInstanceOf(AsignacionInvalidaException.class);

        verify(tickets, never()).guardar(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaTicketEnEstadoQueNoPermiteAsignacion() {
        Ticket cancelado = com.support.ticketservice.support.DomainFixtures
                .ticketEnEstado(1L, EstadoTicket.CANCELADO, com.support.ticketservice.domain.model.enums.Prioridad.ALTA);
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(cancelado));
        when(tecnicos.buscarPorId(20L)).thenReturn(Optional.of(
                tecnicoActivo(20L, Categoria.HARDWARE)));

        assertThatThrownBy(() -> service.ejecutar(1L, 20L, null, admin()))
                .isInstanceOf(com.support.ticketservice.domain.exception.TransicionInvalidaException.class);

        verify(tickets, never()).guardar(any());
    }
}
