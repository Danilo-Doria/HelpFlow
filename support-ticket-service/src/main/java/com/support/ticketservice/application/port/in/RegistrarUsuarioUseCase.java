package com.support.ticketservice.application.port.in;

import com.support.ticketservice.domain.model.Usuario;

public interface RegistrarUsuarioUseCase {

    Usuario ejecutar(RegistrarUsuarioCommand command);

    record RegistrarUsuarioCommand(String documento, String nombre, String correo,
                                   String sede, String area, String password) {
    }
}
