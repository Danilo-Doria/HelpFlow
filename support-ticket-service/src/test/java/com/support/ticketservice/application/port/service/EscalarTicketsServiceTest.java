package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.out.HistorialRepositoryPort;
import com.support.ticketservice.application.port.out.TicketMetricsPort;
import com.support.ticketservice.application.port.out.TicketRepositoryPort;
import com.support.ticketservice.domain.model.HistorialTicket;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.domain.model.enums.Prioridad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.support.ticketservice.support.DomainFixtures.AHORA;
import static com.support.ticketservice.support.DomainFixtures.ticketVencido;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscalarTicketsServiceTest {

    @Mock private TicketRepositoryPort tickets;
    @Mock private HistorialRepositoryPort historial;
    @Mock private TransactionTemplate tx;
    @Mock private TicketMetricsPort metrics;
    @Mock private Clock clock;
    @InjectMocks private EscalarTicketsService service;

    @BeforeEach
    void configurar() {
        ReflectionTestUtils.setField(service, "umbral", Duration.ofHours(2));
        when(clock.instant()).thenReturn(AHORA);
    }

    private void ejecutarTransacciones() {
        when(tx.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Boolean> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void escalaUnTicketProximoAVencerYRegistraHistorial() {
        Ticket ticket = new Ticket(1L, "T", "D", Categoria.HARDWARE, Prioridad.MEDIA,
                "BOGOTA", null, AHORA.minus(Duration.ofHours(1)), AHORA.plus(Duration.ofHours(1)),
                EstadoTicket.ABIERTO, com.support.ticketservice.support.DomainFixtures.usuarioActivo(),
                null, false, null, null);
        when(tickets.buscarIdsEscalables(AHORA.plus(Duration.ofHours(2)))).thenReturn(List.of(1L));
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticket));
        when(tickets.guardar(ticket)).thenReturn(ticket);
        ejecutarTransacciones();

        int escalados = service.ejecutar();

        assertThat(escalados).isEqualTo(1);
        assertThat(ticket.getPrioridad()).isEqualTo(Prioridad.ALTA);
        assertThat(ticket.isEscalado()).isTrue();
        verify(metrics).ticketEscalado(Prioridad.ALTA);

        ArgumentCaptor<HistorialTicket> captor = ArgumentCaptor.forClass(HistorialTicket.class);
        verify(historial).guardar(captor.capture());
        assertThat(captor.getValue().realizadoPor()).isEqualTo("sistema");
        assertThat(captor.getValue().observacion())
                .contains("SLA vence en 60 min")
                .contains("prioridad MEDIA -> ALTA");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void escalaTicketVencidoYDescribeElMotivo() {
        Ticket ticket = ticketVencido(1L);
        when(tickets.buscarIdsEscalables(any())).thenReturn(List.of(1L));
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticket));
        when(tickets.guardar(ticket)).thenReturn(ticket);
        ejecutarTransacciones();

        assertThat(service.ejecutar()).isEqualTo(1);

        ArgumentCaptor<HistorialTicket> captor = ArgumentCaptor.forClass(HistorialTicket.class);
        verify(historial).guardar(captor.capture());
        assertThat(captor.getValue().observacion()).contains("SLA vencido");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void marcaEscaladoAunqueYaEsteEnPrioridadMaxima() {
        Ticket ticket = new Ticket(1L, "T", "D", Categoria.HARDWARE, Prioridad.CRITICA,
                "BOGOTA", null, AHORA.minus(Duration.ofHours(10)), AHORA.minus(Duration.ofHours(1)),
                EstadoTicket.ABIERTO, com.support.ticketservice.support.DomainFixtures.usuarioActivo(),
                null, false, null, null);
        when(tickets.buscarIdsEscalables(any())).thenReturn(List.of(1L));
        when(tickets.buscarPorId(1L)).thenReturn(Optional.of(ticket));
        when(tickets.guardar(ticket)).thenReturn(ticket);
        ejecutarTransacciones();

        assertThat(service.ejecutar()).isEqualTo(1);
        assertThat(ticket.getPrioridad()).isEqualTo(Prioridad.CRITICA);
        assertThat(ticket.isEscalado()).isTrue();

        ArgumentCaptor<HistorialTicket> captor = ArgumentCaptor.forClass(HistorialTicket.class);
        verify(historial).guardar(captor.capture());
        assertThat(captor.getValue().observacion()).contains("ya estaba en prioridad máxima");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void ignoraIdsQueYaNoExistenOQueNoNecesitanEscalado() {
        Ticket noNecesita = new Ticket(2L, "T", "D", Categoria.RED, Prioridad.BAJA,
                "BOGOTA", null, AHORA, AHORA.plus(Duration.ofHours(10)),
                EstadoTicket.ABIERTO, com.support.ticketservice.support.DomainFixtures.usuarioActivo(),
                null, false, null, null);
        when(tickets.buscarIdsEscalables(any())).thenReturn(List.of(1L, 2L));
        when(tickets.buscarPorId(1L)).thenReturn(Optional.empty());
        when(tickets.buscarPorId(2L)).thenReturn(Optional.of(noNecesita));
        ejecutarTransacciones();

        assertThat(service.ejecutar()).isZero();
        verify(tickets, never()).guardar(any());
        verify(historial, never()).guardar(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void continuaConElSiguienteTicketSiUnoFalla() {
        Ticket ticket = ticketVencido(2L);
        when(tickets.buscarIdsEscalables(any())).thenReturn(List.of(1L, 2L));
        when(tickets.buscarPorId(2L)).thenReturn(Optional.of(ticket));
        when(tickets.guardar(ticket)).thenReturn(ticket);
        when(tx.execute(any()))
                .thenThrow(new RuntimeException("fallo de transacción"))
                .thenAnswer(invocation -> {
                    TransactionCallback<Boolean> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });

        assertThat(service.ejecutar()).isEqualTo(1);
        verify(tickets, times(1)).buscarPorId(2L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void noCuentaTicketsSiLaTransaccionDevuelveFalso() {
        when(tickets.buscarIdsEscalables(any())).thenReturn(List.of(1L, 2L));
        when(tx.execute(any())).thenReturn(false);

        assertThat(service.ejecutar()).isZero();
        verify(tickets, never()).buscarPorId(any());
        verifyNoInteractionsMetrics();
    }

    private void verifyNoInteractionsMetrics() {
        verify(metrics, never()).ticketEscalado(any());
    }
}
