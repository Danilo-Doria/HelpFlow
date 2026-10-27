package com.support.ticketservice.infrastructure.adapter.out.persistence.entity;

import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String documento;
    private String nombre;
    private String correo;
    private String sede;
    private String area;

    @Enumerated(EnumType.STRING)
    private EstadoRegistro estado;

    @OneToMany(mappedBy = "usuario")
    private List<TicketEntity> tickets = new ArrayList<>();
}
