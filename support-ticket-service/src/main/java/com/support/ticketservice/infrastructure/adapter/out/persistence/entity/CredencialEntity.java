package com.support.ticketservice.infrastructure.adapter.out.persistence.entity;

import com.support.ticketservice.domain.model.enums.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "credenciales")
@Getter
@Setter
@NoArgsConstructor
public class CredencialEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Rol rol;

    private boolean activo;
    private Long usuarioId;
    private Long tecnicoId;
}
