package com.support.ticketservice.infrastructure.adapter.out.persistance.repository;

import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.infrastructure.adapter.out.persistance.entity.SlaConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlaConfigJpaRepository extends JpaRepository<SlaConfigEntity, Prioridad> {
}
