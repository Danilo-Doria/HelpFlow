package com.support.ticketservice.infrastructure.adapter.out.persistence.adapter;

import com.support.ticketservice.application.port.out.TicketRepositoryPort;
import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.TicketEntity;
import com.support.ticketservice.infrastructure.adapter.out.persistence.mapper.TicketPersistenceMapper;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.TecnicoJpaRepository;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.TicketJpaRepository;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TicketPersistenceAdapter implements TicketRepositoryPort {

    private final TicketJpaRepository jpa;
    private final UsuarioJpaRepository usuarioJpa;
    private final TecnicoJpaRepository tecnicoJpa;
    private final TicketPersistenceMapper mapper;

    @Override
    public Ticket guardar(Ticket ticket) {
        TicketEntity entity = ticket.getId() == null
                ? new TicketEntity()
                : jpa.findById(ticket.getId()).orElseThrow(() ->
                new RecursoNoEncontradoException("Ticket no encontrado: " + ticket.getId()));

        mapper.actualizar(ticket, entity);
        entity.setUsuario(usuarioJpa.getReferenceById(ticket.getSolicitante().id()));
        entity.setTecnico(ticket.getTecnicoAsignado() == null
                ? null
                : tecnicoJpa.getReferenceById(ticket.getTecnicoAsignado().id()));

        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Ticket> buscarPorId(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Ticket> listarTodos() {
        return jpa.findAllByOrderByFechaCreacionDesc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> listarPorSolicitante(Long usuarioId) {
        return jpa.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> listarPorTecnico(Long tecnicoId) {
        return jpa.findByTecnicoIdOrderByFechaCreacionDesc(tecnicoId).stream().map(mapper::toDomain).toList();
    }

    private static final List<EstadoTicket> ESTADOS_ACTIVOS =
            Arrays.stream(EstadoTicket.values()).filter(EstadoTicket::esActivo).toList();

    @Override
    public long contarActivosDeTecnico(Long tecnicoId) {
        return jpa.countByTecnicoIdAndEstadoIn(tecnicoId, ESTADOS_ACTIVOS);
    }

    private static final List<EstadoTicket> ESTADOS_SLA =
            Arrays.stream(EstadoTicket.values()).filter(EstadoTicket::cuentaParaSla).toList();

    @Override
    public List<Long> buscarIdsEscalables(Instant limite) {
        return jpa.buscarIdsEscalables(ESTADOS_SLA, limite);
    }
}
