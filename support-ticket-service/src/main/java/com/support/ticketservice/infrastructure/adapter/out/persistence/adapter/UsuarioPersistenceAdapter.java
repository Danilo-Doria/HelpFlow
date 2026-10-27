package com.support.ticketservice.infrastructure.adapter.out.persistence.adapter;

import com.support.ticketservice.application.port.out.UsuarioRepositoryPort;
import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.model.Usuario;
import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import com.support.ticketservice.infrastructure.adapter.out.persistence.mapper.UsuarioPersistenceMapper;
import com.support.ticketservice.infrastructure.adapter.out.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository jpa;
    private final UsuarioPersistenceMapper mapper;

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity entity = usuario.id() == null
                ? new UsuarioEntity()
                : jpa.findById(usuario.id()).orElseThrow(() ->
                new RecursoNoEncontradoException("Usuario no encontrado: " + usuario.id()));
        mapper.actualizar(usuario, entity);
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public boolean existePorDocumento(String documento) {
        return jpa.existsByDocumento(documento);
    }

    @Override
    public boolean existePorCorreo(String correo) {
        return jpa.existsByCorreoIgnoreCase(correo);
    }

    @Override
    public List<Usuario> listarTodos() {
        return jpa.findAll(Sort.by("id")).stream().map(mapper::toDomain).toList();
    }
}
