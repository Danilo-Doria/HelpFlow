package com.support.ticketservice.application.port.out;

import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.Prioridad;

import java.time.Duration;

public interface TicketMetricsPort {
    void ticketCreado(Categoria categoria, Prioridad prioridad);

    void primeraRespuesta(Categoria categoria, Prioridad prioridad, Duration tiempo);

    void resolucion(Categoria categoria, Prioridad prioridad, Duration tiempo);

    void ticketEscalado(Prioridad prioridadResultante);
}
