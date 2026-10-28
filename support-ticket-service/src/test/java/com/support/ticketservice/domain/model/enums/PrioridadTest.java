package com.support.ticketservice.domain.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrioridadTest {

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void siguienteRecorreLaEscalaYTerminaEnCritica() {
        assertThat(Prioridad.BAJA.siguiente()).contains(Prioridad.MEDIA);
        assertThat(Prioridad.MEDIA.siguiente()).contains(Prioridad.ALTA);
        assertThat(Prioridad.ALTA.siguiente()).contains(Prioridad.CRITICA);
        assertThat(Prioridad.CRITICA.siguiente()).isEmpty();
    }
}
