package com.support.ticketservice.infrastructure.adapter.out.persistence.adapter;

import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.TecnicoEntity;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.TicketEntity;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import com.support.ticketservice.infrastructure.adapter.out.persistence.mapper.TicketPersistenceMapper;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.TecnicoJpaRepository;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.TicketJpaRepository;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.UsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.support.ticketservice.support.DomainFixtures.AHORA;
import static com.support.ticketservice.support.DomainFixtures.tecnicoActivo;
import static com.support.ticketservice.support.DomainFixtures.ticketAbierto;
import static com.support.ticketservice.support.DomainFixtures.ticketAsignado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketPersistenceAdapterTest {

    @Mock private TicketJpaRepository jpa;
    @Mock private UsuarioJpaRepository usuarioJpa;
    @Mock private TecnicoJpaRepository tecnicoJpa;
    private TicketPersistenceAdapter adapter;

    @BeforeEach
    void preparar() {
        TicketPersistenceMapper mapper = Mappers.getMapper(TicketPersistenceMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(mapper,
                "usuarioPersistenceMapper", Mappers.getMapper(
                        com.support.ticketservice.infrastructure.adapter.out.persistence.mapper.UsuarioPersistenceMapper.class));
        org.springframework.test.util.ReflectionTestUtils.setField(mapper,
                "tecnicoPersistenceMapper", Mappers.getMapper(
                        com.support.ticketservice.infrastructure.adapter.out.persistence.mapper.TecnicoPersistenceMapper.class));
        adapter = new TicketPersistenceAdapter(jpa, usuarioJpa, tecnicoJpa, mapper);
        org.mockito.Mockito.lenient().when(usuarioJpa.getReferenceById(1L))
                .thenReturn(usuarioEntity());
    }

    private UsuarioEntity usuarioEntity() {
        UsuarioEntity e = new UsuarioEntity();
        e.setId(1L);
        e.setDocumento("123456");
        e.setNombre("Ana");
        e.setCorreo("ana@helpflow.com");
        e.setSede("BOGOTA");
        e.setArea("TI");
        e.setEstado(EstadoRegistro.ACTIVO);
        return e;
    }

    private TecnicoEntity tecnicoEntity() {
        TecnicoEntity e = new TecnicoEntity();
        e.setId(10L);
        e.setDocumento("T-10");
        e.setNombre("Tecnico 10");
        e.setSalario(new BigDecimal("3500000"));
        e.setFechaAfiliacion(LocalDate.of(2020, 1, 1));
        e.setEstado(EstadoRegistro.ACTIVO);
        e.setEspecialidad(Categoria.HARDWARE);
        e.setSede("BOGOTA");
        e.setCargaMaxima(5);
        return e;
    }

    private TicketEntity entidad(Long id) {
        TicketEntity e = new TicketEntity();
        e.setId(id);
        e.setTitulo("No enciende");
        e.setDescripcion("El equipo no enciende");
        e.setCategoria(Categoria.HARDWARE);
        e.setPrioridad(Prioridad.ALTA);
        e.setSede("BOGOTA");
        e.setActivoAfectado("PC-01");
        e.setFechaCreacion(AHORA);
        e.setFechaLimiteSla(AHORA.plus(Duration.ofHours(8)));
        e.setEstado(EstadoTicket.ABIERTO);
        e.setUsuario(usuarioEntity());
        return e;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void guardaTicketNuevoYResuelveReferencias() {
        when(jpa.save(any(TicketEntity.class))).thenAnswer(invocation -> {
            TicketEntity e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });

        Ticket resultado = adapter.guardar(ticketAbierto());

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(resultado.getSolicitante().id()).isEqualTo(1L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void guardaTicketAsignadoYResuelveTecnico() {
        when(tecnicoJpa.getReferenceById(10L)).thenReturn(tecnicoEntity());
        when(jpa.save(any(TicketEntity.class))).thenAnswer(invocation -> {
            TicketEntity e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });

        Ticket nuevoAsignado = ticketAbierto();
        nuevoAsignado.asignar(tecnicoActivo(10L, Categoria.HARDWARE));
        Ticket resultado = adapter.guardar(nuevoAsignado);

        assertThat(resultado.getTecnicoAsignado().id()).isEqualTo(10L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTicket.ASIGNADO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void actualizaTicketExistenteSobreLaEntidadGestionada() {
        TicketEntity existente = entidad(1L);
        when(jpa.findById(1L)).thenReturn(Optional.of(existente));
        when(jpa.save(existente)).thenReturn(existente);

        Ticket resultado = adapter.guardar(ticketAbierto(1L));

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(existente.getEstado()).isEqualTo(EstadoTicket.ABIERTO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void fallaAlGuardarTicketExistenteQueYaNoEsta() {
        when(jpa.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.guardar(ticketAbierto(1L)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Ticket no encontrado");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void buscaYListaTickets() {
        when(jpa.findById(1L)).thenReturn(Optional.of(entidad(1L)));
        when(jpa.findById(2L)).thenReturn(Optional.empty());
        when(jpa.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of(entidad(1L)));
        when(jpa.findByUsuarioIdOrderByFechaCreacionDesc(1L)).thenReturn(List.of(entidad(1L)));
        when(jpa.findByTecnicoIdOrderByFechaCreacionDesc(10L)).thenReturn(List.of(entidad(1L)));

        assertThat(adapter.buscarPorId(1L)).isPresent();
        assertThat(adapter.buscarPorId(2L)).isEmpty();
        assertThat(adapter.listarTodos()).hasSize(1);
        assertThat(adapter.listarPorSolicitante(1L)).hasSize(1);
        assertThat(adapter.listarPorTecnico(10L)).hasSize(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void delegaConteosYConsultasDeEscalamiento() {
        Instant limite = AHORA.plus(Duration.ofHours(2));
        when(jpa.countByTecnicoIdAndEstadoIn(any(), any())).thenReturn(3L);
        when(jpa.buscarIdsEscalables(any(), any())).thenReturn(List.of(5L));
        when(jpa.countByEstadoInAndFechaLimiteSlaBefore(any(), any())).thenReturn(7L);

        assertThat(adapter.contarActivosDeTecnico(10L)).isEqualTo(3L);
        assertThat(adapter.buscarIdsEscalables(limite)).containsExactly(5L);
        assertThat(adapter.contarVencidos(AHORA)).isEqualTo(7L);
    }
}
