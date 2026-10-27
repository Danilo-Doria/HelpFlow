package com.support.ticketservice.application.port.out;

import com.support.ticketservice.domain.model.Credencial;

public interface TokenGeneratorPort {
    TokenEmitido generar(Credencial credencial);

    record TokenEmitido(String token, long expiraEnSegundos) {
    }
}
