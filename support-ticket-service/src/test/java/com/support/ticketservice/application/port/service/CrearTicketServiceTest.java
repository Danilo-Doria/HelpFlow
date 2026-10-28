package com.support.ticketservice.application.port.service;

import com.support.ticketservice.application.port.in.CrearTicketUseCase.CrearTicketCommand;
import com.support.ticketservice.application.port.out.AsignacionRepositoryPort;
import com.support.ticketservice.application.port.out.HistorialRepositoryPort;
import com.support.ticketservice.application.port.out.SlaConfigPort;
import com.support.ticketservice.application.port.out.TecnicoRepositoryPort;
import com.support.ticketservice.application.port.out.TechnicianDispatchPort;
import com.support.ticketservice.application.port.out.TicketMetricsPort;
import com.support.ticketservice.application.port.out.TicketRepositoryPort;
import com.support.ticketservice.application.port.out.UsuarioRepositoryPort;
import com.support.ticketservice.domain.exception.RecursoNoEncontradoException;
import com.support.ticketservice.domain.exception.UsuarioInactivoException;
import com.support.ticketservice.domain.model.Asignacion;
import com.support.ticketservice.domain.model.HistorialTicket;
import com.support.ticketservice.domain.model.SugerenciaDespacho;
import com.support.ticketservice.domain.model.TecnicoConCarga;
import com.support.ticketservice.domain.model.Ticket;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.Disponibilidad;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.domain.model.enums.Prioridad;
import com.support.ticketservice.domain.service.TecnicoSelector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static com.support.ticketservice.support.DomainFixtures.AHORA;
import static com.support.ticketservice.support.DomainFixtures.sla;
import static com.support.ticketservice.support.DomainFixtures.tecnicoActivo;
import static com.support.ticketservice.support.DomainFixtures.usuarioActivo;
import static com.support.ticketservice.support.DomainFixtures.usuarioInactivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearTicketServiceTest {

    @Mock private UsuarioRepositoryPort usuarios;
    @Mock private TecnicoRepositoryPort tecnicos;
    @Mock private TicketRepositoryPort tickets;
    @Mock private HistorialRepositoryPort historial;
    @Mock private AsignacionRepositoryPort asignaciones;
    @Mock private SlaConfigPort slaConfig;
    @Mock private TechnicianDispatchPort dispatch;
    @Mock private TicketMetricsPort metrics;
    @Mock private Clock clock;
    @Spy private TecnicoSelector selector = new TecnicoSelector();

    @InjectMocks private CrearTicketService service;

    private CrearTicketCommand comando() {
        return new CrearTicketCommand(1L, "No enciende", "El equipo no enciende",
                Categoria.HARDWARE, Prioridad.ALTA, "BOGOTA", "PC-01");
    }

    private void prepararGuardado() {
        when(tickets.guardar(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId(1L);
            }
            return ticket;
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void creaTicketYLoAsignaCuandoHaySugerenciaYCandidato() {
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(slaConfig.obtener()).thenReturn(sla());
        when(clock.instant()).thenReturn(AHORA);
        prepararGuardado();
        when(dispatch.consultar(1L, Categoria.HARDWARE, Prioridad.ALTA))
                .thenReturn(Optional.of(new SugerenciaDespacho(
                        "TEC-010", Disponibilidad.ALTA, 2)));
        when(tecnicos.buscarCandidatos(Categoria.HARDWARE))
                .thenReturn(List.of(new TecnicoConCarga(
                        tecnicoActivo(10L, Categoria.HARDWARE), 1)));

        Ticket resultado = service.ejecutar(comando(), "ana@helpflow.com");

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTicket.ASIGNADO);
        assertThat(resultado.getTecnicoAsignado().id()).isEqualTo(10L);
        assertThat(resultado.getPrioridad()).isEqualTo(Prioridad.ALTA);

        ArgumentCaptor<Asignacion> asignacion = ArgumentCaptor.forClass(Asignacion.class);
        verify(asignaciones).guardar(asignacion.capture());
        assertThat(asignacion.getValue().tecnicoId()).isEqualTo(10L);
        assertThat(asignacion.getValue().tecnicoSugeridoExterno()).isEqualTo("TEC-010");
        assertThat(asignacion.getValue().disponibilidad()).isEqualTo(Disponibilidad.ALTA);
        assertThat(asignacion.getValue().activa()).isTrue();

        verify(historial, org.mockito.Mockito.times(2)).guardar(any(HistorialTicket.class));
        verify(metrics).ticketCreado(Categoria.HARDWARE, Prioridad.ALTA);
        verify(metrics, never()).ticketEscalado(any());
        verify(tickets, org.mockito.Mockito.times(2)).guardar(any(Ticket.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void dejaElTicketAbiertoCuandoElDespachoNoResponde() {
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(slaConfig.obtener()).thenReturn(sla());
        when(clock.instant()).thenReturn(AHORA);
        prepararGuardado();
        when(dispatch.consultar(1L, Categoria.HARDWARE, Prioridad.ALTA))
                .thenReturn(Optional.empty());

        Ticket resultado = service.ejecutar(comando(), "ana@helpflow.com");

        assertThat(resultado.getEstado()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(resultado.getTecnicoAsignado()).isNull();
        verify(metrics).ticketCreado(Categoria.HARDWARE, Prioridad.ALTA);
        verifyNoInteractions(tecnicos, asignaciones);
        verify(historial).guardar(any(HistorialTicket.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void dejaElTicketAbiertoCuandoNoHayTecnicoElegible() {
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(slaConfig.obtener()).thenReturn(sla());
        when(clock.instant()).thenReturn(AHORA);
        prepararGuardado();
        when(dispatch.consultar(1L, Categoria.HARDWARE, Prioridad.ALTA))
                .thenReturn(Optional.of(new SugerenciaDespacho(
                        "TEC-010", Disponibilidad.ALTA, 2)));
        when(tecnicos.buscarCandidatos(Categoria.HARDWARE)).thenReturn(List.of());

        Ticket resultado = service.ejecutar(comando(), "ana@helpflow.com");

        assertThat(resultado.getEstado()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(resultado.getTecnicoAsignado()).isNull();
        verify(asignaciones, never()).guardar(any());
        verify(metrics).ticketCreado(Categoria.HARDWARE, Prioridad.ALTA);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void escalaElTicketCuandoElTiempoEstimadoSuperaElSlaRestante() {
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(usuarioActivo()));
        when(slaConfig.obtener()).thenReturn(sla());
        when(clock.instant()).thenReturn(AHORA);
        prepararGuardado();
        when(dispatch.consultar(1L, Categoria.HARDWARE, Prioridad.ALTA))
                .thenReturn(Optional.of(new SugerenciaDespacho(
                        "TEC-010", Disponibilidad.BAJA, 20)));
        when(tecnicos.buscarCandidatos(Categoria.HARDWARE))
                .thenReturn(List.of(new TecnicoConCarga(
                        tecnicoActivo(10L, Categoria.HARDWARE), 0)));

        Ticket resultado = service.ejecutar(comando(), "ana@helpflow.com");

        assertThat(resultado.getEstado()).isEqualTo(EstadoTicket.ASIGNADO);
        assertThat(resultado.getPrioridad()).isEqualTo(Prioridad.CRITICA);
        assertThat(resultado.isEscalado()).isTrue();
        verify(metrics).ticketEscalado(Prioridad.CRITICA);

        ArgumentCaptor<HistorialTicket> captor = ArgumentCaptor.forClass(HistorialTicket.class);
        verify(historial, org.mockito.Mockito.times(2)).guardar(captor.capture());
        assertThat(captor.getAllValues().get(1).observacion())
                .contains("Asignado a Tecnico 10")
                .contains("Escalado");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void fallaCuandoElUsuarioNoExiste() {
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ejecutar(comando(), "ana@helpflow.com"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Usuario no encontrado");

        verifyNoInteractions(tickets, historial, asignaciones);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void fallaYNoPersisteCuandoElUsuarioEstaInactivo() {
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(usuarioInactivo()));
        when(clock.instant()).thenReturn(AHORA);
        when(slaConfig.obtener()).thenReturn(sla());

        assertThatThrownBy(() -> service.ejecutar(comando(), "ana@helpflow.com"))
                .isInstanceOf(UsuarioInactivoException.class);

        verify(tickets, never()).guardar(any());
        verifyNoInteractions(historial, asignaciones, metrics);
    }
}
