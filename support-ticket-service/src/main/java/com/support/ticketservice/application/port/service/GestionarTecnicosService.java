package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.in.GestionarTecnicosUseCase;
import com.support.ticketservice.application.port.out.CredencialRepositoryPort;
import com.support.ticketservice.application.port.out.PasswordHasherPort;
import com.support.ticketservice.application.port.out.TecnicoRepositoryPort;
import com.support.ticketservice.domain.exception.DuplicadoException;
import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.model.Credencial;
import com.support.ticketservice.domain.model.Tecnico;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import com.support.ticketservice.domain.model.enums.Rol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GestionarTecnicosService implements GestionarTecnicosUseCase {

    private final TecnicoRepositoryPort tecnicos;
    private final CredencialRepositoryPort credenciales;
    private final PasswordHasherPort hasher;

    @Override
    @Transactional
    public Tecnico crear(CrearTecnicoCommand cmd) {
        String username = cmd.username().trim().toLowerCase();

        if (tecnicos.existePorDocumento(cmd.documento())) {
            throw new DuplicadoException("Ya existe un técnico con el documento " + cmd.documento());
        }
        if (credenciales.existePorUsername(username)) {
            throw new DuplicadoException("Ya existe una cuenta con el usuario " + username);
        }

        Tecnico tecnico = tecnicos.guardar(new Tecnico(null, cmd.documento(), cmd.nombre(),
                cmd.salario(), cmd.fechaAfiliacion(), EstadoRegistro.ACTIVO,
                cmd.especialidad(), cmd.sede(), cmd.cargaMaxima()));

        credenciales.guardar(new Credencial(null, username, hasher.encode(cmd.password()),
                Rol.ROLE_TECNICO, true, null, tecnico.id()));

        return tecnico;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tecnico> listar() {
        return tecnicos.listarTodos();
    }

    @Override
    @Transactional(readOnly = true)
    public Tecnico obtener(Long id) {
        return cargar(id);
    }

    @Override
    @Transactional
    public Tecnico actualizar(Long id, ActualizarTecnicoCommand cmd) {
        Tecnico t = cargar(id);
        return tecnicos.guardar(t.actualizar(cmd.nombre(), cmd.salario(),
                cmd.especialidad(), cmd.sede(), cmd.cargaMaxima()));
    }

    @Override
    @Transactional
    public Tecnico cambiarEstado(Long id, EstadoRegistro estado) {
        Tecnico guardado = tecnicos.guardar(cargar(id).conEstado(estado));
        credenciales.actualizarActivoPorTecnicoId(id, estado == EstadoRegistro.ACTIVO);
        return guardado;
    }

    private Tecnico cargar(Long id) {
        return tecnicos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Técnico no encontrado: " + id));
    }
}
