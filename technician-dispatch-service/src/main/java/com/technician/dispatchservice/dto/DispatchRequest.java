package com.technician.dispatchservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Solicitud de cálculo de disponibilidad y despacho técnico")
public record DispatchRequest(
        @Schema(description = "Identificador del ticket", example = "101")
        @NotNull @Positive Long ticketId,

        @Schema(description = "Categoría de la falla reportada", example = "HARDWARE")
        @NotNull Categoria categoria,

        @Schema(description = "Nivel de prioridad del ticket", example = "ALTA")
        @NotNull Prioridad prioridad) {
}
