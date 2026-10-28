package com.support.ticketservice.infrastructure.adapter.out.persistence.adapter;

import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.model.Usuario;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import com.support.ticketservice.infrastructure.adapter.out.persistence.mapper.UsuarioPersistenceMapper;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.UsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioPersistenceAdapterTest {

    @Mock private UsuarioJpaRepository jpa;
    private UsuarioPersistenceAdapter adapter;

    @BeforeEach
    void preparar() {
        adapter = new UsuarioPersistenceAdapter(jpa, Mappers.getMapper(UsuarioPersistenceMapper.class));
    }

    private Usuario dominio(Long id) {
        return new Usuario(id, "123", "Ana", "ana@helpflow.com", "BOGOTA", "TI",
                EstadoRegistro.ACTIVO);
    }

    private UsuarioEntity entidad() {
        UsuarioEntity e = new UsuarioEntity();
        e.setId(1L);
        e.setDocumento("123");
        e.setNombre("Ana");
        e.setCorreo("ana@helpflow.com");
        e.setSede("BOGOTA");
        e.setArea("TI");
        e.setEstado(EstadoRegistro.ACTIVO);
        return e;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void buscaPorIdExistenteEInexistente() {
        when(jpa.findById(1L)).thenReturn(Optional.of(entidad()));
        when(jpa.findById(2L)).thenReturn(Optional.empty());

        assertThat(adapter.buscarPorId(1L)).get().extracting(Usuario::nombre).isEqualTo("Ana");
        assertThat(adapter.buscarPorId(2L)).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void guardaUsuarioNuevo() {
        when(jpa.save(any(UsuarioEntity.class))).thenAnswer(invocation -> {
            UsuarioEntity e = invocation.getArgument(0);
            e.setId(9L);
            return e;
        });

        Usuario resultado = adapter.guardar(dominio(null));

        assertThat(resultado.id()).isEqualTo(9L);
        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getDocumento()).isEqualTo("123");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void guardaUsuarioExistenteSobreLaEntidadGestionada() {
        UsuarioEntity existente = entidad();
        when(jpa.findById(1L)).thenReturn(Optional.of(existente));
        when(jpa.save(existente)).thenReturn(existente);

        Usuario resultado = adapter.guardar(dominio(1L));

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(existente.getNombre()).isEqualTo("Ana");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void fallaAlGuardarUsuarioExistenteQueYaNoEsta() {
        when(jpa.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.guardar(dominio(1L)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void delegaExistenciasYListado() {
        when(jpa.existsByDocumento("123")).thenReturn(true);
        when(jpa.existsByCorreoIgnoreCase("ana@helpflow.com")).thenReturn(true);
        when(jpa.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(entidad()));

        assertThat(adapter.existePorDocumento("123")).isTrue();
        assertThat(adapter.existePorCorreo("ana@helpflow.com")).isTrue();
        assertThat(adapter.listarTodos()).hasSize(1);
    }
}
