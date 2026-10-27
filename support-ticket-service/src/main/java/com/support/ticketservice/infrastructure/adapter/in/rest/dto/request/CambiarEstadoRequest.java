package com.support.ticketservice.infrastructure.adapter.in.rest.dto.request;

import com.support.ticketservice.domain.model.enums.EstadoTicket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambiarEstadoRequest(
        @NotNull EstadoTicket estado,
        @NotBlank @Size(max = 500) String observacion) {
}
