package com.support.ticketservice.application.port.out;

import com.support.ticketservice.domain.model.TecnicoConCarga;
import com.support.ticketservice.domain.model.enums.Categoria;

import java.util.List;

public interface TecnicoRepositoryPort {
    // Solo técnicos activos de esa especialidad, con su conteo de tickets ASIGNADO/EN_PROGRESO
    List<TecnicoConCarga> buscarCandidatos(Categoria categoria);
}
