package com.support.ticketservice.infrastructure.adapter.out.persistence.adapter;

import com.support.ticketservice.domain.model.Credencial;
import com.support.ticketservice.domain.model.enums.Rol;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.CredencialEntity;
import com.support.ticketservice.infrastructure.adapter.out.persistence.mapper.CredencialPersistenceMapper;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.CredencialJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredencialPersistenceAdapterTest {

    @Mock private CredencialJpaRepository jpa;
    private CredencialPersistenceAdapter adapter;

    @BeforeEach
    void preparar() {
        adapter = new CredencialPersistenceAdapter(jpa,
                Mappers.getMapper(CredencialPersistenceMapper.class));
    }

    private Credencial credencial() {
        return new Credencial(1L, "ana@helpflow.com", "hash", Rol.ROLE_USUARIO,
                true, 1L, null);
    }

    private CredencialEntity entidad() {
        CredencialEntity e = new CredencialEntity();
        e.setId(1L);
        e.setUsername("ana@helpflow.com");
        e.setPasswordHash("hash");
        e.setRol(Rol.ROLE_USUARIO);
        e.setActivo(true);
        e.setUsuarioId(1L);
        return e;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void guardaYBuscaCredencial() {
        when(jpa.save(any(CredencialEntity.class))).thenReturn(entidad());
        when(jpa.findByUsername("ana@helpflow.com")).thenReturn(Optional.of(entidad()));
        when(jpa.findByUsername("otro")).thenReturn(Optional.empty());

        assertThat(adapter.guardar(credencial()).username()).isEqualTo("ana@helpflow.com");
        assertThat(adapter.buscarPorUsername("ana@helpflow.com")).isPresent();
        assertThat(adapter.buscarPorUsername("otro")).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void compruebaExistencia() {
        when(jpa.existsByUsername("ana@helpflow.com")).thenReturn(true);

        assertThat(adapter.existePorUsername("ana@helpflow.com")).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void actualizaActivoPorUsuarioCuandoExiste() {
        CredencialEntity existente = entidad();
        when(jpa.findByUsuarioId(1L)).thenReturn(Optional.of(existente));

        adapter.actualizarActivoPorUsuarioId(1L, false);

        assertThat(existente.isActivo()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void actualizaActivoPorTecnicoCuandoExiste() {
        CredencialEntity existente = entidad();
        existente.setUsuarioId(null);
        existente.setTecnicoId(10L);
        existente.setRol(Rol.ROLE_TECNICO);
        when(jpa.findByTecnicoId(10L)).thenReturn(Optional.of(existente));

        adapter.actualizarActivoPorTecnicoId(10L, false);

        assertThat(existente.isActivo()).isFalse();
        verify(jpa).findByTecnicoId(10L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void noFallaSiLaCredencialNoExiste() {
        when(jpa.findByUsuarioId(99L)).thenReturn(Optional.empty());
        when(jpa.findByTecnicoId(99L)).thenReturn(Optional.empty());

        adapter.actualizarActivoPorUsuarioId(99L, false);
        adapter.actualizarActivoPorTecnicoId(99L, false);

        verify(jpa).findByUsuarioId(99L);
        verify(jpa).findByTecnicoId(99L);
    }
}
