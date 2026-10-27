package com.support.ticketservice.domain.model;

import java.time.Instant;

public record Asignacion(
        Long id, Long ticketId, Long tecnicoId,
        String tecnicoSugeridoExterno,   // lo que responde technician-dispatch-service
        Disponibilidad disponibilidad,
        Integer tiempoEstimadoHoras,
        Instant fechaAsignacion, boolean activa, String asignadoPor) {
}
