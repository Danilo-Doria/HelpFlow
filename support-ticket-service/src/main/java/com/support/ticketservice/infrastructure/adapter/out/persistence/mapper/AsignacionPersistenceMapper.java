package com.support.ticketservice.infrastructure.adapter.out.persistence.mapper;

import com.support.ticketservice.domain.model.Asignacion;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.AsignacionEntity;
import org.mapstruct.Mapper;

@Mapper
public interface AsignacionPersistenceMapper {
    Asignacion toDomain(AsignacionEntity entity);

    AsignacionEntity toEntity(Asignacion asignacion);
}
