package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.in.GestionarUsuariosUseCase;
import com.support.ticketservice.application.port.out.CredencialRepositoryPort;
import com.support.ticketservice.application.port.out.UsuarioRepositoryPort;
import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.model.Usuario;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GestionarUsuariosService implements GestionarUsuariosUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final CredencialRepositoryPort credenciales;

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.listarTodos();
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return cargar(id);
    }

    @Override
    @Transactional
    public Usuario actualizar(Long id, String nombre, String sede, String area) {
        Usuario u = cargar(id);
        return usuarios.guardar(u.actualizar(nombre, u.correo(), sede, area));
    }

    @Override
    @Transactional
    public Usuario cambiarEstado(Long id, EstadoRegistro estado) {
        Usuario guardado = usuarios.guardar(cargar(id).conEstado(estado));
        credenciales.actualizarActivoPorUsuarioId(id, estado == EstadoRegistro.ACTIVO);
        return guardado;
    }

    private Usuario cargar(Long id) {
        return usuarios.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
    }
}
