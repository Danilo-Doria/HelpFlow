package com.support.ticketservice.infrastructure.adapter.out.persistence.repository;

import com.support.ticketservice.infrastructure.adapter.out.persistence.entity.TecnicoEntity;

public record TecnicoCargaRow(TecnicoEntity tecnico, Long carga) {
}
