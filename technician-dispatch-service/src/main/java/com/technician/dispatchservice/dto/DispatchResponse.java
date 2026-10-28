package com.technician.dispatchservice.dto;

public record DispatchResponse(
        Long ticketId,
        String tecnicoId,
        Disponibilidad disponibilidad,
        int tiempoEstimadoHoras) {
}
