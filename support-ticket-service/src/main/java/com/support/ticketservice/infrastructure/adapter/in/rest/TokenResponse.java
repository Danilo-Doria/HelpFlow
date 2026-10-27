package com.support.ticketservice.infrastructure.adapter.in.rest;

public record TokenResponse(String token, String tipo, long expiraEnSegundos) {}
