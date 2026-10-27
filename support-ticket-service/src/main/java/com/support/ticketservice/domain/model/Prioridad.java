package com.support.ticketservice.domain.model;

import java.util.Optional;

public enum Prioridad {
    BAJA, MEDIA, ALTA, CRITICA;

    public Optional<Prioridad> siguiente() {
        return ordinal() == values().length - 1 ? Optional.empty() : Optional.of(values()[ordinal() + 1]);
    }
}
