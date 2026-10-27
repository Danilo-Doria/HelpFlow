package com.support.ticketservice.infrastructure.adapter.out.persistence.adapter;

import com.support.ticketservice.application.port.out.UsuarioRepositoryPort;
import com.support.ticketservice.domain.model.Usuario;
import com.support.ticketservice.infrastructure.adapter.out.persistence.mapper.UsuarioPersistenceMapper;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository jpa;
    private final UsuarioPersistenceMapper mapper;

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }
}
