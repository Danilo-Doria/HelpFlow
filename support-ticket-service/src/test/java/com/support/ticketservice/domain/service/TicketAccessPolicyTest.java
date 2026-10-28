package com.support.ticketservice.domain.service;

import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.domain.model.Tecnico;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.model.Usuario;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.domain.model.enums.Rol;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TicketAccessPolicyTest {

    private final TicketAccessPolicy policy = new TicketAccessPolicy();

    private Ticket ticket(Long solicitanteId, Long tecnicoId) {
        Usuario usuario = new Usuario(solicitanteId, "U-" + solicitanteId, "Usuario",
                "u@helpflow.com", "BOGOTA", "TI", EstadoRegistro.ACTIVO);
        Tecnico tecnico = tecnicoId == null ? null
                : new Tecnico(tecnicoId, "T-" + tecnicoId, "Tecnico", BigDecimal.TEN,
                LocalDate.of(2020, 1, 1), EstadoRegistro.ACTIVO,
                Categoria.HARDWARE, "BOGOTA", 5);
        return new Ticket(1L, "T", "D", Categoria.HARDWARE, Prioridad.ALTA,
                "BOGOTA", null, Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T18:00:00Z"), EstadoTicket.ASIGNADO,
                usuario, tecnico, false, null, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void adminPuedeVerCualquierTicket() {
        Actor admin = new Actor("admin", Rol.ROLE_ADMIN, null, null);

        assertThat(policy.puedeVer(ticket(1L, null), admin)).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void usuarioSoloVeSusPropiosTickets() {
        Actor dueno = new Actor("ana", Rol.ROLE_USUARIO, 1L, null);
        Actor otro = new Actor("luis", Rol.ROLE_USUARIO, 2L, null);

        assertThat(policy.puedeVer(ticket(1L, null), dueno)).isTrue();
        assertThat(policy.puedeVer(ticket(1L, null), otro)).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void tecnicoSoloVeTicketsAsignadosASuId() {
        Actor asignado = new Actor("tecnico", Rol.ROLE_TECNICO, null, 10L);
        Actor otro = new Actor("otro", Rol.ROLE_TECNICO, null, 11L);

        assertThat(policy.puedeVer(ticket(1L, 10L), asignado)).isTrue();
        assertThat(policy.puedeVer(ticket(1L, 10L), otro)).isFalse();
        assertThat(policy.puedeVer(ticket(1L, null), asignado)).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void adminPuedeCambiarACualquierEstado() {
        Actor admin = new Actor("admin", Rol.ROLE_ADMIN, null, null);
        Ticket ticket = ticket(1L, null);

        assertThat(policy.puedeCambiarA(ticket, admin, EstadoTicket.CANCELADO)).isTrue();
        assertThat(policy.puedeCambiarA(ticket, admin, EstadoTicket.EN_PROGRESO)).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void tecnicoSoloPuedePasarAEnProgresoOResuelto() {
        Actor tecnico = new Actor("tecnico", Rol.ROLE_TECNICO, null, 10L);
        Ticket ticket = ticket(1L, 10L);

        assertThat(policy.puedeCambiarA(ticket, tecnico, EstadoTicket.EN_PROGRESO)).isTrue();
        assertThat(policy.puedeCambiarA(ticket, tecnico, EstadoTicket.RESUELTO)).isTrue();
        assertThat(policy.puedeCambiarA(ticket, tecnico, EstadoTicket.CANCELADO)).isFalse();
        assertThat(policy.puedeCambiarA(ticket, tecnico, EstadoTicket.CERRADO)).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void usuarioSoloPuedeCancelarOCerrarSusTickets() {
        Actor usuario = new Actor("ana", Rol.ROLE_USUARIO, 1L, null);
        Ticket ticket = ticket(1L, 10L);

        assertThat(policy.puedeCambiarA(ticket, usuario, EstadoTicket.CANCELADO)).isTrue();
        assertThat(policy.puedeCambiarA(ticket, usuario, EstadoTicket.CERRADO)).isTrue();
        assertThat(policy.puedeCambiarA(ticket, usuario, EstadoTicket.EN_PROGRESO)).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void sinAccesoAlTicketTampocoPuedeCambiarlo() {
        Actor otro = new Actor("luis", Rol.ROLE_USUARIO, 2L, null);

        assertThat(policy.puedeCambiarA(ticket(1L, null), otro, EstadoTicket.CANCELADO)).isFalse();
    }
}
