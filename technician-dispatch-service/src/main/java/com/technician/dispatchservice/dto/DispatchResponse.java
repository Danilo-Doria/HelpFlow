package com.technician.dispatchservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta calculada del despacho técnico")
public record DispatchResponse(
        @Schema(description = "Identificador del ticket", example = "101")
        Long ticketId,

        @Schema(description = "Código del técnico sugerido", example = "TEC-008")
        String tecnicoId,

        @Schema(description = "Nivel de disponibilidad calculado", example = "ALTA")
        Disponibilidad disponibilidad,

        @Schema(description = "Tiempo estimado de atención en horas", example = "2")
        int tiempoEstimadoHoras) {
}
