package com.support.ticketservice.infrastructure.adapter.in.rest;

import com.support.ticketservice.application.port.in.GestionarUsuariosUseCase;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.ActualizarUsuarioRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.CambiarEstadoRegistroRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios", description = "Administración de usuarios solicitantes (Requiere rol ADMIN)")
@SecurityRequirement(name = "BearerAuth")
public class UsuarioController {

    private final GestionarUsuariosUseCase usuarios;
    private final UsuarioRestMapper mapper;

    @GetMapping
    @Operation(summary = "Listar todos los usuarios",
            description = "Devuelve la lista completa de usuarios registrados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ROLE_ADMIN)")
    })
    public List<UsuarioResponse> listar() {
        return usuarios.listar().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID",
            description = "Devuelve el detalle de un usuario solicitante específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ROLE_ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public UsuarioResponse obtener(@Parameter(description = "Identificador del usuario", example = "1")
                                   @PathVariable Long id) {
        return mapper.toResponse(usuarios.obtener(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar información de un usuario",
            description = "Permite actualizar nombre, sede y área de un usuario existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de actualización inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ROLE_ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public UsuarioResponse actualizar(@Parameter(description = "Identificador del usuario", example = "1")
                                      @PathVariable Long id,
                                      @Valid @RequestBody ActualizarUsuarioRequest r) {
        return mapper.toResponse(usuarios.actualizar(id, r.nombre(), r.sede(), r.area()));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activar o inactivar usuario",
            description = "Cambia el estado de un usuario a ACTIVO o INACTIVO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del usuario actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Estado inválido"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere ROLE_ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public UsuarioResponse cambiarEstado(@Parameter(description = "Identificador del usuario", example = "1")
                                         @PathVariable Long id,
                                         @Valid @RequestBody CambiarEstadoRegistroRequest r) {
        return mapper.toResponse(usuarios.cambiarEstado(id, r.estado()));
    }
}
