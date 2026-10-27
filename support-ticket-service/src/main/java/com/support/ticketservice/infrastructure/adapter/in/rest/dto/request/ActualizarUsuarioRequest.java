package com.support.ticketservice.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Size(max = 60) String sede,
        @NotBlank @Size(max = 80) String area) {
}
