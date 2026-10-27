package com.support.ticketservice.infrastructure.adapter.out.persistance.mapper;

import com.support.ticketservice.domain.model.Usuario;
import com.support.ticketservice.infrastructure.adapter.out.persistance.entity.UsuarioEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UsuarioPersistenceMapper {
    Usuario toDomain(UsuarioEntity entity);

    @Mapping(target = "tickets", ignore = true)
    UsuarioEntity toEntity(Usuario usuario);
}
