package com.support.ticketservice.application.port.in;

import com.support.ticketservice.application.port.out.TokenGeneratorPort;

public interface LoginUseCase {
    TokenGeneratorPort.TokenEmitido ejecutar(String username, String password);
}
