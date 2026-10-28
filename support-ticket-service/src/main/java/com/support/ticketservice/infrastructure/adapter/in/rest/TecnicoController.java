package com.support.ticketservice.infrastructure.adapter.in.rest;

import com.support.ticketservice.application.port.in.GestionarTecnicosUseCase;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.ActualizarTecnicoRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.CambiarEstadoRegistroRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.CrearTecnicoRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.TecnicoResponse;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tecnicos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Técnicos", description = "Gestión del personal técnico (Requiere rol ADMIN)")
@SecurityRequirement(name = "BearerAuth")
public class TecnicoController {

    private final GestionarTecnicosUseCase tecnicos;
    private final TecnicoRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear nuevo técnico",
            description = "Registra un técnico de soporte con su credencial de acceso asociada.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Técnico creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ROLE_ADMIN)"),
            @ApiResponse(responseCode = "409", description = "Conflicto: ya existe un técnico con ese documento o username")
    })
    public TecnicoResponse crear(@Valid @RequestBody CrearTecnicoRequest r) {
        return mapper.toResponse(tecnicos.crear(
                new GestionarTecnicosUseCase.CrearTecnicoCommand(r.documento(), r.nombre(),
                        r.salario(), r.fechaAfiliacion(), r.especialidad(), r.sede(),
                        r.cargaMaxima(), r.username(), r.password())));
    }

    @GetMapping
    @Operation(summary = "Listar todos los técnicos",
            description = "Devuelve el listado de todos los técnicos registrados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de técnicos obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ROLE_ADMIN)")
    })
    public List<TecnicoResponse> listar() {
        return tecnicos.listar().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener técnico por ID",
            description = "Devuelve el detalle de un técnico específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Técnico encontrado"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ROLE_ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Técnico no encontrado")
    })
    public TecnicoResponse obtener(@Parameter(description = "Identificador del técnico", example = "1")
                                   @PathVariable Long id) {
        return mapper.toResponse(tecnicos.obtener(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar información de un técnico",
            description = "Actualiza nombre, salario, especialidad, sede y carga máxima de un técnico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Técnico actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de actualización inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ROLE_ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Técnico no encontrado")
    })
    public TecnicoResponse actualizar(@Parameter(description = "Identificador del técnico", example = "1")
                                      @PathVariable Long id,
                                      @Valid @RequestBody ActualizarTecnicoRequest r) {
        return mapper.toResponse(tecnicos.actualizar(id,
                new GestionarTecnicosUseCase.ActualizarTecnicoCommand(r.nombre(), r.salario(),
                        r.especialidad(), r.sede(), r.cargaMaxima())));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activar o inactivar técnico",
            description = "Cambia el estado de un técnico a ACTIVO o INACTIVO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del técnico actualizado"),
            @ApiResponse(responseCode = "400", description = "Estado inválido"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ROLE_ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Técnico no encontrado")
    })
    public TecnicoResponse cambiarEstado(@Parameter(description = "Identificador del técnico", example = "1")
                                         @PathVariable Long id,
                                         @Valid @RequestBody CambiarEstadoRegistroRequest r) {
        return mapper.toResponse(tecnicos.cambiarEstado(id, r.estado()));
    }
}
