package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.out.HistorialRepositoryPort;
import com.support.ticketservice.application.port.out.TicketRepositoryPort;
import com.support.ticketservice.domain.exception.OperacionNoPermitidaException;
import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.domain.model.HistorialTicket;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.domain.model.enums.Rol;
import com.support.ticketservice.domain.service.TicketAccessPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.support.ticketservice.support.DomainFixtures.admin;
import static com.support.ticketservice.support.DomainFixtures.tecnico;
import static com.support.ticketservice.support.DomainFixtures.ticketAbierto;
import static com.support.ticketservice.support.DomainFixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarTicketsServiceTest {

    @Mock private TicketRepositoryPort tickets;
    @Mock private HistorialRepositoryPort historial;
    @Spy private TicketAccessPolicy policy = new TicketAccessPolicy();

    @InjectMocks private ConsultarTicketsService service;

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void obtieneTicketSiElActorTieneAcceso() {
        Ticket ticket = ticketAbierto(1L);
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticket));

        assertThat(service.obtener(1L, usuario())).isSameAs(ticket);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void fallaAlObtenerTicketInexistente() {
        when(tickets.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L, admin()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Ticket no encontrado");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaObtenerTicketAjenoSinPermiso() {
        Ticket ticket = ticketAbierto(1L);
        Actor otroUsuario = new Actor("luis@helpflow.com", Rol.ROLE_USUARIO, 2L, null);
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.obtener(1L, otroUsuario))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("No tienes acceso");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void listaSegunElRolDelActor() {
        Ticket ticket = ticketAbierto(1L);
        when(tickets.listarTodos()).thenReturn(List.of(ticket));
        when(tickets.listarPorSolicitante(1L)).thenReturn(List.of(ticket));
        when(tickets.listarPorTecnico(10L)).thenReturn(List.of(ticket));

        assertThat(service.listar(admin())).containsExactly(ticket);
        assertThat(service.listar(usuario())).containsExactly(ticket);
        assertThat(service.listar(tecnico())).containsExactly(ticket);

        verify(tickets).listarTodos();
        verify(tickets).listarPorSolicitante(1L);
        verify(tickets).listarPorTecnico(10L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void devuelveHistorialCuandoElActorPuedeVerElTicket() {
        Ticket ticket = ticketAbierto(1L);
        HistorialTicket entrada = new HistorialTicket(1L, 1L, null, EstadoTicket.ABIERTO,
                null, "Creado", "ana@helpflow.com", ticket.getFechaCreacion());
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticket));
        when(historial.listarPorTicket(1L)).thenReturn(List.of(entrada));

        assertThat(service.historial(1L, usuario())).containsExactly(entrada);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void noConsultaHistorialSiNoPuedeVerElTicket() {
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticketAbierto(1L)));
        Actor otroUsuario = new Actor("luis@helpflow.com", Rol.ROLE_USUARIO, 2L, null);

        assertThatThrownBy(() -> service.historial(1L, otroUsuario))
                .isInstanceOf(OperacionNoPermitidaException.class);

        verify(historial, org.mockito.Mockito.never()).listarPorTicket(1L);
    }
}
