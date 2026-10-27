package com.support.ticketservice.infrastructure.security;

import com.support.ticketservice.application.port.out.PasswordHasherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordHasherAdapter implements PasswordHasherPort {

    private final PasswordEncoder encoder;

    @Override
    public String encode(String raw) { return encoder.encode(raw); }

    @Override
    public boolean matches(String raw, String hash) { return encoder.matches(raw, hash); }
}
