package com.support.ticketservice.infrastructure.adapter.out.persistence.adapter;

import com.support.ticketservice.application.port.out.CredencialRepositoryPort;
import com.support.ticketservice.domain.model.Credencial;
import com.support.ticketservice.infrastructure.adapter.out.persistence.mapper.CredencialPersistenceMapper;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.CredencialJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CredencialPersistenceAdapter implements CredencialRepositoryPort {

    private final CredencialJpaRepository jpa;
    private final CredencialPersistenceMapper mapper;

    @Override
    public Credencial guardar(Credencial credencial) {
        return mapper.toDomain(jpa.save(mapper.toEntity(credencial)));
    }

    @Override
    public Optional<Credencial> buscarPorUsername(String username) {
        return jpa.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public boolean existePorUsername(String username) {
        return jpa.existsByUsername(username);
    }

    @Override
    public void actualizarActivoPorUsuarioId(Long usuarioId, boolean activo) {
        jpa.findByUsuarioId(usuarioId).ifPresent(c -> c.setActivo(activo));
    }

    @Override
    public void actualizarActivoPorTecnicoId(Long tecnicoId, boolean activo) {
        jpa.findByTecnicoId(tecnicoId).ifPresent(c -> c.setActivo(activo));
    }
}
