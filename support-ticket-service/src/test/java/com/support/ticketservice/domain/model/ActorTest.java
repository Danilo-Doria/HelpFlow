package com.support.ticketservice.domain.model;

import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import com.support.ticketservice.domain.model.enums.Rol;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActorTest {

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void reconoceCadaRol() {
        Actor admin = new Actor("admin", Rol.ROLE_ADMIN, null, null);
        Actor tecnico = new Actor("tecnico", Rol.ROLE_TECNICO, null, 10L);
        Actor usuario = new Actor("usuario", Rol.ROLE_USUARIO, 20L, null);

        assertThat(admin.esAdmin()).isTrue();
        assertThat(admin.esTecnico()).isFalse();
        assertThat(admin.esUsuario()).isFalse();

        assertThat(tecnico.esAdmin()).isFalse();
        assertThat(tecnico.esTecnico()).isTrue();
        assertThat(tecnico.esUsuario()).isFalse();

        assertThat(usuario.esAdmin()).isFalse();
        assertThat(usuario.esTecnico()).isFalse();
        assertThat(usuario.esUsuario()).isTrue();
    }
}
