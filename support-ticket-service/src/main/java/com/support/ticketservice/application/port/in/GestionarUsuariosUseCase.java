package com.support.ticketservice.application.port.in;

import com.support.ticketservice.domain.model.Usuario;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;

import java.util.List;

public interface GestionarUsuariosUseCase {
    List<Usuario> listar();

    Usuario obtener(Long id);

    Usuario actualizar(Long id, String nombre, String sede, String area);

    Usuario cambiarEstado(Long id, EstadoRegistro estado);
}