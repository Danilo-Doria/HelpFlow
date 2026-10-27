package com.support.ticketservice.infrastructure.adapter.out.persistence.repository;

import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.TicketEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketJpaRepository extends JpaRepository<TicketEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"usuario", "tecnico"}) // EntityGraph evita N+1: trae usuario y técnico en la misma consulta
    Optional<TicketEntity> findById(Long id);

    @EntityGraph(attributePaths = {"usuario", "tecnico"})
    List<TicketEntity> findAllByOrderByFechaCreacionDesc();

    @EntityGraph(attributePaths = {"usuario", "tecnico"})
    List<TicketEntity> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario", "tecnico"})
    List<TicketEntity> findByTecnicoIdOrderByFechaCreacionDesc(Long tecnicoId);
}
