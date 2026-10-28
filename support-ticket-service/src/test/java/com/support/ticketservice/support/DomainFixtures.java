package com.support.ticketservice.support;

import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.domain.model.Tecnico;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.model.Usuario;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.domain.model.enums.Rol;
import com.support.ticketservice.domain.service.SlaPolicy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public final class DomainFixtures {

    public static final Instant AHORA = Instant.parse("2026-01-10T10:00:00Z");
    public static final String SECRET = "clave-de-prueba-de-al-menos-32-bytes!!";

    private DomainFixtures() {
    }

    public static SlaPolicy sla() {
        return new SlaPolicy(Map.of(
                Prioridad.BAJA, Duration.ofHours(72),
                Prioridad.MEDIA, Duration.ofHours(24),
                Prioridad.ALTA, Duration.ofHours(8),
                Prioridad.CRITICA, Duration.ofHours(4)));
    }

    public static Usuario usuarioActivo() {
        return new Usuario(1L, "123456", "Ana Perez", "ana@helpflow.com",
                "BOGOTA", "TI", EstadoRegistro.ACTIVO);
    }

    public static Usuario usuarioInactivo() {
        return usuarioActivo().conEstado(EstadoRegistro.INACTIVO);
    }

    public static Tecnico tecnicoActivo(Long id, Categoria categoria) {
        return new Tecnico(id, "T-" + id, "Tecnico " + id, new BigDecimal("3500000"),
                LocalDate.of(2020, 1, 1), EstadoRegistro.ACTIVO, categoria, "BOGOTA", 5);
    }

    public static Tecnico tecnicoInactivo(Long id, Categoria categoria) {
        return tecnicoActivo(id, categoria).conEstado(EstadoRegistro.INACTIVO);
    }

    public static Ticket ticketAbierto() {
        return Ticket.abrir("No enciende", "El equipo no enciende",
                Categoria.HARDWARE, Prioridad.ALTA, "BOGOTA", "PC-01",
                usuarioActivo(), sla(), AHORA);
    }

    public static Ticket ticketAbierto(Long id) {
        Ticket ticket = ticketAbierto();
        ticket.setId(id);
        return ticket;
    }

    public static Ticket ticketAsignado() {
        Ticket ticket = ticketAbierto(1L);
        ticket.asignar(tecnicoActivo(10L, Categoria.HARDWARE));
        return ticket;
    }

    public static Ticket ticketEnEstado(Long id, EstadoTicket estado, Prioridad prioridad) {
        return new Ticket(id, "No enciende", "El equipo no enciende",
                Categoria.HARDWARE, prioridad, "BOGOTA", "PC-01",
                AHORA, AHORA.plus(Duration.ofHours(8)), estado,
                usuarioActivo(), tecnicoActivo(10L, Categoria.HARDWARE),
                false, null, null);
    }

    public static Ticket ticketVencido(Long id) {
        return new Ticket(id, "No enciende", "El equipo no enciende",
                Categoria.HARDWARE, Prioridad.ALTA, "BOGOTA", "PC-01",
                AHORA.minus(Duration.ofHours(10)), AHORA.minus(Duration.ofHours(1)),
                EstadoTicket.ABIERTO, usuarioActivo(), null, false, null, null);
    }

    public static Actor admin() {
        return new Actor("admin@helpflow.com", Rol.ROLE_ADMIN, null, null);
    }

    public static Actor usuario() {
        return new Actor("ana@helpflow.com", Rol.ROLE_USUARIO, 1L, null);
    }

    public static Actor tecnico() {
        return new Actor("tecnico@helpflow.com", Rol.ROLE_TECNICO, null, 10L);
    }
}
