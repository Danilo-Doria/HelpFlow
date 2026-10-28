package com.support.ticketservice.domain.model;

import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TecnicoConCargaTest {

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void tieneCapacidadDelegaEnElTecnico() {
        Tecnico tecnico = new Tecnico(1L, "T-1", "Ana", BigDecimal.TEN,
                LocalDate.of(2020, 1, 1), EstadoRegistro.ACTIVO,
                Categoria.HARDWARE, "BOGOTA", 2);

        assertThat(new TecnicoConCarga(tecnico, 1).tieneCapacidad()).isTrue();
        assertThat(new TecnicoConCarga(tecnico, 2).tieneCapacidad()).isFalse();
    }
}
