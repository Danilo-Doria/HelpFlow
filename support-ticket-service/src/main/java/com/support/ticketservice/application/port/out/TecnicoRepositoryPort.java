package com.support.ticketservice.application.port.out;

import com.support.ticketservice.domain.model.Tecnico;
import com.support.ticketservice.domain.model.TecnicoConCarga;
import com.support.ticketservice.domain.model.enums.Categoria;

import java.util.List;
import java.util.Optional;

public interface TecnicoRepositoryPort {
    // Solo técnicos activos de esa especialidad, con su conteo de tickets ASIGNADO/EN_PROGRESO
    List<TecnicoConCarga> buscarCandidatos(Categoria categoria);

    Tecnico guardar(Tecnico tecnico);

    Optional<Tecnico> buscarPorId(Long id);

    List<Tecnico> listarTodos();

    boolean existePorDocumento(String documento);
}
