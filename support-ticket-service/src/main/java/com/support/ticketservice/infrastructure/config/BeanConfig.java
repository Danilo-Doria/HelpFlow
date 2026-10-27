package com.support.ticketservice.infrastructure.config;

import com.support.ticketservice.domain.service.TecnicoSelector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BeanConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public TecnicoSelector tecnicoSelector() {
        return new TecnicoSelector();
    }
}
