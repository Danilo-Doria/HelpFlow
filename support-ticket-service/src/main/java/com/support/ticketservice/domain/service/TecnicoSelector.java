package com.support.ticketservice.domain.service;

import com.support.ticketservice.domain.model.Tecnico;
import com.support.ticketservice.domain.model.TecnicoConCarga;
import com.support.ticketservice.domain.model.Ticket;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TecnicoSelector {

    public Optional<Tecnico> seleccionar(Ticket ticket, List<TecnicoConCarga> candidatos) {
        return candidatos.stream()
                .filter(c -> c.tecnico().estaActivo())
                .filter(c -> c.tecnico().puedeAtender(ticket.getCategoria()))
                .filter(TecnicoConCarga::tieneCapacidad)
                .min(Comparator.comparing((TecnicoConCarga c) -> !c.tecnico().sede().equalsIgnoreCase(ticket.getSede()))
                        .thenComparingLong(TecnicoConCarga::ticketsActivos)
                        .thenComparing(c -> c.tecnico().id()))
                .map(TecnicoConCarga::tecnico);
    }
}
