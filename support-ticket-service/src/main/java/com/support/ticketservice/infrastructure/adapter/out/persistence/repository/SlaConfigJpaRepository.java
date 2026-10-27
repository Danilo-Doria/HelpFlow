package com.support.ticketservice.infrastructure.adapter.out.persistence.repository;

import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.SlaConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlaConfigJpaRepository extends JpaRepository<SlaConfigEntity, Prioridad> {
}
