package com.support.ticketservice.infrastructure.adapter.out.dispatch;

import com.support.ticketservice.application.port.out.TechnicianDispatchPort;
import com.support.ticketservice.domain.model.SugerenciaDespacho;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.Prioridad;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
@Slf4j
public class TechnicianDispatchRestAdapter implements TechnicianDispatchPort {

    private final RestClient client;

    public TechnicianDispatchRestAdapter(@Qualifier("dispatchRestClient") RestClient client) {
        this.client = client;
    }

    @Override
    public Optional<SugerenciaDespacho> consultar(Long ticketId, Categoria categoria, Prioridad prioridad) {
        try {
            DispatchResponse r = client.post()
                    .uri("/technician-dispatch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DispatchRequest(ticketId, categoria, prioridad))
                    .retrieve()
                    .body(DispatchResponse.class);

            if (r == null || r.tecnicoId() == null
                    || r.disponibilidad() == null || r.tiempoEstimadoHoras() == null) {
                log.warn("Respuesta incompleta del despacho para ticket {}", ticketId);
                return Optional.empty();
            }
            return Optional.of(new SugerenciaDespacho(
                    r.tecnicoId(), r.disponibilidad(), r.tiempoEstimadoHoras()));
        } catch (Exception e) {
            log.warn("technician-dispatch-service no disponible para ticket {}: {}",
                    ticketId, e.getMessage());
            return Optional.empty();
        }
    }
}
