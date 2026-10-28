package com.support.ticketservice.infrastructure.config;

import com.support.ticketservice.application.port.out.CredencialRepositoryPort;
import com.support.ticketservice.application.port.out.PasswordHasherPort;
import com.support.ticketservice.domain.model.Credencial;
import com.support.ticketservice.domain.model.enums.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock private CredencialRepositoryPort credenciales;
    @Mock private PasswordHasherPort hasher;
    @InjectMocks private AdminInitializer initializer;

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void creaAdminCuandoNoExiste() throws Exception {
        ReflectionTestUtils.setField(initializer, "username", "admin@helpflow.com");
        ReflectionTestUtils.setField(initializer, "password", "admin123");
        when(credenciales.existePorUsername("admin@helpflow.com")).thenReturn(false);
        when(hasher.encode("admin123")).thenReturn("hash-admin");

        initializer.run(null);

        ArgumentCaptor<Credencial> captor = ArgumentCaptor.forClass(Credencial.class);
        verify(credenciales).guardar(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("admin@helpflow.com");
        assertThat(captor.getValue().passwordHash()).isEqualTo("hash-admin");
        assertThat(captor.getValue().rol()).isEqualTo(Rol.ROLE_ADMIN);
        assertThat(captor.getValue().activo()).isTrue();
        assertThat(captor.getValue().usuarioId()).isNull();
        assertThat(captor.getValue().tecnicoId()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void noDuplicaAdminSiYaExiste() throws Exception {
        ReflectionTestUtils.setField(initializer, "username", "admin@helpflow.com");
        ReflectionTestUtils.setField(initializer, "password", "admin123");
        when(credenciales.existePorUsername("admin@helpflow.com")).thenReturn(true);

        initializer.run(null);

        verify(credenciales, never()).guardar(any());
        verify(hasher, never()).encode(any());
    }
}
