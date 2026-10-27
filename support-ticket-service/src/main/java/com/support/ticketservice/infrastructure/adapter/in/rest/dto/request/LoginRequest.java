package com.support.ticketservice.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
