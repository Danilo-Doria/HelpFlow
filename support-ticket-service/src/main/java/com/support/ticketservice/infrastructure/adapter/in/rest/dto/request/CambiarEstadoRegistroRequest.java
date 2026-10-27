package com.support.ticketservice.infrastructure.adapter.in.rest.dto.request;

import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRegistroRequest(@NotNull EstadoRegistro estado) {
}
