package com.support.ticketservice.infrastructure.adapter.in.rest;

import com.support.ticketservice.application.port.in.GestionarTecnicosUseCase;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.ActualizarTecnicoRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.CambiarEstadoRegistroRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.CrearTecnicoRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.TecnicoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tecnicos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TecnicoController {

    private final GestionarTecnicosUseCase tecnicos;
    private final TecnicoRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TecnicoResponse crear(@Valid @RequestBody CrearTecnicoRequest r) {
        return mapper.toResponse(tecnicos.crear(
                new GestionarTecnicosUseCase.CrearTecnicoCommand(r.documento(), r.nombre(),
                        r.salario(), r.fechaAfiliacion(), r.especialidad(), r.sede(),
                        r.cargaMaxima(), r.username(), r.password())));
    }

    @GetMapping
    public List<TecnicoResponse> listar() {
        return tecnicos.listar().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TecnicoResponse obtener(@PathVariable Long id) {
        return mapper.toResponse(tecnicos.obtener(id));
    }

    @PutMapping("/{id}")
    public TecnicoResponse actualizar(@PathVariable Long id,
                                      @Valid @RequestBody ActualizarTecnicoRequest r) {
        return mapper.toResponse(tecnicos.actualizar(id,
                new GestionarTecnicosUseCase.ActualizarTecnicoCommand(r.nombre(), r.salario(),
                        r.especialidad(), r.sede(), r.cargaMaxima())));
    }

    @PatchMapping("/{id}/estado")
    public TecnicoResponse cambiarEstado(@PathVariable Long id,
                                         @Valid @RequestBody CambiarEstadoRegistroRequest r) {
        return mapper.toResponse(tecnicos.cambiarEstado(id, r.estado()));
    }
}
