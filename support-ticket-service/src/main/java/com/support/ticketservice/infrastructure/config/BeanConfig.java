package com.support.ticketservice.infrastructure.config;

import com.support.ticketservice.domain.service.TecnicoSelector;
import com.support.ticketservice.domain.service.TicketAccessPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class BeanConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public TecnicoSelector tecnicoSelector() {
        return new TecnicoSelector();
    }

    @Bean
    public TicketAccessPolicy ticketAccessPolicy() {
        return new TicketAccessPolicy();
    }
}
