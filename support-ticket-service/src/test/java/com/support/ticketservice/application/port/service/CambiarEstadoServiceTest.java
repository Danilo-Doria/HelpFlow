package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.out.HistorialRepositoryPort;
import com.support.ticketservice.application.port.out.TicketMetricsPort;
import com.support.ticketservice.application.port.out.TicketRepositoryPort;
import com.support.ticketservice.domain.exception.OperacionNoPermitidaException;
import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.exception.TransicionInvalidaException;
import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.domain.model.HistorialTicket;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.domain.model.enums.Rol;
import com.support.ticketservice.domain.service.TicketAccessPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static com.support.ticketservice.support.DomainFixtures.AHORA;
import static com.support.ticketservice.support.DomainFixtures.admin;
import static com.support.ticketservice.support.DomainFixtures.ticketAsignado;
import static com.support.ticketservice.support.DomainFixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CambiarEstadoServiceTest {

    @Mock private TicketRepositoryPort tickets;
    @Mock private HistorialRepositoryPort historial;
    @Mock private TicketMetricsPort metrics;
    @Mock private Clock clock;
    @Spy private TicketAccessPolicy policy = new TicketAccessPolicy();

    @InjectMocks private CambiarEstadoService service;

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void pasaAEnProgresoYRegistraPrimeraRespuesta() {
        Ticket ticket = ticketAsignado();
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticket));
        when(clock.instant()).thenReturn(AHORA.plus(Duration.ofMinutes(45)));
        when(tickets.guardar(ticket)).thenReturn(ticket);

        Ticket resultado = service.ejecutar(1L, EstadoTicket.EN_PROGRESO,
                "Iniciando diagnóstico", admin());

        assertThat(resultado.getEstado()).isEqualTo(EstadoTicket.EN_PROGRESO);
        assertThat(resultado.getFechaPrimeraRespuesta()).isEqualTo(AHORA.plus(Duration.ofMinutes(45)));
        verify(metrics).primeraRespuesta(Categoria.HARDWARE, Prioridad.ALTA,
                Duration.ofMinutes(45));

        ArgumentCaptor<HistorialTicket> captor = ArgumentCaptor.forClass(HistorialTicket.class);
        verify(historial).guardar(captor.capture());
        assertThat(captor.getValue().estadoAnterior()).isEqualTo(EstadoTicket.ASIGNADO);
        assertThat(captor.getValue().estadoNuevo()).isEqualTo(EstadoTicket.EN_PROGRESO);
        assertThat(captor.getValue().observacion()).isEqualTo("Iniciando diagnóstico");
        assertThat(captor.getValue().tecnicoId()).isEqualTo(10L);
        assertThat(captor.getValue().realizadoPor()).isEqualTo("admin@helpflow.com");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void resuelveYRegistraMetricaDeResolucion() {
        Ticket ticket = ticketAsignado();
        ticket.cambiarEstado(EstadoTicket.EN_PROGRESO, AHORA.plus(Duration.ofMinutes(10)));
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticket));
        when(clock.instant()).thenReturn(AHORA.plus(Duration.ofHours(2)));
        when(tickets.guardar(ticket)).thenReturn(ticket);

        Ticket resultado = service.ejecutar(1L, EstadoTicket.RESUELTO,
                "Equipo reparado", admin());

        assertThat(resultado.getEstado()).isEqualTo(EstadoTicket.RESUELTO);
        assertThat(resultado.getFechaResolucion()).isEqualTo(AHORA.plus(Duration.ofHours(2)));
        verify(metrics).resolucion(Categoria.HARDWARE, Prioridad.ALTA, Duration.ofHours(2));
        verify(metrics, never()).primeraRespuesta(any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void noDuplicaMetricaDePrimeraRespuestaSiYaExistia() {
        Ticket ticket = ticketAsignado();
        ticket.cambiarEstado(EstadoTicket.EN_PROGRESO, AHORA.plus(Duration.ofMinutes(10)));
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticket));
        when(clock.instant()).thenReturn(AHORA.plus(Duration.ofHours(1)));
        when(tickets.guardar(ticket)).thenReturn(ticket);

        service.ejecutar(1L, EstadoTicket.RESUELTO, "Listo", admin());

        verify(metrics, never()).primeraRespuesta(any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void fallaCuandoElTicketNoExiste() {
        when(tickets.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ejecutar(99L, EstadoTicket.EN_PROGRESO,
                "x", admin()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Ticket no encontrado");

        verifyNoInteractions(historial, metrics);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaCambioCuandoElActorNoPuedeVerElTicket() {
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticketAsignado()));
        Actor otroUsuario = new Actor("luis@helpflow.com", Rol.ROLE_USUARIO, 2L, null);

        assertThatThrownBy(() -> service.ejecutar(1L, EstadoTicket.CANCELADO,
                "x", otroUsuario))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("No tienes acceso");

        verify(tickets, never()).guardar(any());
        verifyNoInteractions(historial, metrics);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaTransicionQueElRolNoTienePermitida() {
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticketAsignado()));

        assertThatThrownBy(() -> service.ejecutar(1L, EstadoTicket.EN_PROGRESO,
                "x", usuario()))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("Tu rol no puede");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void propagaLaTransicionInvalidaDelDominio() {
        Ticket ticket = ticketAsignado();
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticket));
        when(clock.instant()).thenReturn(AHORA);

        assertThatThrownBy(() -> service.ejecutar(1L, EstadoTicket.CERRADO,
                "x", admin()))
                .isInstanceOf(TransicionInvalidaException.class);

        verify(tickets, never()).guardar(any());
    }
}
