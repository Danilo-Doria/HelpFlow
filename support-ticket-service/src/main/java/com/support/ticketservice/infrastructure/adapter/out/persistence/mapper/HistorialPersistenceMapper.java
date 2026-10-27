package com.support.ticketservice.infrastructure.adapter.out.persistence.mapper;

import com.support.ticketservice.domain.model.HistorialTicket;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.HistorialTicketEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface HistorialPersistenceMapper {

    @Mapping(source = "ticket.id", target = "ticketId")
    HistorialTicket toDomain(HistorialTicketEntity entity);

    @Mapping(target = "ticket", ignore = true)
    HistorialTicketEntity toEntity(HistorialTicket historial);
}
