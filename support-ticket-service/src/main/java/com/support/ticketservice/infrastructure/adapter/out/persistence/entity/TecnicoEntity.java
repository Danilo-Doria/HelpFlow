package com.support.ticketservice.infrastructure.adapter.out.persistence.entity;

import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.EstadoRegistro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tecnicos")
@Getter
@Setter
@NoArgsConstructor
public class TecnicoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String documento;
    private String nombre;

    @Column(precision = 12, scale = 2)
    private BigDecimal salario;
    private LocalDate fechaAfiliacion;

    @Enumerated(EnumType.STRING)
    private EstadoRegistro estado;

    @Enumerated(EnumType.STRING)
    private Categoria especialidad;

    private String sede;
    private int cargaMaxima;
}
