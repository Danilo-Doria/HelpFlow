package com.support.ticketservice.domain.exception;

import com.support.ticketservice.domain.model.enums.EstadoTicket;

public class TransicionInvalidaException extends DomainException {
    public TransicionInvalidaException(EstadoTicket origen, EstadoTicket destino) {
        super("Transición no permitida: " + origen + " -> " + destino);
    }
}
