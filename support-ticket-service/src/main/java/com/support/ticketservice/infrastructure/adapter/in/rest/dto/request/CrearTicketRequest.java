package com.support.ticketservice.infrastructure.adapter.in.rest.dto.request;

import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.Prioridad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearTicketRequest(
        @NotBlank @Size(max = 150) String titulo,
        @NotBlank @Size(max = 2000) String descripcion,
        @NotNull Categoria categoria,
        @NotNull Prioridad prioridad,
        @NotBlank @Size(max = 60) String sede,
        @Size(max = 120) String activoAfectado) {
}
