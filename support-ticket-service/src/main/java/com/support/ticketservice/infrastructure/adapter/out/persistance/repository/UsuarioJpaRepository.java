package com.support.ticketservice.infrastructure.adapter.out.persistance.repository;

import com.support.ticketservice.infrastructure.adapter.out.persistance.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {
}
