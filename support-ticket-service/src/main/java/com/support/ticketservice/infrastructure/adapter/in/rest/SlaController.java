package com.support.ticketservice.infrastructure.adapter.in.rest;

import com.support.ticketservice.application.port.in.ConfigurarSlaUseCase;
import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.domain.service.SlaPolicy;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.ActualizarSlaRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.SlaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "SLA", description = "Configuración y consulta de políticas de tiempos de atención SLA (Requiere rol ADMIN)")
@SecurityRequirement(name = "BearerAuth")
public class SlaController {

    private final ConfigurarSlaUseCase sla;

    @GetMapping
    @Operation(summary = "Obtener configuración vigente de SLAs",
            description = "Retorna los plazos en horas para cada prioridad (CRITICA, ALTA, MEDIA, BAJA).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuración de SLA obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ROLE_ADMIN)")
    })
    public List<SlaResponse> obtener() {
        return responder(sla.obtener());
    }

    @PutMapping("/{prioridad}")
    @Operation(summary = "Actualizar plazo SLA para una prioridad",
            description = "Modifica la cantidad de horas asignadas a una prioridad determinada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SLA actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Horas inválidas o formato incorrecto"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ROLE_ADMIN)")
    })
    public List<SlaResponse> actualizar(@Parameter(description = "Prioridad a actualizar", example = "CRITICA")
                                        @PathVariable Prioridad prioridad,
                                        @Valid @RequestBody ActualizarSlaRequest r,
                                        @Parameter(hidden = true) @AuthenticationPrincipal Actor actor) {
        return responder(sla.actualizar(prioridad, r.horas(), actor));
    }

    private List<SlaResponse> responder(SlaPolicy policy) {
        return Arrays.stream(Prioridad.values())
                .sorted(Comparator.reverseOrder())
                .map(p -> new SlaResponse(p, policy.plazoDe(p).toHours()))
                .toList();
    }
}
