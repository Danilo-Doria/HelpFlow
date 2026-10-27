package com.support.ticketservice.infrastructure.adapter.in.rest;

import com.support.ticketservice.application.port.in.ConfigurarSlaUseCase;
import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.domain.service.SlaPolicy;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.ActualizarSlaRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.SlaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/sla")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SlaController {

    private final ConfigurarSlaUseCase sla;

    @GetMapping
    public List<SlaResponse> obtener() {
        return responder(sla.obtener());
    }

    @PutMapping("/{prioridad}")
    public List<SlaResponse> actualizar(@PathVariable Prioridad prioridad,
                                        @Valid @RequestBody ActualizarSlaRequest r,
                                        @AuthenticationPrincipal Actor actor) {
        return responder(sla.actualizar(prioridad, r.horas(), actor));
    }

    private List<SlaResponse> responder(SlaPolicy policy) {
        return Arrays.stream(Prioridad.values())
                .sorted(Comparator.reverseOrder())
                .map(p -> new SlaResponse(p, policy.plazoDe(p).toHours()))
                .toList();
    }
}