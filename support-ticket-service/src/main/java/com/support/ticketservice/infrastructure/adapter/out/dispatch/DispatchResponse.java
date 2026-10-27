package com.support.ticketservice.infrastructure.adapter.out.dispatch;

import com.support.ticketservice.domain.model.enums.Disponibilidad;

record DispatchResponse(Long ticketId, String tecnicoId, Disponibilidad disponibilidad, Integer tiempoEstimadoHoras) {
}
