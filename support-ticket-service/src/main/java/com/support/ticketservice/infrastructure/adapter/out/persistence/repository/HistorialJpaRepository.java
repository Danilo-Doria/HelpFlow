package com.support.ticketservice.infrastructure.adapter.out.persistence.repository;

import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.HistorialTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialJpaRepository extends JpaRepository<HistorialTicketEntity, Long> {
    List<HistorialTicketEntity> findByTicketIdOrderByFechaAsc(Long ticketId);
}
