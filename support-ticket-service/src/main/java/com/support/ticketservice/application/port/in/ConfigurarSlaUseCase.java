package com.support.ticketservice.application.port.in;

import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.domain.service.SlaPolicy;

public interface ConfigurarSlaUseCase {
    SlaPolicy obtener();

    SlaPolicy actualizar(Prioridad prioridad, int horas, Actor actor);
}
