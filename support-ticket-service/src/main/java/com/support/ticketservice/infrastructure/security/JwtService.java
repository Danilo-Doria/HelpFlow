package com.support.ticketservice.infrastructure.security;

import com.support.ticketservice.application.port.out.TokenGeneratorPort;
import com.support.ticketservice.domain.model.Credencial;
import com.support.ticketservice.domain.model.enums.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtService implements TokenGeneratorPort {

    private final SecretKey key;
    private final long expiracionSegundos;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expiracionMinutos) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // mínimo 32 bytes
        this.expiracionSegundos = expiracionMinutos * 60;
    }

    @Override
    public TokenEmitido generar(Credencial c) {
        Instant ahora = Instant.now();
        String token = Jwts.builder()
                .subject(c.username())
                .claim("rol", c.rol().name())
                .claim("uid", c.usuarioId())
                .claim("tid", c.tecnicoId())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(expiracionSegundos)))
                .signWith(key)
                .compact();
        return new TokenEmitido(token, expiracionSegundos);
    }

    /** Devuelve vacío si el token es inválido, está manipulado o expiró. */
    public Optional<AuthPrincipal> validar(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            return Optional.of(new AuthPrincipal(
                    claims.getSubject(),
                    Rol.valueOf(claims.get("rol", String.class)),
                    claims.get("uid", Long.class),
                    claims.get("tid", Long.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
