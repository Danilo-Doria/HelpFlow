package com.support.ticketservice.domain.model;

import com.support.ticketservice.domain.model.enums.Disponibilidad;

public record SugerenciaDespacho(
        String tecnicoCodigo,
        Disponibilidad disponibilidad,
        int tiempoEstimadoHoras) {
}
