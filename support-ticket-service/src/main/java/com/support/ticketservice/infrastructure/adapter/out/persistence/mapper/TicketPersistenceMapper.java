package com.support.ticketservice.infrastructure.adapter.out.persistence.mapper;

import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.TicketEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(uses = {UsuarioPersistenceMapper.class, TecnicoPersistenceMapper.class})
public interface TicketPersistenceMapper {

    @Mapping(source = "usuario", target = "solicitante")
    @Mapping(source = "tecnico", target = "tecnicoAsignado")
    Ticket toDomain(TicketEntity entity);

    // Copia el estado del dominio sobre una entidad (nueva o ya gestionada)
    // Las relaciones las resuelven en el adaptador con getReferenceById
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "tecnico", ignore = true)
    @Mapping(target = "historiales", ignore = true)
    void actualizar(Ticket ticket, @MappingTarget TicketEntity entity);
}
