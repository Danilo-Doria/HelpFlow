package com.support.ticketservice.domain.model.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum EstadoTicket {
    ABIERTO, ASIGNADO, EN_PROGRESO, RESUELTO, CERRADO, CANCELADO;

    private static final Map<EstadoTicket, Set<EstadoTicket>> PERMITIDAS = Map.of(
            ABIERTO, EnumSet.of(ASIGNADO, CANCELADO),
            ASIGNADO, EnumSet.of(ASIGNADO, EN_PROGRESO, CANCELADO),
            EN_PROGRESO, EnumSet.of(RESUELTO, CANCELADO),
            RESUELTO, EnumSet.of(CERRADO, EN_PROGRESO),
            CERRADO, EnumSet.noneOf(EstadoTicket.class),
            CANCELADO, EnumSet.noneOf(EstadoTicket.class)
    );

    public boolean puedePasarA(EstadoTicket destino) {
        return PERMITIDAS.get(this).contains(destino);
    }

    public boolean esActivo() {
        return this == ASIGNADO || this == EN_PROGRESO;
    }

    public boolean esTerminal() {
        return this == CERRADO || this == CANCELADO;
    }

    public boolean cuentaParaSla() {return this == ABIERTO || this == ASIGNADO || this == EN_PROGRESO; }
}
