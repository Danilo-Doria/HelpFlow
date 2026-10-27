package com.support.ticketservice.application.port.out;

import com.support.ticketservice.domain.model.SugerenciaDespacho;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.Prioridad;

import java.util.Optional;

public interface TechnicianDispatchPort {
    // Vacío en caso de que el servicio externo falle o no responda
    Optional<SugerenciaDespacho> consultar(Long ticketId, Categoria categoria, Prioridad prioridad);
}
