package com.support.ticketservice.application.port.out;

import com.support.ticketservice.domain.service.SlaPolicy;

public interface SlaConfigPort {
    SlaPolicy obtener();
}
