package com.support.ticketservice.infrastructure.adapter.out.persistence.repository;

import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.CredencialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredencialJpaRepository extends JpaRepository<CredencialEntity, Long> {
    Optional<CredencialEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<CredencialEntity> findByUsuarioId(Long usuarioId);

    Optional<CredencialEntity> findByTecnicoId(Long tecnicoId);
}
