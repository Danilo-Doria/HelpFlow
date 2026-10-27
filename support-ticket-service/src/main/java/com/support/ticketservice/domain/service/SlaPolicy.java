package com.support.ticketservice.domain.service;

import com.support.ticketservice.domain.model.Prioridad;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

public class SlaPolicy {

    private final Map<Prioridad, Duration> plazos;

    public SlaPolicy(Map<Prioridad, Duration> plazos) {
        for (Prioridad p : Prioridad.values()) {
            if (!plazos.containsKey(p)) {
                throw new IllegalArgumentException("Falta plazo de SLA para " + p);
            }
        }
        this.plazos = new EnumMap<>(plazos);
    }

    public Instant calcularFechaLimite(Prioridad prioridad, Instant desde) {
        return desde.plus(plazos.get(prioridad));
    }

    public Duration plazoDe(Prioridad prioridad) {
        return plazos.get(prioridad);
    }
}
