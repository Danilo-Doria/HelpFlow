package com.support.ticketservice.infrastructure.adapter.out.persistance.adapter;

import com.support.ticketservice.application.port.out.TicketRepositoryPort;
import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.infrastructure.adapter.out.persistance.entity.TicketEntity;
import com.support.ticketservice.infrastructure.adapter.out.persistance.mapper.TicketPersistenceMapper;
import com.support.ticketservice.infrastructure.adapter.out.persistance.repository.TecnicoJpaRepository;
import com.support.ticketservice.infrastructure.adapter.out.persistance.repository.TicketJpaRepository;
import com.support.ticketservice.infrastructure.adapter.out.persistance.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}
