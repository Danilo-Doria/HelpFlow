package com.support.ticketservice.domain.model;

import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TecnicoTest {

    private Tecnico tecnico() {
        return new Tecnico(5L, "T-5", "Laura", new BigDecimal("3200000"),
                LocalDate.of(2021, 5, 10), EstadoRegistro.ACTIVO,
                Categoria.SOFTWARE, "BOGOTA", 3);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void evaluaActividadEspecialidadYCapacidad() {
        Tecnico tecnico = tecnico();

        assertThat(tecnico.estaActivo()).isTrue();
        assertThat(tecnico.puedeAtender(Categoria.SOFTWARE)).isTrue();
        assertThat(tecnico.puedeAtender(Categoria.RED)).isFalse();
        assertThat(tecnico.tieneCapacidad(2)).isTrue();
        assertThat(tecnico.tieneCapacidad(3)).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void actualizarConservaDatosInmutables() {
        Tecnico actualizado = tecnico().actualizar("Laura Gomez", new BigDecimal("4000000"),
                Categoria.RED, "MEDELLIN", 6);

        assertThat(actualizado.id()).isEqualTo(5L);
        assertThat(actualizado.documento()).isEqualTo("T-5");
        assertThat(actualizado.nombre()).isEqualTo("Laura Gomez");
        assertThat(actualizado.salario()).isEqualByComparingTo("4000000");
        assertThat(actualizado.especialidad()).isEqualTo(Categoria.RED);
        assertThat(actualizado.sede()).isEqualTo("MEDELLIN");
        assertThat(actualizado.cargaMaxima()).isEqualTo(6);
        assertThat(actualizado.fechaAfiliacion()).isEqualTo(LocalDate.of(2021, 5, 10));
        assertThat(actualizado.estado()).isEqualTo(EstadoRegistro.ACTIVO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void conEstadoCambiaSoloElEstado() {
        Tecnico inactivo = tecnico().conEstado(EstadoRegistro.INACTIVO);

        assertThat(inactivo.estaActivo()).isFalse();
        assertThat(inactivo.nombre()).isEqualTo("Laura");
    }
}
