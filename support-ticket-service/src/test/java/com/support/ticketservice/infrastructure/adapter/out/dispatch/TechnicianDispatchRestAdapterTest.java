package com.support.ticketservice.infrastructure.adapter.out.dispatch;

import com.support.ticketservice.domain.model.SugerenciaDespacho;
import com.support.ticketservice.domain.model.enums.Categoria;
import com.support.ticketservice.domain.model.enums.Disponibilidad;
import com.support.ticketservice.domain.model.enums.Prioridad;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TechnicianDispatchRestAdapterTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private TechnicianDispatchRestAdapter adapter;

    private void preparar() {
        builder = RestClient.builder().baseUrl("http://dispatch");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new TechnicianDispatchRestAdapter(builder.build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void devuelveSugerenciaCuandoLaRespuestaEsValida() {
        preparar();
        server.expect(requestTo("http://dispatch/technician-dispatch"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"ticketId":1,"categoria":"HARDWARE","prioridad":"ALTA"}
                        """))
                .andRespond(withSuccess("""
                        {"ticketId":1,"tecnicoId":"TEC-001",
                         "disponibilidad":"ALTA","tiempoEstimadoHoras":2}
                        """, MediaType.APPLICATION_JSON));

        Optional<SugerenciaDespacho> resultado =
                adapter.consultar(1L, Categoria.HARDWARE, Prioridad.ALTA);

        assertThat(resultado).contains(new SugerenciaDespacho("TEC-001", Disponibilidad.ALTA, 2));
        server.verify();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void devuelveVacioCuandoLaRespuestaEstaIncompleta() {
        preparar();
        server.expect(requestTo("http://dispatch/technician-dispatch"))
                .andRespond(withSuccess("""
                        {"ticketId":1}
                        """, MediaType.APPLICATION_JSON));

        Optional<SugerenciaDespacho> resultado =
                adapter.consultar(1L, Categoria.HARDWARE, Prioridad.ALTA);

        assertThat(resultado).isEmpty();
        server.verify();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void devuelveVacioCuandoElServicioRespondeError() {
        preparar();
        server.expect(requestTo("http://dispatch/technician-dispatch"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        Optional<SugerenciaDespacho> resultado =
                adapter.consultar(1L, Categoria.RED, Prioridad.MEDIA);

        assertThat(resultado).isEmpty();
        server.verify();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void devuelveVacioCuandoElCuerpoEsNulo() {
        preparar();
        server.expect(requestTo("http://dispatch/technician-dispatch"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThat(adapter.consultar(1L, Categoria.RED, Prioridad.MEDIA)).isEmpty();
    }
}
