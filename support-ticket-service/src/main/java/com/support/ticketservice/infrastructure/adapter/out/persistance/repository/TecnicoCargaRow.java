package com.support.ticketservice.infrastructure.adapter.out.persistance.repository;

import com.support.ticketservice.infrastructure.adapter.out.persistance.entity.TecnicoEntity;

public record TecnicoCargaRow(TecnicoEntity tecnico, Long carga) {
}
