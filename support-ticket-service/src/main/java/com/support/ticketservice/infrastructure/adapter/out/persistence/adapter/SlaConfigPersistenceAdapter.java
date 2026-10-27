package com.support.ticketservice.infrastructure.adapter.out.persistence.adapter;

import com.support.ticketservice.application.port.out.SlaConfigPort;
import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.domain.service.SlaPolicy;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.SlaConfigEntity;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.SlaConfigJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SlaConfigPersistenceAdapter implements SlaConfigPort {

    private final SlaConfigJpaRepository jpa;

    @Override
    public SlaPolicy obtener() {
        Map<Prioridad, Duration> plazos = new EnumMap<>(Prioridad.class);
        jpa.findAll().forEach(c -> plazos.put(c.getPrioridad(), Duration.ofHours(c.getHoras())));
        return new SlaPolicy(plazos);
    }

    @Override
    public void guardar(Prioridad prioridad, int horas, String actualizadoPor, Instant fecha) {
        SlaConfigEntity e = jpa.findById(prioridad).orElseGet(() -> {
            SlaConfigEntity nuevo = new SlaConfigEntity();
            nuevo.setPrioridad(prioridad);
            return nuevo;
        });
        e.setHoras(horas);
        e.setActualizadoPor(actualizadoPor);
        e.setFechaActualizacion(fecha);
        jpa.save(e);
    }
}