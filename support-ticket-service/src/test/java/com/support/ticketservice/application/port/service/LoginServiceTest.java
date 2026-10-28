package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.out.CredencialRepositoryPort;
import com.support.ticketservice.application.port.out.PasswordHasherPort;
import com.support.ticketservice.application.port.out.TokenGeneratorPort;
import com.support.ticketservice.application.port.out.TokenGeneratorPort.TokenEmitido;
import com.support.ticketservice.domain.exception.CredencialesInvalidasException;
import com.support.ticketservice.domain.model.Credencial;
import com.support.ticketservice.domain.model.enums.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock private CredencialRepositoryPort credenciales;
    @Mock private PasswordHasherPort hasher;
    @Mock private TokenGeneratorPort tokens;
    @InjectMocks private LoginService service;

    private Credencial credencial(boolean activo) {
        return new Credencial(1L, "ana@helpflow.com", "hash",
                Rol.ROLE_USUARIO, activo, 1L, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void iniciaSesionNormalizandoElUsernameYGeneraToken() {
        Credencial credencial = credencial(true);
        TokenEmitido token = new TokenEmitido("jwt", 3600);
        when(credenciales.buscarPorUsername("ana@helpflow.com"))
                .thenReturn(Optional.of(credencial));
        when(hasher.matches("Secreta123", "hash")).thenReturn(true);
        when(tokens.generar(credencial)).thenReturn(token);

        TokenEmitido resultado = service.ejecutar("  ANA@HELPFLOW.COM  ", "Secreta123");

        assertThat(resultado).isSameAs(token);
        verify(tokens).generar(credencial);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaCredencialInexistente() {
        when(credenciales.buscarPorUsername("ana@helpflow.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ejecutar("ana@helpflow.com", "Secreta123"))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(tokens, never()).generar(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaCredencialInactivaSinVerificarPassword() {
        when(credenciales.buscarPorUsername("ana@helpflow.com"))
                .thenReturn(Optional.of(credencial(false)));

        assertThatThrownBy(() -> service.ejecutar("ana@helpflow.com", "Secreta123"))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(hasher, never()).matches(any(), any());
        verify(tokens, never()).generar(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaPasswordIncorrecta() {
        when(credenciales.buscarPorUsername("ana@helpflow.com"))
                .thenReturn(Optional.of(credencial(true)));
        when(hasher.matches("incorrecta", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.ejecutar("ana@helpflow.com", "incorrecta"))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(tokens, never()).generar(any());
    }
}
