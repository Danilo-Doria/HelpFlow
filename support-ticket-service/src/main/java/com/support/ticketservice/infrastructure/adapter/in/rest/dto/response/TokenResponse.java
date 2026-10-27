package com.support.ticketservice.infrastructure.adapter.in.rest.dto.response;

public record TokenResponse(String token, String tipo, long expiraEnSegundos) {}
