package com.support.ticketservice.infrastructure.adapter.out.persistance.repository;

import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.infrastructure.adapter.out.persistance.entity.TecnicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TecnicoJpaRepository extends JpaRepository<TecnicoEntity, Long> {

    @Query("""
            select new com.support.ticketservice.infrastructure.adapter.out.persistence.repository.TecnicoCargaRow(
                t,
                (select count(k) from TicketEntity k
                  where k.tecnico = t and k.estado in :estadosActivos))
            from TecnicoEntity t
            where t.estado = :estado and t.especialidad = :categoria
            """)
    List<TecnicoCargaRow> buscarCandidatos(@Param("categoria") Categoria categoria,
                                           @Param("estado") EstadoRegistro estado,
                                           @Param("estadosActivos") Collection<EstadoTicket> estadosActivos);
}
