package com.support.ticketservice.infrastructure.adapter.in.rest.dto.response;

import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TecnicoResponse(Long id, String documento, String nombre, BigDecimal salario,
                              LocalDate fechaAfiliacion, EstadoRegistro estado,
                              Categoria especialidad, String sede, int cargaMaxima) {
}