package com.support.ticketservice.infrastructure.adapter.in.rest;

import com.support.ticketservice.domain.model.Usuario;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.UsuarioResponse;
import org.mapstruct.Mapper;

@Mapper
public interface UsuarioRestMapper {
    UsuarioResponse toResponse(Usuario usuario);
}
