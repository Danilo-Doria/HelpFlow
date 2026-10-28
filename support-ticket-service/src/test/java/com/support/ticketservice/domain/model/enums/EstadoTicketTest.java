package com.support.ticketservice.domain.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoTicketTest {

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void permiteSoloLasTransicionesDeclaradas() {
        assertThat(EstadoTicket.ABIERTO.puedePasarA(EstadoTicket.ASIGNADO)).isTrue();
        assertThat(EstadoTicket.ABIERTO.puedePasarA(EstadoTicket.CANCELADO)).isTrue();
        assertThat(EstadoTicket.ABIERTO.puedePasarA(EstadoTicket.RESUELTO)).isFalse();

        assertThat(EstadoTicket.ASIGNADO.puedePasarA(EstadoTicket.ASIGNADO)).isTrue();
        assertThat(EstadoTicket.ASIGNADO.puedePasarA(EstadoTicket.EN_PROGRESO)).isTrue();
        assertThat(EstadoTicket.EN_PROGRESO.puedePasarA(EstadoTicket.RESUELTO)).isTrue();
        assertThat(EstadoTicket.RESUELTO.puedePasarA(EstadoTicket.CERRADO)).isTrue();
        assertThat(EstadoTicket.RESUELTO.puedePasarA(EstadoTicket.EN_PROGRESO)).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void clasificaEstadosActivosTerminalesYDeSla() {
        assertThat(EstadoTicket.ASIGNADO.esActivo()).isTrue();
        assertThat(EstadoTicket.EN_PROGRESO.esActivo()).isTrue();
        assertThat(EstadoTicket.ABIERTO.esActivo()).isFalse();

        assertThat(EstadoTicket.CERRADO.esTerminal()).isTrue();
        assertThat(EstadoTicket.CANCELADO.esTerminal()).isTrue();
        assertThat(EstadoTicket.RESUELTO.esTerminal()).isFalse();

        assertThat(EstadoTicket.ABIERTO.cuentaParaSla()).isTrue();
        assertThat(EstadoTicket.ASIGNADO.cuentaParaSla()).isTrue();
        assertThat(EstadoTicket.EN_PROGRESO.cuentaParaSla()).isTrue();
        assertThat(EstadoTicket.RESUELTO.cuentaParaSla()).isFalse();
    }
}
