package com.support.ticketservice.infrastructure.adapter.out.dispatch;

import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.Prioridad;

record DispatchRequest(Long ticketId, Categoria categoria, Prioridad prioridad) {
}
