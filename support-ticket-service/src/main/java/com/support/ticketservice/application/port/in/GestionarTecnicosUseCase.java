package com.support.ticketservice.application.port.in;

import com.support.ticketservice.domain.model.Tecnico;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GestionarTecnicosUseCase {
    Tecnico crear(CrearTecnicoCommand cmd);

    List<Tecnico> listar();

    Tecnico obtener(Long id);

    Tecnico actualizar(Long id, ActualizarTecnicoCommand cmd);

    Tecnico cambiarEstado(Long id, EstadoRegistro estado);

    record CrearTecnicoCommand(String documento, String nombre, BigDecimal salario,
                               LocalDate fechaAfiliacion, Categoria especialidad,
                               String sede, int cargaMaxima,
                               String username, String password) {
    }

    record ActualizarTecnicoCommand(String nombre, BigDecimal salario,
                                    Categoria especialidad, String sede, int cargaMaxima) {
    }
}