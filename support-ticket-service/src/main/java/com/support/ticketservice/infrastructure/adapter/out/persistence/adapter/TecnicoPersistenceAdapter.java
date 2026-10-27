package com.support.ticketservice.infrastructure.adapter.out.persistence.adapter;

import com.support.ticketservice.application.port.out.TecnicoRepositoryPort;
import com.support.ticketservice.domain.model.TecnicoConCarga;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.infrastructure.adapter.out.persistence.mapper.TecnicoPersistenceMapper;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.TecnicoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TecnicoPersistenceAdapter implements TecnicoRepositoryPort {

    private static final List<EstadoTicket> ESTADOS_ACTIVOS =
            Arrays.stream(EstadoTicket.values()).filter(EstadoTicket::esActivo).toList();

    private final TecnicoJpaRepository jpa;
    private final TecnicoPersistenceMapper mapper;

    @Override
    public List<TecnicoConCarga> buscarCandidatos(Categoria categoria) {
        return jpa.buscarCandidatos(categoria, EstadoRegistro.ACTIVO, ESTADOS_ACTIVOS).stream()
                .map(r -> new TecnicoConCarga(mapper.toDomain(r.tecnico()), r.carga()))
                .toList();
    }
}
