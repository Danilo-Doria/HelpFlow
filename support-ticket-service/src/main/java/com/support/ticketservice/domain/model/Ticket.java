package com.support.ticketservice.domain.model;

import com.support.ticketservice.domain.exception.AsignacionInvalidaException;
import com.support.ticketservice.domain.exception.TransicionInvalidaException;
import com.support.ticketservice.domain.exception.UsuarioInactivoException;
import com.support.ticketservice.domain.service.SlaPolicy;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class Ticket {

    private Long id;
    private final String titulo;
    private final String descripcion;
    private final Categoria categoria;
    private Prioridad prioridad;
    private final String sede;
    private final String activoAfectado;
    private final Instant fechaCreacion;
    private final Instant fechaLimiteSla;
    private EstadoTicket estado;
    private final Usuario solicitante;
    private Tecnico tecnicoAsignado;
    private boolean escalado;
    private Instant fechaPrimeraRespuesta;
    private Instant fechaResolucion;

    public Ticket(Long id, String titulo, String descripcion, Categoria categoria,
                  Prioridad prioridad, String sede, String activoAfectado,
                  Instant fechaCreacion, Instant fechaLimiteSla, EstadoTicket estado,
                  Usuario solicitante, Tecnico tecnicoAsignado, boolean escalado,
                  Instant fechaPrimeraRespuesta, Instant fechaResolucion) {
        this.id = id;
        this.titulo = Objects.requireNonNull(titulo);
        this.descripcion = Objects.requireNonNull(descripcion);
        this.categoria = Objects.requireNonNull(categoria);
        this.prioridad = Objects.requireNonNull(prioridad);
        this.sede = Objects.requireNonNull(sede);
        this.activoAfectado = activoAfectado;
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion);
        this.fechaLimiteSla = Objects.requireNonNull(fechaLimiteSla);
        this.estado = Objects.requireNonNull(estado);
        this.solicitante = Objects.requireNonNull(solicitante);
        this.tecnicoAsignado = tecnicoAsignado;
        this.escalado = escalado;
        this.fechaPrimeraRespuesta = fechaPrimeraRespuesta;
        this.fechaResolucion = fechaResolucion;
        if (!fechaLimiteSla.isAfter(fechaCreacion)) {
            throw new IllegalArgumentException("La fecha límite de SLA debe ser posterior a la creación");
        }
    }

    public static Ticket abrir(String titulo, String descripcion, Categoria categoria,
                               Prioridad prioridad, String sede, String activoAfectado,
                               Usuario solicitante, SlaPolicy sla, Instant ahora) {
        if (solicitante == null || !solicitante.estaActivo()) {
            throw new UsuarioInactivoException("Solo un usuario ACTIVO puede registrar tickets");
        }
        return new Ticket(null, titulo, descripcion, categoria, prioridad, sede,
                activoAfectado, ahora, sla.calcularFechaLimite(prioridad, ahora),
                EstadoTicket.ABIERTO, solicitante, null, false, null, null);
    }


    // Asigna o reasigna los estados (ABIERTO -> ASIGNADO, ASIGNADO -> ASIGNADO).
    public void asignar(Tecnico tecnico) {
        if (!estado.puedePasarA(EstadoTicket.ASIGNADO)) {
            throw new TransicionInvalidaException(estado, EstadoTicket.ASIGNADO);
        }
        if (!tecnico.estaActivo()) {
            throw new AsignacionInvalidaException("El técnico no está habilitado (INACTIVO)");
        }
        if (!tecnico.puedeAtender(categoria)) {
            throw new AsignacionInvalidaException("La especialidad del técnico no coincide con la categoría " + categoria);
        }
        this.tecnicoAsignado = tecnico;
        this.estado = EstadoTicket.ASIGNADO;
    }


    // Resto de las transiciones, ASIGNADO se hace solo con asignar()
    public void cambiarEstado(EstadoTicket nuevo, Instant ahora) {
        if (nuevo == EstadoTicket.ASIGNADO || !estado.puedePasarA(nuevo)) {
            throw new TransicionInvalidaException(estado, nuevo);
        }
        switch (nuevo) {
            case EN_PROGRESO -> {
                if (fechaPrimeraRespuesta == null) fechaPrimeraRespuesta = ahora;
                fechaResolucion = null; // reapertura desde RESUELTO
            }
            case RESUELTO -> fechaResolucion = ahora;
            default -> {
            }
        }
        this.estado = nuevo;
    }

    public boolean estaVencido(Instant ahora) {
        return !estado.esTerminal() && estado != EstadoTicket.RESUELTO
                && ahora.isAfter(fechaLimiteSla);
    }

    public boolean proximoAVencer(Instant ahora, Duration umbral) {
        return !estado.esTerminal() && estado != EstadoTicket.RESUELTO
                && !estaVencido(ahora)
                && Duration.between(ahora, fechaLimiteSla).compareTo(umbral) <= 0;
    }

    // Sube el nivel de prioridad. Solo se escala una vez (secuencial).
    public boolean escalar() {
        if (escalado || estado.esTerminal()) return false;
        prioridad.siguiente().ifPresent(p -> this.prioridad = p);
        this.escalado = true;
        return true;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public String getSede() {
        return sede;
    }

    public String getActivoAfectado() {
        return activoAfectado;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getFechaLimiteSla() {
        return fechaLimiteSla;
    }

    public EstadoTicket getEstado() {
        return estado;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public Tecnico getTecnicoAsignado() {
        return tecnicoAsignado;
    }

    public boolean isEscalado() {
        return escalado;
    }

    public Instant getFechaPrimeraRespuesta() {
        return fechaPrimeraRespuesta;
    }

    public Instant getFechaResolucion() { return fechaResolucion; }

    public void setId(Long id) { this.id = id; }
}
