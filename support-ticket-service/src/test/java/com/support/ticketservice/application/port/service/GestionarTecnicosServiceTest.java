package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.in.GestionarTecnicosUseCase.ActualizarTecnicoCommand;
import com.support.ticketservice.application.port.in.GestionarTecnicosUseCase.CrearTecnicoCommand;
import com.support.ticketservice.application.port.out.CredencialRepositoryPort;
import com.support.ticketservice.application.port.out.PasswordHasherPort;
import com.support.ticketservice.application.port.out.TecnicoRepositoryPort;
import com.support.ticketservice.domain.exception.DuplicadoException;
import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.model.Credencial;
import com.support.ticketservice.domain.model.Tecnico;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import com.support.ticketservice.domain.model.enums.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.support.ticketservice.support.DomainFixtures.tecnicoActivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionarTecnicosServiceTest {

    @Mock private TecnicoRepositoryPort tecnicos;
    @Mock private CredencialRepositoryPort credenciales;
    @Mock private PasswordHasherPort hasher;
    @InjectMocks private GestionarTecnicosService service;

    private CrearTecnicoCommand crear() {
        return new CrearTecnicoCommand("T-100", "Laura Gomez", new BigDecimal("4000000"),
                LocalDate.of(2024, 1, 15), Categoria.SOFTWARE, "MEDELLIN", 6,
                "  LAURA@HELPFLOW.COM  ", "Secreta123");
    }

    private Tecnico guardado() {
        return new Tecnico(10L, "T-100", "Laura Gomez", new BigDecimal("4000000"),
                LocalDate.of(2024, 1, 15), EstadoRegistro.ACTIVO,
                Categoria.SOFTWARE, "MEDELLIN", 6);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void creaTecnicoYCredencialConUsernameNormalizado() {
        when(tecnicos.existePorDocumento("T-100")).thenReturn(false);
        when(credenciales.existePorUsername("laura@helpflow.com")).thenReturn(false);
        when(tecnicos.guardar(any(Tecnico.class))).thenReturn(guardado());
        when(hasher.encode("Secreta123")).thenReturn("hash");

        Tecnico resultado = service.crear(crear());

        assertThat(resultado.id()).isEqualTo(10L);
        ArgumentCaptor<Credencial> captor = ArgumentCaptor.forClass(Credencial.class);
        verify(credenciales).guardar(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("laura@helpflow.com");
        assertThat(captor.getValue().passwordHash()).isEqualTo("hash");
        assertThat(captor.getValue().rol()).isEqualTo(Rol.ROLE_TECNICO);
        assertThat(captor.getValue().tecnicoId()).isEqualTo(10L);
        assertThat(captor.getValue().usuarioId()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaDocumentoDuplicado() {
        when(tecnicos.existePorDocumento("T-100")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(crear()))
                .isInstanceOf(DuplicadoException.class)
                .hasMessageContaining("documento");

        verify(tecnicos, never()).guardar(any());
        verify(credenciales, never()).guardar(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaUsernameDuplicado() {
        when(tecnicos.existePorDocumento("T-100")).thenReturn(false);
        when(credenciales.existePorUsername("laura@helpflow.com")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(crear()))
                .isInstanceOf(DuplicadoException.class)
                .hasMessageContaining("usuario");

        verify(tecnicos, never()).guardar(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void listaYObtieneTecnicos() {
        Tecnico tecnico = guardado();
        when(tecnicos.listarTodos()).thenReturn(List.of(tecnico));
        when(tecnicos.buscarPorId(10L)).thenReturn(Optional.of(tecnico));

        assertThat(service.listar()).containsExactly(tecnico);
        assertThat(service.obtener(10L)).isSameAs(tecnico);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void fallaAlObtenerTecnicoInexistente() {
        when(tecnicos.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Técnico no encontrado");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void actualizaDatosDelTecnico() {
        Tecnico actual = guardado();
        when(tecnicos.buscarPorId(10L)).thenReturn(Optional.of(actual));
        when(tecnicos.guardar(any(Tecnico.class))).thenAnswer(i -> i.getArgument(0));

        Tecnico resultado = service.actualizar(10L, new ActualizarTecnicoCommand(
                "Laura Actualizada", new BigDecimal("5000000"),
                Categoria.RED, "BOGOTA", 8));

        assertThat(resultado.nombre()).isEqualTo("Laura Actualizada");
        assertThat(resultado.especialidad()).isEqualTo(Categoria.RED);
        assertThat(resultado.sede()).isEqualTo("BOGOTA");
        assertThat(resultado.cargaMaxima()).isEqualTo(8);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void fallaAlActualizarTecnicoInexistente() {
        when(tecnicos.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(99L, new ActualizarTecnicoCommand(
                "X", BigDecimal.ONE, Categoria.RED, "BOGOTA", 1)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void cambiarEstadoSincronizaLaCredencial() {
        Tecnico actual = tecnicoActivo(10L, Categoria.SOFTWARE);
        when(tecnicos.buscarPorId(10L)).thenReturn(Optional.of(actual));
        when(tecnicos.guardar(any(Tecnico.class))).thenAnswer(i -> i.getArgument(0));

        Tecnico resultado = service.cambiarEstado(10L, EstadoRegistro.INACTIVO);

        assertThat(resultado.estado()).isEqualTo(EstadoRegistro.INACTIVO);
        verify(credenciales).actualizarActivoPorTecnicoId(10L, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void cambiarAActivoSincronizaLaCredencial() {
        Tecnico actual = tecnicoActivo(10L, Categoria.SOFTWARE).conEstado(EstadoRegistro.INACTIVO);
        when(tecnicos.buscarPorId(10L)).thenReturn(Optional.of(actual));
        when(tecnicos.guardar(any(Tecnico.class))).thenAnswer(i -> i.getArgument(0));

        service.cambiarEstado(10L, EstadoRegistro.ACTIVO);

        verify(credenciales).actualizarActivoPorTecnicoId(10L, true);
    }
}
