package com.support.ticketservice.domain.model;

import com.support.ticketservice.domain.model.enums.EstadoRegistro;

public record Usuario(
        Long id, String documento, String nombre, String correo,
        String sede, String area, EstadoRegistro estado) {

    public boolean estaActivo() {
        return estado == EstadoRegistro.ACTIVO;
    }

    public Usuario actualizar(String nombre, String correo, String sede, String area) {
        return new Usuario(id, documento, nombre, correo, sede, area, estado);
    }

    public Usuario conEstado(EstadoRegistro nuevo) {
        return new Usuario(id, documento, nombre, correo, sede, area, nuevo);
    }
}
