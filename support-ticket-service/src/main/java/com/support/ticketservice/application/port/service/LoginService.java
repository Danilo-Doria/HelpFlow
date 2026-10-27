package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.in.LoginUseCase;
import com.support.ticketservice.application.port.out.CredencialRepositoryPort;
import com.support.ticketservice.application.port.out.PasswordHasherPort;
import com.support.ticketservice.application.port.out.TokenGeneratorPort;
import com.support.ticketservice.domain.exception.CredencialesInvalidasException;
import com.support.ticketservice.domain.model.Credencial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private final CredencialRepositoryPort credenciales;
    private final PasswordHasherPort hasher;
    private final TokenGeneratorPort tokens;

    @Override
    @Transactional(readOnly = true)
    public TokenGeneratorPort.TokenEmitido ejecutar(String username, String password) {
        Credencial credencial = credenciales.buscarPorUsername(username.trim().toLowerCase())
                .filter(Credencial::activo)
                .filter(c -> hasher.matches(password, c.passwordHash()))
                .orElseThrow(CredencialesInvalidasException::new);
        return tokens.generar(credencial);
    }
}
