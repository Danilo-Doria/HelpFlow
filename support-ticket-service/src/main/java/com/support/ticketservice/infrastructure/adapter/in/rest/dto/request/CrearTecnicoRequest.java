package com.support.ticketservice.infrastructure.adapter.in.rest.dto.request;

import com.support.ticketservice.domain.model.enums.Categoria;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CrearTecnicoRequest(
        @NotBlank @Size(max = 20) String documento,
        @NotBlank @Size(max = 120) String nombre,
        @NotNull @PositiveOrZero BigDecimal salario,
        @NotNull @PastOrPresent LocalDate fechaAfiliacion,
        @NotNull Categoria especialidad,
        @NotBlank @Size(max = 60) String sede,
        @NotNull @Min(1) @Max(50) Integer cargaMaxima,
        @NotBlank @Email @Size(max = 150) String username,
        @NotBlank @Size(min = 8, max = 72) String password) {
}
