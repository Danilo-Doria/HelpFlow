package com.support.ticketservice.domain.model;

public record TecnicoConCarga(Tecnico tecnico, long ticketsActivos) {
    public boolean tieneCapacidad() {
        return tecnico.tieneCapacidad(ticketsActivos);
    }
}
