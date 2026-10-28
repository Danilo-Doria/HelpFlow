package com.support.ticketservice.domain.model;

import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    private Usuario usuario() {
        return new Usuario(1L, "123", "Ana", "ana@helpflow.com",
                "BOGOTA", "TI", EstadoRegistro.ACTIVO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void estaActivoSegunEstado() {
        assertThat(usuario().estaActivo()).isTrue();
        assertThat(usuario().conEstado(EstadoRegistro.INACTIVO).estaActivo()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void actualizarConservaIdentidadYEstado() {
        Usuario actualizado = usuario().actualizar("Ana Maria", "ana@nuevo.com", "MEDELLIN", "SOPORTE");

        assertThat(actualizado.id()).isEqualTo(1L);
        assertThat(actualizado.documento()).isEqualTo("123");
        assertThat(actualizado.nombre()).isEqualTo("Ana Maria");
        assertThat(actualizado.correo()).isEqualTo("ana@nuevo.com");
        assertThat(actualizado.sede()).isEqualTo("MEDELLIN");
        assertThat(actualizado.area()).isEqualTo("SOPORTE");
        assertThat(actualizado.estado()).isEqualTo(EstadoRegistro.ACTIVO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void conEstadoCambiaSoloElEstado() {
        Usuario inactivo = usuario().conEstado(EstadoRegistro.INACTIVO);

        assertThat(inactivo.estado()).isEqualTo(EstadoRegistro.INACTIVO);
        assertThat(inactivo.nombre()).isEqualTo("Ana");
    }
}
