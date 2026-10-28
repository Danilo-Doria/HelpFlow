package com.support.ticketservice.infrastructure.adapter.in.rest;

import com.support.ticketservice.application.port.in.*;
import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.CambiarEstadoRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.CrearTicketRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.ReasignarTicketRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.HistorialResponse;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.TicketResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Operaciones del ciclo de vida de tickets de soporte TI")
@SecurityRequirement(name = "BearerAuth")
public class TicketController {

    private final CrearTicketUseCase crearTicket;
    private final ConsultarTicketsUseCase consultarTickets;
    private final CambiarEstadoUseCase cambiarEstado;
    private final TicketRestMapper mapper;
    private final ReasignarTicketUseCase reasignar;
    private final EscalarTicketsUseCase escalarTickets;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USUARIO')")
    @Operation(summary = "Crear nuevo ticket de soporte",
            description = "Crea un ticket en estado ABIERTO o ASIGNADO (si hay despacho automático disponible). Solo permitido a usuarios solicitantes.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ROLE_USUARIO)")
    })
    public TicketResponse crear(@Valid @RequestBody CrearTicketRequest r,
                                @Parameter(hidden = true) @AuthenticationPrincipal Actor actor) {
        var cmd = new CrearTicketUseCase.CrearTicketCommand(actor.usuarioId(), r.titulo(),
                r.descripcion(), r.categoria(), r.prioridad(), r.sede(), r.activoAfectado());
        return mapper.toResponse(crearTicket.ejecutar(cmd, actor.username()));
    }

    @GetMapping
    @Operation(summary = "Listar tickets visibles para el actor autenticado",
            description = "ADMIN lista todos los tickets; USUARIO lista solo sus solicitudes; TECNICO lista solo sus tickets asignados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de tickets obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public List<TicketResponse> listar(@Parameter(hidden = true) @AuthenticationPrincipal Actor actor) {
        return consultarTickets.listar(actor).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un ticket",
            description = "Retorna el ticket con su solicitante, técnico asignado y fecha límite de SLA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket encontrado"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado para ver este ticket"),
            @ApiResponse(responseCode = "404", description = "Ticket no encontrado")
    })
    public TicketResponse obtener(@Parameter(description = "Identificador del ticket", example = "1")
                                  @PathVariable Long id,
                                  @Parameter(hidden = true) @AuthenticationPrincipal Actor actor) {
        return mapper.toResponse(consultarTickets.obtener(id, actor));
    }

    @GetMapping("/{id}/historial")
    @Operation(summary = "Consultar historial de cambios de un ticket",
            description = "Devuelve la traza cronológica de transiciones de estado, técnico y observaciones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado para ver este ticket"),
            @ApiResponse(responseCode = "404", description = "Ticket no encontrado")
    })
    public List<HistorialResponse> historial(@Parameter(description = "Identificador del ticket", example = "1")
                                             @PathVariable Long id,
                                             @Parameter(hidden = true) @AuthenticationPrincipal Actor actor) {
        return consultarTickets.historial(id, actor).stream().map(mapper::toResponse).toList();
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado de un ticket",
            description = "Permite avanzar el flujo del ticket (EN_PROCESO, EN_ESPERA, RESUELTO, CERRADO) validando las reglas de negocio y permisos del actor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado cambiado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No autorizado para cambiar el estado de este ticket"),
            @ApiResponse(responseCode = "404", description = "Ticket no encontrado"),
            @ApiResponse(responseCode = "409", description = "Transición de estado inválida para el flujo permitido")
    })
    public TicketResponse cambiarEstado(@Parameter(description = "Identificador del ticket", example = "1")
                                        @PathVariable Long id,
                                        @Valid @RequestBody CambiarEstadoRequest r,
                                        @Parameter(hidden = true) @AuthenticationPrincipal Actor actor) {
        return mapper.toResponse(
                cambiarEstado.ejecutar(id, r.estado(), r.observacion(), actor));
    }

    @PutMapping("/{id}/asignacion")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reasignar técnico a un ticket",
            description = "Asigna un nuevo técnico activo con capacidad disponible al ticket. Desactiva la asignación previa. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket reasignado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de reasignación inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ROLE_ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Ticket o técnico no encontrado"),
            @ApiResponse(responseCode = "422", description = "Técnico inactivo o con carga máxima superada")
    })
    public TicketResponse reasignar(@Parameter(description = "Identificador del ticket", example = "1")
                                    @PathVariable Long id,
                                    @Valid @RequestBody ReasignarTicketRequest r,
                                    @Parameter(hidden = true) @AuthenticationPrincipal Actor actor) {
        return mapper.toResponse(reasignar.ejecutar(id, r.tecnicoId(), r.observacion(), actor));
    }

    @PostMapping("/escalamiento")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ejecutar escalamiento manual de tickets próximos a vencer",
            description = "Revisa los tickets activos próximos a vencer su SLA (umbral configurado) y los marca como escalados. Requiere ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Escalamiento ejecutado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ROLE_ADMIN)")
    })
    public Map<String, Integer> escalar() {
        return Map.of("escalados", escalarTickets.ejecutar());
    }
}
