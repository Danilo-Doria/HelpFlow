package com.support.ticketservice.infrastructure.adapter.out.persistence.adapter;

import com.support.ticketservice.application.port.out.HistorialRepositoryPort;
import com.support.ticketservice.domain.model.HistorialTicket;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.HistorialTicketEntity;
import com.support.ticketservice.infrastructure.adapter.out.persistence.mapper.HistorialPersistenceMapper;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.HistorialJpaRepository;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.TicketJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HistorialPersistenceAdapter implements HistorialRepositoryPort {

    private final HistorialJpaRepository jpa;
    private final TicketJpaRepository ticketJpa;
    private final HistorialPersistenceMapper mapper;

    @Override
    public HistorialTicket guardar(HistorialTicket historial) {
        HistorialTicketEntity entity = mapper.toEntity(historial);
        entity.setTicket(ticketJpa.getReferenceById(historial.ticketId()));
        return mapper.toDomain(jpa.save(entity));
    }
}
