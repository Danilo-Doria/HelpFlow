package com.support.ticketservice.application.port.out;

import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.domain.service.SlaPolicy;

import java.time.Instant;

public interface SlaConfigPort {
    SlaPolicy obtener();

    void guardar(Prioridad prioridad, int horas, String actualizadoPor, Instant fecha);
}
