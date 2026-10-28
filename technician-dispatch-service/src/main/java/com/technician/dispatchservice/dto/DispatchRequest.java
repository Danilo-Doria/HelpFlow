package com.technician.dispatchservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DispatchRequest(
        @NotNull @Positive Long ticketId,
        @NotNull Categoria categoria,
        @NotNull Prioridad prioridad) {
}
