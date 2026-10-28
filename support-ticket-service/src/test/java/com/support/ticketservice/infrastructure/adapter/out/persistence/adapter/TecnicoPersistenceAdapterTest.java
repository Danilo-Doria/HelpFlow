package com.support.ticketservice.infrastructure.adapter.out.persistence.adapter;

import com.support.ticketservice.domain.model.Tecnico;
import com.support.ticketservice.domain.model.TecnicoConCarga;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.TecnicoEntity;
import com.support.ticketservice.infrastructure.adapter.out.persistence.mapper.TecnicoPersistenceMapper;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.TecnicoCargaRow;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.TecnicoJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TecnicoPersistenceAdapterTest {

    @Mock private TecnicoJpaRepository jpa;
    private TecnicoPersistenceAdapter adapter;

    @BeforeEach
    void preparar() {
        adapter = new TecnicoPersistenceAdapter(jpa, Mappers.getMapper(TecnicoPersistenceMapper.class));
    }

    private Tecnico dominio(Long id) {
        return new Tecnico(id, "T-1", "Laura", new BigDecimal("3500000"),
                LocalDate.of(2020, 1, 1), EstadoRegistro.ACTIVO,
                Categoria.HARDWARE, "BOGOTA", 5);
    }

    private TecnicoEntity entidad() {
        TecnicoEntity e = new TecnicoEntity();
        e.setId(1L);
        e.setDocumento("T-1");
        e.setNombre("Laura");
        e.setSalario(new BigDecimal("3500000"));
        e.setFechaAfiliacion(LocalDate.of(2020, 1, 1));
        e.setEstado(EstadoRegistro.ACTIVO);
        e.setEspecialidad(Categoria.HARDWARE);
        e.setSede("BOGOTA");
        e.setCargaMaxima(5);
        return e;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void buscaCandidatosConSuCarga() {
        when(jpa.buscarCandidatos(eq(Categoria.HARDWARE), eq(EstadoRegistro.ACTIVO), any()))
                .thenReturn(List.of(new TecnicoCargaRow(entidad(), 2L)));

        List<TecnicoConCarga> candidatos = adapter.buscarCandidatos(Categoria.HARDWARE);

        assertThat(candidatos).hasSize(1);
        assertThat(candidatos.get(0).tecnico().id()).isEqualTo(1L);
        assertThat(candidatos.get(0).ticketsActivos()).isEqualTo(2L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void guardaTecnicoNuevoYExistente() {
        when(jpa.save(any(TecnicoEntity.class))).thenAnswer(invocation -> {
            TecnicoEntity e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(1L);
            return e;
        });

        assertThat(adapter.guardar(dominio(null)).id()).isEqualTo(1L);
        assertThat(adapter.guardar(dominio(1L)).id()).isEqualTo(1L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void buscaListaYCompruebaDocumento() {
        when(jpa.findById(1L)).thenReturn(Optional.of(entidad()));
        when(jpa.findById(2L)).thenReturn(Optional.empty());
        when(jpa.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(entidad()));
        when(jpa.existsByDocumento("T-1")).thenReturn(true);

        assertThat(adapter.buscarPorId(1L)).isPresent();
        assertThat(adapter.buscarPorId(2L)).isEmpty();
        assertThat(adapter.listarTodos()).hasSize(1);
        assertThat(adapter.existePorDocumento("T-1")).isTrue();
    }
}
