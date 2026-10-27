package com.support.ticketservice.application.port.out;

import com.support.ticketservice.domain.model.HistorialTicket;

import java.util.List;

public interface HistorialRepositoryPort {
    HistorialTicket guardar(HistorialTicket historial);

    List<HistorialTicket> listarPorTicket(Long ticketId);
}
