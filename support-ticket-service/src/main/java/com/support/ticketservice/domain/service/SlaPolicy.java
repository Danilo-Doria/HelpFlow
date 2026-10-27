package com.support.ticketservice.domain.service;

import com.support.ticketservice.domain.exception.SlaInvalidoException;
import com.support.ticketservice.domain.model.enums.Prioridad;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

public class SlaPolicy {

    private final Map<Prioridad, Duration> plazos;

    public SlaPolicy(Map<Prioridad, Duration> plazos) {
        Prioridad[] orden = Prioridad.values();   // BAJA, MEDIA, ALTA, CRITICA
        for (Prioridad p : orden) {
            if (!plazos.containsKey(p)) {
                throw new IllegalArgumentException("Falta plazo de SLA para " + p);
            }
        }
        for (int i = 1; i < orden.length; i++) {
            if (plazos.get(orden[i]).compareTo(plazos.get(orden[i - 1])) > 0) {
                throw new SlaInvalidoException("El plazo de " + orden[i]
                        + " no puede ser mayor que el de " + orden[i - 1]);
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

    /** Devuelve una política nueva con ese plazo cambiado (valida coherencia). */
    public SlaPolicy conPlazo(Prioridad prioridad, Duration plazo) {
        if (plazo.isZero() || plazo.isNegative()) {
            throw new SlaInvalidoException("El plazo debe ser mayor que cero");
        }
        Map<Prioridad, Duration> copia = new EnumMap<>(plazos);
        copia.put(prioridad, plazo);
        return new SlaPolicy(copia);
    }
}
