package com.support.ticketservice.infrastructure.adapter.out.persistence.repository;

import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.AsignacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AsignacionJpaRepository extends JpaRepository<AsignacionEntity, Long> {
    @Modifying
    @Query("update AsignacionEntity a set a.activa = false where a.ticketId = :ticketId and a.activa = true")
    void desactivarActivas(@Param("ticketId") Long ticketId);
}
