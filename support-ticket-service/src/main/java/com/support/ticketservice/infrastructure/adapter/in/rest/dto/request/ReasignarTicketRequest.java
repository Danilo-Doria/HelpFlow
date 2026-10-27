package com.support.ticketservice.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReasignarTicketRequest(
        @NotNull Long tecnicoId,
        @Size(max = 500) String observacion) {
}
