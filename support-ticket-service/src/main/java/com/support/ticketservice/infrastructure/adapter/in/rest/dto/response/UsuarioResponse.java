package com.support.ticketservice.infrastructure.adapter.in.rest.dto.response;

import com.support.ticketservice.domain.model.enums.EstadoRegistro;

public record UsuarioResponse(Long id, String documento, String nombre, String correo,
                              String sede, String area, EstadoRegistro estado) {
}
