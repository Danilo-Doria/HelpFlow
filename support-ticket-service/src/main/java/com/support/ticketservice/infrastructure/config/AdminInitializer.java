package com.support.ticketservice.infrastructure.config;

import com.support.ticketservice.application.port.out.CredencialRepositoryPort;
import com.support.ticketservice.application.port.out.PasswordHasherPort;
import com.support.ticketservice.domain.model.Credencial;
import com.support.ticketservice.domain.model.enums.Rol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements ApplicationRunner {

    private final CredencialRepositoryPort credenciales;
    private final PasswordHasherPort hasher;

    @Value("${app.admin.username}") private String username;
    @Value("${app.admin.password}") private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (credenciales.existePorUsername(username)) return;
        credenciales.guardar(new Credencial(null, username, hasher.encode(password),
                Rol.ROLE_ADMIN, true, null, null));
        log.info("Administrador inicial creado: {}", username);
    }
}
