package com.support.ticketservice.infrastructure.adapter.out.persistance.repository;

import com.support.ticketservice.infrastructure.adapter.out.persistance.entity.AsignacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignacionJpaRepository extends JpaRepository<AsignacionEntity, Long> {
}
