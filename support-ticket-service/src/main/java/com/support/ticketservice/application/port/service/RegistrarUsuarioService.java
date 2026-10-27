package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.in.RegistrarUsuarioUseCase;
import com.support.ticketservice.application.port.out.CredencialRepositoryPort;
import com.support.ticketservice.application.port.out.PasswordHasherPort;
import com.support.ticketservice.application.port.out.UsuarioRepositoryPort;
import com.support.ticketservice.domain.exception.DuplicadoException;
import com.support.ticketservice.domain.model.Credencial;
import com.support.ticketservice.domain.model.Usuario;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import com.support.ticketservice.domain.model.enums.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrarUsuarioService implements RegistrarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final CredencialRepositoryPort credenciales;
    private final PasswordHasherPort hasher;

    @Override
    @Transactional
    public Usuario ejecutar(RegistrarUsuarioCommand cmd) {
        String correo = cmd.correo().trim().toLowerCase();

        if (usuarios.existePorDocumento(cmd.documento())) {
            throw new DuplicadoException("Ya existe un usuario con el documento " + cmd.documento());
        }
        if (usuarios.existePorCorreo(correo)) {
            throw new DuplicadoException("Ya existe un usuario con el correo " + correo);
        }

        Usuario usuario = usuarios.guardar(new Usuario(null, cmd.documento(), cmd.nombre(),
                correo, cmd.sede(), cmd.area(), EstadoRegistro.ACTIVO));

        credenciales.guardar(new Credencial(null, correo, hasher.encode(cmd.password()),
                Rol.ROLE_USUARIO, true, usuario.id(), null));

        return usuario;
    }
}
