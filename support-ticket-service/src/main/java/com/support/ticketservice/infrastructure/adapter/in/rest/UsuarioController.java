package com.support.ticketservice.infrastructure.adapter.in.rest;

import com.support.ticketservice.application.port.in.GestionarUsuariosUseCase;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.ActualizarUsuarioRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.CambiarEstadoRegistroRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final GestionarUsuariosUseCase usuarios;
    private final UsuarioRestMapper mapper;

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarios.listar().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return mapper.toResponse(usuarios.obtener(id));
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id,
                                      @Valid @RequestBody ActualizarUsuarioRequest r) {
        return mapper.toResponse(usuarios.actualizar(id, r.nombre(), r.sede(), r.area()));
    }

    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Long id,
                                         @Valid @RequestBody CambiarEstadoRegistroRequest r) {
        return mapper.toResponse(usuarios.cambiarEstado(id, r.estado()));
    }
}
