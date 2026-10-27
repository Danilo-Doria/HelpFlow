package com.support.ticketservice.application.port.out;

import com.support.ticketservice.domain.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositoryPort {
    Optional<Usuario> buscarPorId(Long id);

    Usuario guardar(Usuario usuario);

    boolean existePorDocumento(String documento);

    boolean existePorCorreo(String correo);

    List<Usuario> listarTodos();
}
