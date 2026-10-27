package com.support.ticketservice.infrastructure.adapter.in.rest.dto.response;

import com.support.ticketservice.domain.model.enums.Prioridad;

public record SlaResponse(Prioridad prioridad, long horas) {
}
