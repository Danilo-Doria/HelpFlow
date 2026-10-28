package com.technician.dispatchservice.service;

import com.technician.dispatchservice.dto.DispatchRequest;
import com.technician.dispatchservice.dto.DispatchResponse;
import com.technician.dispatchservice.dto.Disponibilidad;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DispatchService {

    private static final int TECNICOS_SIMULADOS = 20;
    private static final int HORAS_MAXIMAS = 12;

    public DispatchResponse calcular(DispatchRequest request) {
        long seed = seed(request);

        int numeroTecnico = (int) (seed % TECNICOS_SIMULADOS) + 1;           // 1..20
        int horas = (int) ((seed / TECNICOS_SIMULADOS) % (HORAS_MAXIMAS + 1)); // 0..12

        DispatchResponse response = new DispatchResponse(
                request.ticketId(),
                "TEC-%03d".formatted(numeroTecnico),
                clasificar(horas),
                horas);

        log.info("Despacho ticket={} categoria={} prioridad={} -> {} ({} h, {})",
                request.ticketId(), request.categoria(), request.prioridad(),
                response.tecnicoId(), horas, response.disponibilidad());
        return response;
    }

    /** Determinista entre ejecuciones: String.hashCode está especificado por el lenguaje. */
    private long seed(DispatchRequest r) {
        String clave = "%d|%s|%s".formatted(r.ticketId(), r.categoria().name(), r.prioridad().name());
        return Integer.toUnsignedLong(clave.hashCode());
    }

    private Disponibilidad clasificar(int horas) {
        if (horas <= 2) return Disponibilidad.ALTA;
        if (horas <= 8) return Disponibilidad.MEDIA;
        return Disponibilidad.BAJA;
    }
}
