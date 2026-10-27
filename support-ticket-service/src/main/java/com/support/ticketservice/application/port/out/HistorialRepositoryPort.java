package com.support.ticketservice.application.port.out;

import com.support.ticketservice.domain.model.HistorialTicket;

public interface HistorialRepositoryPort {
    HistorialTicket guardar(HistorialTicket historial);
}
