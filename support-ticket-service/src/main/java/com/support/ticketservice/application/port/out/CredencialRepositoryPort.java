package com.support.ticketservice.application.port.out;

import com.support.ticketservice.domain.model.Credencial;

import java.util.Optional;

public interface CredencialRepositoryPort {
    Credencial guardar(Credencial credencial);

    Optional<Credencial> buscarPorUsername(String username);

    boolean existePorUsername(String username);

    void actualizarActivoPorUsuarioId(Long usuarioId, boolean activo);

    void actualizarActivoPorTecnicoId(Long tecnicoId, boolean activo);
}
