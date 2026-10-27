package com.support.ticketservice.infrastructure.adapter.in.rest;

import com.support.ticketservice.domain.exception.*;
import com.support.ticketservice.infrastructure.config.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.naming.AuthenticationException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String BASE = "https://helpflow.com/problems/";

    // ---------- Dominio ----------
    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return problema(HttpStatus.NOT_FOUND, "recurso-no-encontrado",
                "Recurso no encontrado", ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicadoException.class)
    ProblemDetail duplicado(DuplicadoException ex, HttpServletRequest req) {
        return problema(HttpStatus.CONFLICT, "duplicado", "Registro duplicado", ex.getMessage(), req);
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    ProblemDetail transicion(TransicionInvalidaException ex, HttpServletRequest req) {
        return problema(HttpStatus.CONFLICT, "transicion-invalida",
                "Transición de estado inválida", ex.getMessage(), req);
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    ProblemDetail noPermitida(OperacionNoPermitidaException ex, HttpServletRequest req) {
        return problema(HttpStatus.FORBIDDEN, "operacion-no-permitida",
                "Operación no permitida", ex.getMessage(), req);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    ProblemDetail credenciales(CredencialesInvalidasException ex, HttpServletRequest req) {
        return problema(HttpStatus.UNAUTHORIZED, "credenciales-invalidas",
                "Credenciales inválidas", ex.getMessage(), req);
    }

    @ExceptionHandler(DomainException.class)   // UsuarioInactivo, AsignacionInvalida, etc.
    ProblemDetail regla(DomainException ex, HttpServletRequest req) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "regla-de-negocio",
                "Regla de negocio incumplida", ex.getMessage(), req);
    }

    // ---------- Seguridad ----------
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
        return problema(HttpStatus.FORBIDDEN, "acceso-denegado", "Acceso denegado",
                "No tienes permisos para realizar esta operación", req);
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail noAutenticado(AuthenticationException ex, HttpServletRequest req) {
        return problema(HttpStatus.UNAUTHORIZED, "no-autenticado", "No autenticado",
                "Token ausente, inválido o expirado", req);
    }

    // ---------- Validación / JPA ----------
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail constraint(ConstraintViolationException ex, HttpServletRequest req) {
        return problema(HttpStatus.BAD_REQUEST, "validacion", "Validación fallida",
                ex.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "integridad-datos", "Conflicto de datos",
                "La operación viola una restricción de la base de datos", req);
    }

    // ---------- Fallback ----------
    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "error-interno", "Error interno",
                "Ocurrió un error inesperado. Reporta el traceId al soporte", req);
    }

    // ---------- Errores propios de Spring MVC ----------
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = ex.getBody();
        pd.setType(URI.create(BASE + "validacion"));
        pd.setTitle("Validación fallida");
        pd.setDetail("La solicitud contiene campos inválidos");
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("campo", e.getField(),
                        "mensaje", String.valueOf(e.getDefaultMessage())))
                .toList());
        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response =
                super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail pd) {
            if (pd.getInstance() == null && request instanceof ServletWebRequest swr) {
                pd.setInstance(URI.create(swr.getRequest().getRequestURI()));
            }
            pd.setProperty("timestamp", Instant.now());
            pd.setProperty("traceId", MDC.get(TraceIdFilter.MDC_KEY));
        }
        return response;
    }

    private ProblemDetail problema(HttpStatus status, String slug, String title,
                                   String detail, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(BASE + slug));
        pd.setTitle(title);
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("traceId", MDC.get(TraceIdFilter.MDC_KEY));
        return pd;
    }
}