package com.support.ticketservice.application.port.out;

public interface PasswordHasherPort {
    String encode(String raw);

    boolean matches(String raw, String hash);
}
