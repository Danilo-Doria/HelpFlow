package com.support.ticketservice.infrastructure.adapter.out.persistence.adapter;

import com.support.ticketservice.application.port.out.AsignacionRepositoryPort;
import com.support.ticketservice.domain.model.Asignacion;
import com.support.ticketservice.infrastructure.adapter.out.persistence.mapper.AsignacionPersistenceMapper;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.AsignacionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AsignacionPersistenceAdapter implements AsignacionRepositoryPort {

    private final AsignacionJpaRepository jpa;
    private final AsignacionPersistenceMapper mapper;

    @Override
    public Asignacion guardar(Asignacion asignacion) {
        return mapper.toDomain(jpa.save(mapper.toEntity(asignacion)));
    }
}
