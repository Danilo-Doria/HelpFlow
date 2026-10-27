package com.support.ticketservice.domain.model;

import com.support.ticketservice.domain.model.enums.Rol;

public record Credencial(
        Long id,
        String username,
        String passwordHash,
        Rol rol,
        boolean activo,
        Long usuarioId,
        Long tecnicoId) {
}
