package com.support.ticketservice.infrastructure.adapter.in.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 20) String documento,
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Email @Size(max = 150) String correo,
        @NotBlank @Size(max = 60) String sede,
        @NotBlank @Size(max = 80) String area,
        @NotBlank @Size(min = 8, max = 72) String password) {
}
