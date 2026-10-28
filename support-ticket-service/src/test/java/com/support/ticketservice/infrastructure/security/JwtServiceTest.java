package com.support.ticketservice.infrastructure.security;

import com.support.ticketservice.application.port.out.TokenGeneratorPort.TokenEmitido;
import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.domain.model.Credencial;
import com.support.ticketservice.domain.model.enums.Rol;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.support.ticketservice.support.DomainFixtures.SECRET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private Credencial credencial() {
        return new Credencial(1L, "ana@helpflow.com", "hash",
                Rol.ROLE_USUARIO, true, 25L, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void generaYValidaUnTokenConLosClaimsDelActor() {
        JwtService service = new JwtService(SECRET, 60);

        TokenEmitido token = service.generar(credencial());
        Optional<Actor> actor = service.validar(token.token());

        assertThat(token.expiraEnSegundos()).isEqualTo(3600);
        assertThat(actor).isPresent();
        assertThat(actor.get().username()).isEqualTo("ana@helpflow.com");
        assertThat(actor.get().rol()).isEqualTo(Rol.ROLE_USUARIO);
        assertThat(actor.get().usuarioId()).isEqualTo(25L);
        assertThat(actor.get().tecnicoId()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void incluyeTecnicoIdCuandoLaCredencialEsDeTecnico() {
        JwtService service = new JwtService(SECRET, 60);
        Credencial tecnico = new Credencial(2L, "tecnico@helpflow.com", "hash",
                Rol.ROLE_TECNICO, true, null, 77L);

        Optional<Actor> actor = service.validar(service.generar(tecnico).token());

        assertThat(actor).isPresent();
        assertThat(actor.get().tecnicoId()).isEqualTo(77L);
        assertThat(actor.get().usuarioId()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaTokenExpirado() {
        JwtService service = new JwtService(SECRET, -1);

        TokenEmitido token = service.generar(credencial());

        assertThat(service.validar(token.token())).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaTokenConFirmaDeOtroSecreto() {
        JwtService emisor = new JwtService(SECRET, 60);
        JwtService validador = new JwtService("otro-secreto-de-prueba-de-32-bytes!!!", 60);

        assertThat(validador.validar(emisor.generar(credencial()).token())).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaTokenMalformado() {
        JwtService service = new JwtService(SECRET, 60);

        assertThat(service.validar("no-es-un-jwt")).isEmpty();
        assertThat(service.validar("")).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaSecretoDemasiadoCorto() {
        assertThatThrownBy(() -> new JwtService("corto", 60))
                .isInstanceOf(WeakKeyException.class);
    }
}
