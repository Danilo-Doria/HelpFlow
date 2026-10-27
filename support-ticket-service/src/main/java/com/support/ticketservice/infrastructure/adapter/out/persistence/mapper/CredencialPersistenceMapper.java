package com.support.ticketservice.infrastructure.adapter.out.persistence.mapper;

import com.support.ticketservice.domain.model.Credencial;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.CredencialEntity;
import org.mapstruct.Mapper;

@Mapper
public interface CredencialPersistenceMapper {
    Credencial toDomain(CredencialEntity entity);

    CredencialEntity toEntity(Credencial credencial);
}
