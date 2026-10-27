package com.support.ticketservice.infrastructure.adapter.out.persistance.mapper;

import com.support.ticketservice.domain.model.Tecnico;
import com.support.ticketservice.infrastructure.adapter.out.persistance.entity.TecnicoEntity;
import org.mapstruct.Mapper;

@Mapper
public interface TecnicoPersistenceMapper {
    Tecnico toDomain(TecnicoEntity entity);
    TecnicoEntity toEntity(Tecnico tecnico);
}
