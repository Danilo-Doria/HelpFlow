package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.out.CredencialRepositoryPort;
import com.support.ticketservice.application.port.out.UsuarioRepositoryPort;
import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.model.Usuario;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.support.ticketservice.support.DomainFixtures.usuarioActivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionarUsuariosServiceTest {

    @Mock private UsuarioRepositoryPort usuarios;
    @Mock private CredencialRepositoryPort credenciales;
    @InjectMocks private GestionarUsuariosService service;

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void listaYObtieneUsuarios() {
        Usuario usuario = usuarioActivo();
        when(usuarios.listarTodos()).thenReturn(List.of(usuario));
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(usuario));

        assertThat(service.listar()).containsExactly(usuario);
        assertThat(service.obtener(1L)).isSameAs(usuario);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void fallaAlObtenerUsuarioInexistente() {
        when(usuarios.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void actualizaNombreSedeYAreaConservandoCorreo() {
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(usuarios.guardar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario resultado = service.actualizar(1L, "Ana Actualizada", "MEDELLIN", "SOPORTE");

        assertThat(resultado.nombre()).isEqualTo("Ana Actualizada");
        assertThat(resultado.sede()).isEqualTo("MEDELLIN");
        assertThat(resultado.area()).isEqualTo("SOPORTE");
        assertThat(resultado.correo()).isEqualTo("ana@helpflow.com");
        assertThat(resultado.documento()).isEqualTo("123456");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void fallaAlActualizarUsuarioInexistente() {
        when(usuarios.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(99L, "X", "Y", "Z"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void cambiarEstadoSincronizaLaCredencial() {
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(usuarios.guardar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario resultado = service.cambiarEstado(1L, EstadoRegistro.INACTIVO);

        assertThat(resultado.estado()).isEqualTo(EstadoRegistro.INACTIVO);
        verify(credenciales).actualizarActivoPorUsuarioId(1L, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void cambiarAActivoSincronizaLaCredencial() {
        Usuario inactivo = usuarioActivo().conEstado(EstadoRegistro.INACTIVO);
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(inactivo));
        when(usuarios.guardar(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        service.cambiarEstado(1L, EstadoRegistro.ACTIVO);

        verify(credenciales).actualizarActivoPorUsuarioId(1L, true);
    }
}
