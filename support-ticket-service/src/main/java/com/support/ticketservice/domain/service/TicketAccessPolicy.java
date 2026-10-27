package com.support.ticketservice.domain.service;

import com.support.ticketservice.domain.model.Actor;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.model.enums.EstadoTicket;

public class TicketAccessPolicy {

    public boolean puedeVer(Ticket ticket, Actor actor) {
        return switch (actor.rol()) {
            case ROLE_ADMIN -> true;
            case ROLE_USUARIO -> ticket.getSolicitante().id().equals(actor.usuarioId());
            case ROLE_TECNICO -> ticket.getTecnicoAsignado() != null
                    && ticket.getTecnicoAsignado().id().equals(actor.tecnicoId());
        };
    }

    public boolean puedeCambiarA(Ticket ticket, Actor actor, EstadoTicket destino) {
        if (!puedeVer(ticket, actor)) return false;
        return switch (actor.rol()) {
            case ROLE_ADMIN -> true;
            case ROLE_TECNICO -> destino == EstadoTicket.EN_PROGRESO || destino == EstadoTicket.RESUELTO;
            case ROLE_USUARIO -> destino == EstadoTicket.CANCELADO || destino == EstadoTicket.CERRADO;
        };
    }
}
