package com.support.ticketservice.infrastructure.adapter.out.persistence.repository;

import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.TicketEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
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

    long countByTecnicoIdAndEstadoIn(Long tecnicoId, Collection<EstadoTicket> estados);

    @Query("""
        select t.id from TicketEntity t
        where t.escalado = false
          and t.estado in :estados
          and t.fechaLimiteSla <= :limite
        order by t.fechaLimiteSla
        """)
    List<Long> buscarIdsEscalables(@Param("estados") Collection<EstadoTicket> estados,
                                   @Param("limite") Instant limite);

    long countByEstadoInAndFechaLimiteSlaBefore(Collection<EstadoTicket> estados, Instant ahora);
}
