package com.support.ticketservice.domain.model;

import com.support.ticketservice.domain.exception.AsignacionInvalidaException;
import com.support.ticketservice.domain.exception.TransicionInvalidaException;
import com.support.ticketservice.domain.exception.UsuarioInactivoException;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.domain.service.SlaPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketTest {

    private static final Instant AHORA = Instant.parse("2026-01-10T10:00:00Z");
    private final SlaPolicy sla = new SlaPolicy(Map.of(
            Prioridad.BAJA, Duration.ofHours(72),
            Prioridad.MEDIA, Duration.ofHours(24),
            Prioridad.ALTA, Duration.ofHours(8),
            Prioridad.CRITICA, Duration.ofHours(4)));

    private Usuario usuarioActivo() {
        return new Usuario(1L, "123", "Ana", "ana@helpflow.com",
                "BOGOTA", "TI", EstadoRegistro.ACTIVO);
    }

    private Tecnico tecnico(Categoria categoria, EstadoRegistro estado, int cargaMaxima) {
        return new Tecnico(10L, "T-10", "Tecnico Diez", new BigDecimal("3500000"),
                LocalDate.of(2020, 1, 1), estado, categoria, "BOGOTA", cargaMaxima);
    }

    private Ticket ticketAbierto() {
        return Ticket.abrir("No enciende", "El equipo no enciende",
                Categoria.HARDWARE, Prioridad.ALTA, "BOGOTA", "PC-01",
                usuarioActivo(), sla, AHORA);
    }

    private Ticket ticketEnEstado(EstadoTicket estado) {
        return new Ticket(1L, "No enciende", "El equipo no enciende",
                Categoria.HARDWARE, Prioridad.ALTA, "BOGOTA", "PC-01",
                AHORA, AHORA.plus(Duration.ofHours(8)), estado,
                usuarioActivo(), tecnico(Categoria.HARDWARE, EstadoRegistro.ACTIVO, 5),
                false, null, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void abrirCreaTicketAbiertoConSlaCalculado() {
        Ticket ticket = ticketAbierto();

        assertThat(ticket.getId()).isNull();
        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(ticket.getTecnicoAsignado()).isNull();
        assertThat(ticket.getFechaLimiteSla()).isEqualTo(AHORA.plus(Duration.ofHours(8)));
        assertThat(ticket.getFechaCreacion()).isEqualTo(AHORA);
        assertThat(ticket.isEscalado()).isFalse();
        assertThat(ticket.getActivoAfectado()).isEqualTo("PC-01");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void abrirRechazaUsuarioNuloOInactivo() {
        assertThatThrownBy(() -> Ticket.abrir("T", "D", Categoria.RED, Prioridad.BAJA,
                "S", null, null, sla, AHORA))
                .isInstanceOf(UsuarioInactivoException.class);

        Usuario inactivo = usuarioActivo().conEstado(EstadoRegistro.INACTIVO);
        assertThatThrownBy(() -> Ticket.abrir("T", "D", Categoria.RED, Prioridad.BAJA,
                "S", null, inactivo, sla, AHORA))
                .isInstanceOf(UsuarioInactivoException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void constructorValidaCamposObligatoriosYOrdenDeFechas() {
        assertThatThrownBy(() -> new Ticket(1L, null, "D", Categoria.RED, Prioridad.BAJA,
                "S", null, AHORA, AHORA.plusSeconds(1), EstadoTicket.ABIERTO,
                usuarioActivo(), null, false, null, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new Ticket(1L, "T", "D", Categoria.RED, Prioridad.BAJA,
                "S", null, AHORA, AHORA, EstadoTicket.ABIERTO,
                usuarioActivo(), null, false, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posterior");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void asignarCambiaAAssignadoCuandoTecnicoEsCompatible() {
        Ticket ticket = ticketAbierto();
        Tecnico tecnico = tecnico(Categoria.HARDWARE, EstadoRegistro.ACTIVO, 5);

        ticket.asignar(tecnico);

        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.ASIGNADO);
        assertThat(ticket.getTecnicoAsignado()).isEqualTo(tecnico);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void asignarRechazaTecnicoInactivoOEspecialidadIncorrecta() {
        Ticket ticket = ticketAbierto();

        assertThatThrownBy(() -> ticket.asignar(
                tecnico(Categoria.HARDWARE, EstadoRegistro.INACTIVO, 5)))
                .isInstanceOf(AsignacionInvalidaException.class)
                .hasMessageContaining("INACTIVO");

        assertThatThrownBy(() -> ticket.asignar(
                tecnico(Categoria.SOFTWARE, EstadoRegistro.ACTIVO, 5)))
                .isInstanceOf(AsignacionInvalidaException.class)
                .hasMessageContaining("especialidad");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void asignarRechazaTransicionInvalidaDesdeEstadoTerminal() {
        Ticket cerrado = ticketEnEstado(EstadoTicket.CERRADO);

        assertThatThrownBy(() -> cerrado.asignar(
                tecnico(Categoria.HARDWARE, EstadoRegistro.ACTIVO, 5)))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void cambiarEstadoAEnProgresoRegistraPrimeraRespuesta() {
        Ticket ticket = ticketAbierto();
        ticket.asignar(tecnico(Categoria.HARDWARE, EstadoRegistro.ACTIVO, 5));

        Instant respuesta = AHORA.plus(Duration.ofMinutes(30));
        ticket.cambiarEstado(EstadoTicket.EN_PROGRESO, respuesta);

        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.EN_PROGRESO);
        assertThat(ticket.getFechaPrimeraRespuesta()).isEqualTo(respuesta);
        assertThat(ticket.getFechaResolucion()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void cambiarEstadoAResueltoRegistraFechaDeResolucion() {
        Ticket ticket = ticketAbierto();
        ticket.asignar(tecnico(Categoria.HARDWARE, EstadoRegistro.ACTIVO, 5));
        ticket.cambiarEstado(EstadoTicket.EN_PROGRESO, AHORA.plusSeconds(60));

        Instant resolucion = AHORA.plus(Duration.ofHours(2));
        ticket.cambiarEstado(EstadoTicket.RESUELTO, resolucion);

        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.RESUELTO);
        assertThat(ticket.getFechaResolucion()).isEqualTo(resolucion);
        assertThat(ticket.getFechaPrimeraRespuesta()).isEqualTo(AHORA.plusSeconds(60));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void reaperturaDesdeResueltoLimpiaFechaDeResolucion() {
        Ticket resuelto = new Ticket(1L, "T", "D", Categoria.HARDWARE, Prioridad.ALTA,
                "BOGOTA", null, AHORA, AHORA.plus(Duration.ofHours(8)),
                EstadoTicket.RESUELTO, usuarioActivo(),
                tecnico(Categoria.HARDWARE, EstadoRegistro.ACTIVO, 5), false,
                AHORA.plusSeconds(10), AHORA.plusSeconds(20));

        resuelto.cambiarEstado(EstadoTicket.EN_PROGRESO, AHORA.plusSeconds(30));

        assertThat(resuelto.getEstado()).isEqualTo(EstadoTicket.EN_PROGRESO);
        assertThat(resuelto.getFechaResolucion()).isNull();
        assertThat(resuelto.getFechaPrimeraRespuesta()).isEqualTo(AHORA.plusSeconds(10));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void cambiarEstadoRechazaDestinoAsignadoYTransicionesNoPermitidas() {
        Ticket ticket = ticketAbierto();

        assertThatThrownBy(() -> ticket.cambiarEstado(EstadoTicket.ASIGNADO, AHORA))
                .isInstanceOf(TransicionInvalidaException.class);

        assertThatThrownBy(() -> ticket.cambiarEstado(EstadoTicket.RESUELTO, AHORA))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void estaVencidoSoloCuentaEnEstadosConSla() {
        Ticket abierto = ticketAbierto();
        assertThat(abierto.estaVencido(AHORA.plus(Duration.ofHours(9)))).isTrue();
        assertThat(abierto.estaVencido(AHORA.plus(Duration.ofHours(1)))).isFalse();

        Ticket cerrado = ticketEnEstado(EstadoTicket.CERRADO);
        assertThat(cerrado.estaVencido(AHORA.plus(Duration.ofDays(30)))).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void proximoAVencerEvaluaUmbralYExclusionDeVencidos() {
        Ticket ticket = ticketAbierto();

        assertThat(ticket.proximoAVencer(AHORA.plus(Duration.ofHours(6)), Duration.ofHours(3))).isTrue();
        assertThat(ticket.proximoAVencer(AHORA.plus(Duration.ofHours(6)), Duration.ofHours(1))).isFalse();
        assertThat(ticket.proximoAVencer(AHORA.plus(Duration.ofHours(10)), Duration.ofHours(3))).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void necesitaEscaladoConsideraVencimientoYMarcaPrevia() {
        Ticket ticket = ticketAbierto();
        assertThat(ticket.necesitaEscalado(AHORA.plus(Duration.ofHours(7)), Duration.ofHours(2))).isTrue();
        assertThat(ticket.necesitaEscalado(AHORA.plus(Duration.ofHours(1)), Duration.ofHours(2))).isFalse();

        ticket.escalar();
        assertThat(ticket.necesitaEscalado(AHORA.plus(Duration.ofHours(7)), Duration.ofHours(2))).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void escalarSubePrioridadUnaSolaVez() {
        Ticket ticket = Ticket.abrir("T", "D", Categoria.RED, Prioridad.BAJA,
                "BOGOTA", null, usuarioActivo(), sla, AHORA);

        assertThat(ticket.escalar()).isTrue();
        assertThat(ticket.getPrioridad()).isEqualTo(Prioridad.MEDIA);
        assertThat(ticket.isEscalado()).isTrue();
        assertThat(ticket.escalar()).isFalse();
        assertThat(ticket.getPrioridad()).isEqualTo(Prioridad.MEDIA);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void escalarEnPrioridadMaximaMarcaEscaladoSinCambiarPrioridad() {
        Ticket ticket = Ticket.abrir("T", "D", Categoria.RED, Prioridad.CRITICA,
                "BOGOTA", null, usuarioActivo(), sla, AHORA);

        assertThat(ticket.escalar()).isTrue();
        assertThat(ticket.getPrioridad()).isEqualTo(Prioridad.CRITICA);
        assertThat(ticket.isEscalado()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void escalarNoHaceNadaEnEstadoTerminal() {
        Ticket cancelado = ticketEnEstado(EstadoTicket.CANCELADO);

        assertThat(cancelado.escalar()).isFalse();
        assertThat(cancelado.isEscalado()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void exponeYActualizaIdentificador() {
        Ticket ticket = ticketAbierto();
        ticket.setId(99L);

        assertThat(ticket.getId()).isEqualTo(99L);
        assertThat(ticket.getTitulo()).isEqualTo("No enciende");
        assertThat(ticket.getDescripcion()).isEqualTo("El equipo no enciende");
        assertThat(ticket.getCategoria()).isEqualTo(Categoria.HARDWARE);
        assertThat(ticket.getPrioridad()).isEqualTo(Prioridad.ALTA);
        assertThat(ticket.getSede()).isEqualTo("BOGOTA");
        assertThat(ticket.getSolicitante().id()).isEqualTo(1L);
    }
}
