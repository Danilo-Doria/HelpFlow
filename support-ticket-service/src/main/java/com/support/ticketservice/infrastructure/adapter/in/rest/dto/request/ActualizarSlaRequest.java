package com.support.ticketservice.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarSlaRequest(@NotNull @Min(1) @Max(720) Integer horas) {
}
