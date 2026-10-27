package com.support.ticketservice.infrastructure.adapter.in.rest;

import com.support.ticketservice.domain.model.Tecnico;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.TecnicoResponse;
import org.mapstruct.Mapper;

@Mapper
public interface TecnicoRestMapper {
    TecnicoResponse toResponse(Tecnico tecnico);
}
