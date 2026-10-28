package com.support.ticketservice.domain.service;

import com.support.ticketservice.domain.exception.SlaInvalidoException;
import com.support.ticketservice.domain.model.enums.Prioridad;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlaPolicyTest {

    private Map<Prioridad, Duration> plazosValidos() {
        return Map.of(
                Prioridad.BAJA, Duration.ofHours(72),
                Prioridad.MEDIA, Duration.ofHours(24),
                Prioridad.ALTA, Duration.ofHours(8),
                Prioridad.CRITICA, Duration.ofHours(4));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void calculaFechaLimiteYExponePlazo() {
        SlaPolicy policy = new SlaPolicy(plazosValidos());
        Instant ahora = Instant.parse("2026-01-01T10:00:00Z");

        assertThat(policy.calcularFechaLimite(Prioridad.ALTA, ahora))
                .isEqualTo(ahora.plus(Duration.ofHours(8)));
        assertThat(policy.plazoDe(Prioridad.CRITICA)).isEqualTo(Duration.ofHours(4));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaPlazosFaltantes() {
        Map<Prioridad, Duration> incompletos = new EnumMap<>(Prioridad.class);
        incompletos.put(Prioridad.BAJA, Duration.ofHours(72));

        assertThatThrownBy(() -> new SlaPolicy(incompletos))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Falta plazo");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rechazaPlazosQueNoRespetanElOrdenDePrioridad() {
        Map<Prioridad, Duration> invalidos = new EnumMap<>(Prioridad.class);
        invalidos.put(Prioridad.BAJA, Duration.ofHours(1));
        invalidos.put(Prioridad.MEDIA, Duration.ofHours(2));
        invalidos.put(Prioridad.ALTA, Duration.ofHours(8));
        invalidos.put(Prioridad.CRITICA, Duration.ofHours(4));

        assertThatThrownBy(() -> new SlaPolicy(invalidos))
                .isInstanceOf(SlaInvalidoException.class)
                .hasMessageContaining("no puede ser mayor");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void conPlazoCreaUnaPoliticaNuevaSinMutarLaOriginal() {
        SlaPolicy original = new SlaPolicy(plazosValidos());
        SlaPolicy nueva = original.conPlazo(Prioridad.ALTA, Duration.ofHours(6));

        assertThat(original.plazoDe(Prioridad.ALTA)).isEqualTo(Duration.ofHours(8));
        assertThat(nueva.plazoDe(Prioridad.ALTA)).isEqualTo(Duration.ofHours(6));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void conPlazoRechazaDuracionesNoPositivas() {
        SlaPolicy policy = new SlaPolicy(plazosValidos());

        assertThatThrownBy(() -> policy.conPlazo(Prioridad.ALTA, Duration.ZERO))
                .isInstanceOf(SlaInvalidoException.class)
                .hasMessageContaining("mayor que cero");

        assertThatThrownBy(() -> policy.conPlazo(Prioridad.ALTA, Duration.ofHours(-1)))
                .isInstanceOf(SlaInvalidoException.class)
                .hasMessageContaining("mayor que cero");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void conPlazoRechazaPoliticaIncoherente() {
        SlaPolicy policy = new SlaPolicy(plazosValidos());

        assertThatThrownBy(() -> policy.conPlazo(Prioridad.CRITICA, Duration.ofHours(72)))
                .isInstanceOf(SlaInvalidoException.class);
    }
}
