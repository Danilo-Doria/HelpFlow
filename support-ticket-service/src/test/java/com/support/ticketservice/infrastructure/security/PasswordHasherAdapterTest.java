package com.support.ticketservice.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHasherAdapterTest {

    private final PasswordHasherAdapter adapter =
            new PasswordHasherAdapter(new BCryptPasswordEncoder());

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void codificaYVerificaPassword() {
        String hash = adapter.encode("Secreta123");

        assertThat(hash).isNotEqualTo("Secreta123");
        assertThat(adapter.matches("Secreta123", hash)).isTrue();
        assertThat(adapter.matches("otra", hash)).isFalse();
    }
}
