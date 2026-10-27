package com.support.ticketservice.infrastructure.adapter.out.persistance.entity;

import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoTicket;
import com.support.ticketservice.domain.model.enums.Prioridad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class TicketEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    private Prioridad prioridad;

    private String sede;
    private String activoAfectado;
    private Instant fechaCreacion;
    private Instant fechaLimiteSla;

    @Enumerated(EnumType.STRING)
    private EstadoTicket estado;

    private boolean escalado;
    private Instant fechaPrimeraRespuesta;
    private Instant fechaResolucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private TecnicoEntity tecnico;

    @OneToMany(mappedBy = "ticket")
    @OrderBy("fecha ASC")
    private List<HistorialTicketEntity> historiales = new ArrayList<>();
}
