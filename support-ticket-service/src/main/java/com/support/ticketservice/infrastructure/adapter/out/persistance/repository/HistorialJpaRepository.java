package com.support.ticketservice.infrastructure.adapter.out.persistance.repository;

import com.support.ticketservice.infrastructure.adapter.out.persistance.entity.HistorialTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialJpaRepository extends JpaRepository<HistorialTicketEntity, Long> {
}
