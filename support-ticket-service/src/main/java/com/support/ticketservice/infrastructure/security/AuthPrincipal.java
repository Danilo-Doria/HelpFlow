package com.support.ticketservice.infrastructure.security;

import com.support.ticketservice.domain.model.enums.Rol;

public record AuthPrincipal(String username, Rol rol, Long usuarioId, Long tecnicoId) {

    public boolean esAdmin() {
        return rol == Rol.ROLE_ADMIN;
    }

    public boolean esTecnico() {
        return rol == Rol.ROLE_TECNICO;
    }

    public boolean esUsuario() {
        return rol == Rol.ROLE_USUARIO;
    }
}