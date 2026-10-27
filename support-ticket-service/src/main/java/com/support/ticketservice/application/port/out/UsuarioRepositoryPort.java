package com.support.ticketservice.application.port.out;

import com.support.ticketservice.domain.model.Usuario;

import java.util.Optional;

public interface UsuarioRepositoryPort {
    Optional<Usuario> buscarPorId(Long id);
}
