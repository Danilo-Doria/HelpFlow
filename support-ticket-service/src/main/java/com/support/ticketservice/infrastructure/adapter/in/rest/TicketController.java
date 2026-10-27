package com.support.ticketservice.infrastructure.adapter.in.rest;

import com.support.ticketservice.application.port.in.CambiarEstadoUseCase;
import com.support.ticketservice.application.port.in.ConsultarTicketsUseCase;
import com.support.ticketservice.application.port.in.CrearTicketUseCase;
import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.CambiarEstadoRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.CrearTicketRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.HistorialResponse;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.TicketResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final CrearTicketUseCase crearTicket;
    private final ConsultarTicketsUseCase consultarTickets;
    private final CambiarEstadoUseCase cambiarEstado;
    private final TicketRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USUARIO')")
    public TicketResponse crear(@Valid @RequestBody CrearTicketRequest r,
                                @AuthenticationPrincipal Actor actor) {
        var cmd = new CrearTicketUseCase.CrearTicketCommand(actor.usuarioId(), r.titulo(),
                r.descripcion(), r.categoria(), r.prioridad(), r.sede(), r.activoAfectado());
        return mapper.toResponse(crearTicket.ejecutar(cmd, actor.username()));
    }

    @GetMapping
    public List<TicketResponse> listar(@AuthenticationPrincipal Actor actor) {
        return consultarTickets.listar(actor).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TicketResponse obtener(@PathVariable Long id, @AuthenticationPrincipal Actor actor) {
        return mapper.toResponse(consultarTickets.obtener(id, actor));
    }

    @GetMapping("/{id}/historial")
    public List<HistorialResponse> historial(@PathVariable Long id,
                                             @AuthenticationPrincipal Actor actor) {
        return consultarTickets.historial(id, actor).stream().map(mapper::toResponse).toList();
    }

    @PatchMapping("/{id}/estado")
    public TicketResponse cambiarEstado(@PathVariable Long id,
                                        @Valid @RequestBody CambiarEstadoRequest r,
                                        @AuthenticationPrincipal Actor actor) {
        return mapper.toResponse(
                cambiarEstado.ejecutar(id, r.estado(), r.observacion(), actor));
    }
}
