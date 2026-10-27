package com.support.ticketservice;

import org.springframework.boot.SpringApplication;

public class TestSupportTicketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(SupportTicketServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
