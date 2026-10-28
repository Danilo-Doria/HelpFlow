package com.technician.dispatchservice.controller;

import com.technician.dispatchservice.dto.DispatchRequest;
import com.technician.dispatchservice.dto.DispatchResponse;
import com.technician.dispatchservice.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/technician-dispatch")
@RequiredArgsConstructor
@Tag(name = "Despacho de Técnicos", description = "Simulador determinista de disponibilidad y asignación técnica")
public class DispatchController {

    private final DispatchService service;

    @PostMapping
    @Operation(summary = "Calcular despacho para un ticket",
            description = "Calcula de forma determinista el técnico sugerido, nivel de disponibilidad y tiempo estimado en horas a partir del ticketId, categoría y prioridad.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Despacho calculado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o campos requeridos ausentes")
    })
    public DispatchResponse despachar(@Valid @RequestBody DispatchRequest request) {
        return service.calcular(request);
    }
}
