package com.support.ticketservice.domain.service;

import com.support.ticketservice.domain.model.Tecnico;
import com.support.ticketservice.domain.model.TecnicoConCarga;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.model.Usuario;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import com.support.ticketservice.domain.model.enums.Prioridad;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TecnicoSelectorTest {

    private final TecnicoSelector selector = new TecnicoSelector();

    private Tecnico tecnico(long id, String sede, Categoria especialidad,
                            EstadoRegistro estado, int cargaMaxima) {
        return new Tecnico(id, "T-" + id, "Tecnico " + id, new BigDecimal("3000000"),
                LocalDate.of(2020, 1, 1), estado, especialidad, sede, cargaMaxima);
    }

    private Ticket ticket(String sede) {
        Usuario usuario = new Usuario(1L, "123", "Ana", "ana@helpflow.com",
                sede, "TI", EstadoRegistro.ACTIVO);
        return Ticket.abrir("T", "D", Categoria.HARDWARE, Prioridad.ALTA,
                sede, null, usuario,
                new SlaPolicy(Map.of(Prioridad.BAJA, Duration.ofHours(72),
                        Prioridad.MEDIA, Duration.ofHours(24),
                        Prioridad.ALTA, Duration.ofHours(8),
                        Prioridad.CRITICA, Duration.ofHours(4))),
                Instant.parse("2026-01-01T10:00:00Z"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void prefiereMismaSedeLuegoMenorCargaYLuegoMenorId() {
        TecnicoConCarga otraSede = new TecnicoConCarga(
                tecnico(1, "MEDELLIN", Categoria.HARDWARE, EstadoRegistro.ACTIVO, 5), 0);
        TecnicoConCarga mismaSedeCargado = new TecnicoConCarga(
                tecnico(2, "BOGOTA", Categoria.HARDWARE, EstadoRegistro.ACTIVO, 5), 3);
        TecnicoConCarga mismaSedeLibre = new TecnicoConCarga(
                tecnico(3, "BOGOTA", Categoria.HARDWARE, EstadoRegistro.ACTIVO, 5), 1);

        Optional<Tecnico> elegido = selector.seleccionar(ticket("BOGOTA"),
                List.of(otraSede, mismaSedeCargado, mismaSedeLibre));

        assertThat(elegido).containsSame(mismaSedeLibre.tecnico());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void desempataPorIdCuandoSedeYCargaSonIguales() {
        TecnicoConCarga mayorId = new TecnicoConCarga(
                tecnico(9, "BOGOTA", Categoria.HARDWARE, EstadoRegistro.ACTIVO, 5), 1);
        TecnicoConCarga menorId = new TecnicoConCarga(
                tecnico(4, "BOGOTA", Categoria.HARDWARE, EstadoRegistro.ACTIVO, 5), 1);

        Optional<Tecnico> elegido = selector.seleccionar(ticket("BOGOTA"),
                List.of(mayorId, menorId));

        assertThat(elegido).containsSame(menorId.tecnico());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void descartaInactivosEspecialidadIncorrectaYSinCapacidad() {
        TecnicoConCarga inactivo = new TecnicoConCarga(
                tecnico(1, "BOGOTA", Categoria.HARDWARE, EstadoRegistro.INACTIVO, 5), 0);
        TecnicoConCarga otraEspecialidad = new TecnicoConCarga(
                tecnico(2, "BOGOTA", Categoria.RED, EstadoRegistro.ACTIVO, 5), 0);
        TecnicoConCarga saturado = new TecnicoConCarga(
                tecnico(3, "BOGOTA", Categoria.HARDWARE, EstadoRegistro.ACTIVO, 2), 2);
        TecnicoConCarga valido = new TecnicoConCarga(
                tecnico(4, "MEDELLIN", Categoria.HARDWARE, EstadoRegistro.ACTIVO, 3), 1);

        Optional<Tecnico> elegido = selector.seleccionar(ticket("BOGOTA"),
                List.of(inactivo, otraEspecialidad, saturado, valido));

        assertThat(elegido).containsSame(valido.tecnico());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void devuelveVacioCuandoNoHayCandidatos() {
        assertThat(selector.seleccionar(ticket("BOGOTA"), List.of())).isEmpty();
    }
}
