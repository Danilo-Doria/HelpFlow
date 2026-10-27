package com.support.ticketservice.infrastructure.adapter.in.rest.dto.request;

import com.support.ticketservice.domain.model.enums.Categoria;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ActualizarTecnicoRequest(
        @NotBlank @Size(max = 120) String nombre,
        @NotNull @PositiveOrZero BigDecimal salario,
        @NotNull Categoria especialidad,
        @NotBlank @Size(max = 60) String sede,
        @NotNull @Min(1) @Max(50) Integer cargaMaxima) {
}
