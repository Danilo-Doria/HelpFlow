package com.support.ticketservice.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Tecnico(
        Long id, String documento, String nombre,
        BigDecimal salario, LocalDate fechaAfiliacion, EstadoRegistro estado,
        Categoria especialidad, String sede, int cargaMaxima) {

    public boolean estaActivo() {
        return estado == EstadoRegistro.ACTIVO;
    }

    public boolean puedeAtender(Categoria categoria) {
        return especialidad == categoria;
    }

    public boolean tieneCapacidad(long ticketsActivos) {
        return ticketsActivos < cargaMaxima;
    }

    public Tecnico actualizar(String nombre, BigDecimal salario,
                              Categoria especialidad, String sede, int cargaMaxima) {
        return new Tecnico(id, documento, nombre, salario, fechaAfiliacion,
                estado, especialidad, sede, cargaMaxima);
    }

    public Tecnico conEstado(EstadoRegistro nuevo) {
        return new Tecnico(id, documento, nombre, salario, fechaAfiliacion,
                nuevo, especialidad, sede, cargaMaxima);
    }
}
