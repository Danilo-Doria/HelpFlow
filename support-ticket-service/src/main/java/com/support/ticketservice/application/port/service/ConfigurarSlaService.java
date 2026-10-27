package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.in.ConfigurarSlaUseCase;
import com.support.ticketservice.application.port.out.SlaConfigPort;
import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.domain.service.SlaPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ConfigurarSlaService implements ConfigurarSlaUseCase {

    private final SlaConfigPort slaConfig;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public SlaPolicy obtener() {
        return slaConfig.obtener();
    }

    @Override
    @Transactional
    public SlaPolicy actualizar(Prioridad prioridad, int horas, Actor actor) {
        SlaPolicy nueva = slaConfig.obtener().conPlazo(prioridad, Duration.ofHours(horas));
        slaConfig.guardar(prioridad, horas, actor.username(), clock.instant());
        return nueva;
    }
}
