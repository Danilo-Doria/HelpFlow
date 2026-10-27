package com.support.ticketservice.infrastructure.adapter.in.rest;

import com.support.ticketservice.domain.model.HistorialTicket;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.HistorialResponse;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.TicketResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TicketRestMapper {

    @Mapping(source = "solicitante.id", target = "usuarioId")
    @Mapping(source = "tecnicoAsignado.id", target = "tecnicoId")
    @Mapping(source = "tecnicoAsignado.nombre", target = "tecnicoNombre")
    TicketResponse toResponse(Ticket ticket);

    HistorialResponse toResponse(HistorialTicket historial);
}
