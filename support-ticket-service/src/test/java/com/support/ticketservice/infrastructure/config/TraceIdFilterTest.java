package com.support.ticketservice.infrastructure.config;

import jakarta.servlet.FilterChain;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void conservaUnTraceIdValidoYLoDevuelveEnLaRespuesta() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER, "trace-12345678");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Object> traceEnCadena = new AtomicReference<>();
        FilterChain chain = (req, res) -> traceEnCadena.set(MDC.get(TraceIdFilter.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(traceEnCadena.get()).isEqualTo("trace-12345678");
        assertThat(response.getHeader(TraceIdFilter.HEADER)).isEqualTo("trace-12345678");
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void generaTraceIdCuandoElRecibidoEsInvalido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER, "corto");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
        });

        assertThat(response.getHeader(TraceIdFilter.HEADER))
                .matches("[a-f0-9]{32}")
                .isNotEqualTo("corto");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void generaTraceIdCuandoNoVieneHeader() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> {
        });

        assertThat(response.getHeader(TraceIdFilter.HEADER)).matches("[a-f0-9]{32}");
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void limpiaElMdcAunqueLaCadenaFalle() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> filter.doFilter(request, response, chain))
                .isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }
}
