package com.support.ticketservice.infrastructure.adapter.in.rest;

import com.support.ticketservice.application.port.in.LoginUseCase;
import com.support.ticketservice.application.port.in.RegistrarUsuarioUseCase;
import com.support.ticketservice.domain.model.Usuario;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.request.RegisterRequest;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.RegisterResponse;
import com.support.ticketservice.infrastructure.adapter.in.rest.dto.response.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrarUsuarioUseCase registrar;
    private final LoginUseCase login;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest r) {
        Usuario u = registrar.ejecutar(new RegistrarUsuarioUseCase.RegistrarUsuarioCommand(
                r.documento(), r.nombre(), r.correo(), r.sede(), r.area(), r.password()));
        return new RegisterResponse(u.id(), u.documento(), u.nombre(), u.correo());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest r) {
        var t = login.ejecutar(r.username(), r.password());
        return new TokenResponse(t.token(), "Bearer", t.expiraEnSegundos());
    }
}