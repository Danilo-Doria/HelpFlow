package com.support.ticketservice.infrastructure.adapter.in.rest;

import com.support.ticketservice.application.port.in.LoginUseCase;
import com.support.ticketservice.application.port.in.RegistrarUsuarioUseCase;
import com.support.ticketservice.domain.model.Usuario;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.RegisterRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.RegisterResponse;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de registro de usuarios y generación de tokens JWT")
public class AuthController {

    private final RegistrarUsuarioUseCase registrar;
    private final LoginUseCase login;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements // Endpoint público, no requiere JWT
    @Operation(summary = "Registrar nuevo usuario solicitante",
            description = "Crea un nuevo usuario en estado ACTIVO y genera sus credenciales de acceso.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "Conflicto: ya existe un usuario con ese documento o correo")
    })
    public RegisterResponse register(@Valid @RequestBody RegisterRequest r) {
        Usuario u = registrar.ejecutar(new RegistrarUsuarioUseCase.RegistrarUsuarioCommand(
                r.documento(), r.nombre(), r.correo(), r.sede(), r.area(), r.password()));
        return new RegisterResponse(u.id(), u.documento(), u.nombre(), u.correo());
    }

    @PostMapping("/login")
    @SecurityRequirements // Endpoint público, no requiere JWT
    @Operation(summary = "Iniciar sesión",
            description = "Autentica las credenciales (correo y contraseña) y retorna un token JWT Bearer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa"),
            @ApiResponse(responseCode = "400", description = "Formato de credenciales inválido"),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas o usuario inactivo")
    })
    public TokenResponse login(@Valid @RequestBody LoginRequest r) {
        var t = login.ejecutar(r.username(), r.password());
        return new TokenResponse(t.token(), "Bearer", t.expiraEnSegundos());
    }
}
