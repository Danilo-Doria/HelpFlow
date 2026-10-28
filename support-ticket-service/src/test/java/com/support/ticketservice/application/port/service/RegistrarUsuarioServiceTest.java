package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.in.RegistrarUsuarioUseCase.RegistrarUsuarioCommand;
import com.support.ticketservice.application.port.out.CredencialRepositoryPort;
import com.support.ticketservice.application.port.out.PasswordHasherPort;
import com.support.ticketservice.application.port.out.UsuarioRepositoryPort;
import com.support.ticketservice.domain.exception.DuplicadoException;
import com.support.ticketservice.domain.model.Credencial;
import com.support.ticketservice.domain.model.Usuario;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import com.support.ticketservice.domain.model.enums.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class RegistrarUsuarioServiceTest {

    @Mock private UsuarioRepositoryPort usuarios;
    @Mock private CredencialRepositoryPort credenciales;
    @Mock private PasswordHasherPort hasher;
    @InjectMocks private RegistrarUsuarioService service;

    private RegistrarUsuarioCommand comando() {
        return new RegistrarUsuarioCommand("123456", "Ana Perez",
                "  ANA@HELPFLOW.COM  ", "BOGOTA", "TI", "Secreta123");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void registraUsuarioYCredencialConCorreoNormalizado() {
        Usuario guardado = new Usuario(5L, "123456", "Ana Perez", "ana@helpflow.com",
                "BOGOTA", "TI", EstadoRegistro.ACTIVO);
        when(usuarios.existePorDocumento("123456")).thenReturn(false);
        when(usuarios.existePorCorreo("ana@helpflow.com")).thenReturn(false);
        when(usuarios.guardar(any(Usuario.class))).thenReturn(guardado);
        when(hasher.encode("Secreta123")).thenReturn("hash");

        Usuario resultado = service.ejecutar(comando());

        assertThat(resultado.id()).isEqualTo(5L);
        assertThat(resultado.correo()).isEqualTo("ana@helpflow.com");

        ArgumentCaptor<Credencial> captor = ArgumentCaptor.forClass(Credencial.class);
        verify(credenciales).guardar(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("ana@helpflow.com");
        assertThat(captor.getValue().passwordHash()).isEqualTo("hash");
        assertThat(captor.getValue().rol()).isEqualTo(Rol.ROLE_USUARIO);
        assertThat(captor.getValue().usuarioId()).isEqualTo(5L);
        assertThat(captor.getValue().tecnicoId()).isNull();
        assertThat(captor.getValue().activo()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaDocumentoDuplicado() {
        when(usuarios.existePorDocumento("123456")).thenReturn(true);

        assertThatThrownBy(() -> service.ejecutar(comando()))
                .isInstanceOf(DuplicadoException.class)
                .hasMessageContaining("documento");

        verify(usuarios, never()).guardar(any());
        verify(credenciales, never()).guardar(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaCorreoDuplicadoSinImportarMayusculas() {
        when(usuarios.existePorDocumento("123456")).thenReturn(false);
        when(usuarios.existePorCorreo("ana@helpflow.com")).thenReturn(true);

        assertThatThrownBy(() -> service.ejecutar(comando()))
                .isInstanceOf(DuplicadoException.class)
                .hasMessageContaining("correo");

        verify(usuarios, never()).guardar(any());
        verify(credenciales, never()).guardar(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void noGeneraCredencialSiElGuardadoDeUsuarioFalla() {
        when(usuarios.existePorDocumento("123456")).thenReturn(false);
        when(usuarios.existePorCorreo("ana@helpflow.com")).thenReturn(false);
        when(usuarios.guardar(any(Usuario.class))).thenThrow(new IllegalStateException("db"));

        assertThatThrownBy(() -> service.ejecutar(comando()))
                .isInstanceOf(IllegalStateException.class);

        verify(credenciales, never()).guardar(any());
        verify(hasher, never()).encode(any());
    }
}
