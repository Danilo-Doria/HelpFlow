package com.support.ticketservice.application.port.out;

import com.support.ticketservice.domain.model.Asignacion;

public interface AsignacionRepositoryPort {
    Asignacion guardar(Asignacion asignacion);
}
