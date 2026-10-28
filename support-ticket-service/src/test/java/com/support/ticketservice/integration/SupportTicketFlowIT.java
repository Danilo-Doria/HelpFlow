package com.support.ticketservice.integration;

import com.support.ticketservice.TestcontainersConfiguration;
import com.support.ticketservice.domain.model.Credencial;
import com.support.ticketservice.domain.model.enums.Rol;
import com.support.ticketservice.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static com.support.ticketservice.support.DomainFixtures.SECRET;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.jwt.secret=" + SECRET,
        "app.admin.username=admin@helpflow.com",
        "app.admin.password=Admin12345*",
        "app.dispatch.base-url=http://127.0.0.1:1",
        "app.dispatch.timeout-ms=200",
        "app.escalation.initial-delay-ms=3600000",
        "app.escalation.interval-ms=3600000"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class SupportTicketFlowIT {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    private static final String USER_EMAIL = "ana.flow@helpflow.com";
    private static final String USER_PASSWORD = "Secreta123";

    private MvcResult registrarUsuario(String documento, String correo) throws Exception {
        return mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documento":"%s","nombre":"Ana Flow","correo":"%s",
                                 "sede":"BOGOTA","area":"TI","password":"%s"}
                                """.formatted(documento, correo, USER_PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("token").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private long crearUsuarioYLogin() throws Exception {
        MvcResult registro = registrarUsuario("900100", USER_EMAIL);
        return json(registro).get("id").asLong();
    }

    private long crearTicket(String userToken) throws Exception {
        MvcResult result = mvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"No enciende el equipo","descripcion":"Pantalla negra",
                                 "categoria":"HARDWARE","prioridad":"ALTA","sede":"BOGOTA",
                                 "activoAfectado":"PC-001"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andReturn();
        return json(result).get("id").asLong();
    }

    private long crearTecnico(String adminToken) throws Exception {
        MvcResult result = mvc.perform(post("/tecnicos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documento":"T-900","nombre":"Tecnico Flow","salario":3500000,
                                 "fechaAfiliacion":"2020-01-01","especialidad":"HARDWARE",
                                 "sede":"BOGOTA","cargaMaxima":5,
                                 "username":"tecnico.flow@helpflow.com","password":"Secreta123"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asLong();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void flujoCompletoLoginSolicitudEvaluacionYCierre() throws Exception {
        crearUsuarioYLogin();
        String userToken = login(USER_EMAIL, USER_PASSWORD);
        long ticketId = crearTicket(userToken);
        mvc.perform(get("/tickets").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        String adminToken = login("admin@helpflow.com", "Admin12345*");
        long tecnicoId = crearTecnico(adminToken);

        mvc.perform(put("/tickets/{id}/asignacion", ticketId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tecnicoId":%d,"observacion":"Asignación E2E"}
                                """.formatted(tecnicoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ASIGNADO"))
                .andExpect(jsonPath("$.tecnicoId").value(tecnicoId));

        String tecnicoToken = login("tecnico.flow@helpflow.com", "Secreta123");

        mvc.perform(patch("/tickets/{id}/estado", ticketId)
                        .header("Authorization", "Bearer " + tecnicoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"EN_PROGRESO","observacion":"Diagnóstico iniciado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PROGRESO"))
                .andExpect(jsonPath("$.fechaPrimeraRespuesta").isNotEmpty());

        mvc.perform(patch("/tickets/{id}/estado", ticketId)
                        .header("Authorization", "Bearer " + tecnicoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"RESUELTO","observacion":"Equipo reparado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESUELTO"))
                .andExpect(jsonPath("$.fechaResolucion").isNotEmpty());

        mvc.perform(patch("/tickets/{id}/estado", ticketId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"CERRADO","observacion":"Conforme"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"));

        mvc.perform(get("/tickets/{id}/historial", ticketId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[4].estadoNuevo").value("CERRADO"))
                .andExpect(jsonPath("$[0].estadoNuevo").value("ABIERTO"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void tokenExpiradoYTokenMalformadoDevuelvenProblemDetail401() throws Exception {
        String expirado = new JwtService(SECRET, -1).generar(new Credencial(
                1L, "ana@helpflow.com", "hash", Rol.ROLE_USUARIO, true, 1L, null)).token();

        mvc.perform(get("/tickets").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").value("https://helpflow.com/problems/no-autenticado"));

        mvc.perform(get("/tickets").header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void rolSinPermisoRecibe403ConProblemDetail() throws Exception {
        crearUsuarioYLogin();
        String userToken = login(USER_EMAIL, USER_PASSWORD);

        mvc.perform(get("/usuarios").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.type").value("https://helpflow.com/problems/acceso-denegado"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void credencialesInvalidasDevuelvenProblemDetail401() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"nadie@helpflow.com","password":"incorrecta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").value("https://helpflow.com/problems/credenciales-invalidas"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void validacionDeRequestDevuelve400ConDetallePorCampo() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documento":"","nombre":"Ana","correo":"no-es-correo",
                                 "sede":"","area":"","password":"corta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void registroDuplicadoDevuelve409YNoDuplicaDatos() throws Exception {
        crearUsuarioYLogin();

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documento":"900100","nombre":"Otra Ana",
                                 "correo":"otra@helpflow.com","sede":"BOGOTA",
                                 "area":"TI","password":"Secreta123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("https://helpflow.com/problems/duplicado"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void ticketInexistenteDevuelve404() throws Exception {
        String adminToken = login("admin@helpflow.com", "Admin12345*");

        mvc.perform(get("/tickets/{id}", 999999)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("https://helpflow.com/problems/recurso-no-encontrado"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void usuarioNoPuedeVerTicketDeOtroUsuario() throws Exception {
        crearUsuarioYLogin();
        String tokenAna = login(USER_EMAIL, USER_PASSWORD);
        long ticketId = crearTicket(tokenAna);

        registrarUsuario("900200", "luis.flow@helpflow.com");
        String tokenLuis = login("luis.flow@helpflow.com", USER_PASSWORD);

        mvc.perform(get("/tickets/{id}", ticketId)
                        .header("Authorization", "Bearer " + tokenLuis))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.type").value("https://helpflow.com/problems/operacion-no-permitida"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void adminGestionaUsuariosTecnicosYSla() throws Exception {
        long usuarioId = crearUsuarioYLogin();
        String adminToken = login("admin@helpflow.com", "Admin12345*");

        mvc.perform(get("/usuarios").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correo").value(USER_EMAIL));

        mvc.perform(get("/usuarios/{id}", usuarioId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuarioId));

        mvc.perform(put("/usuarios/{id}", usuarioId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana Actualizada","sede":"MEDELLIN","area":"SOPORTE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Actualizada"))
                .andExpect(jsonPath("$.correo").value(USER_EMAIL));

        mvc.perform(patch("/usuarios/{id}/estado", usuarioId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"INACTIVO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));

        long tecnicoId = crearTecnico(adminToken);

        mvc.perform(get("/tecnicos").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(tecnicoId)).isNotEmpty());

        mvc.perform(get("/tecnicos/{id}", tecnicoId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.especialidad").value("HARDWARE"));

        mvc.perform(put("/tecnicos/{id}", tecnicoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Tecnico Actualizado","salario":4000000,
                                 "especialidad":"RED","sede":"MEDELLIN","cargaMaxima":8}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Tecnico Actualizado"))
                .andExpect(jsonPath("$.especialidad").value("RED"));

        mvc.perform(patch("/tecnicos/{id}/estado", tecnicoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"INACTIVO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));

        mvc.perform(get("/sla").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].prioridad").value("CRITICA"))
                .andExpect(jsonPath("$[0].horas").value(4));

        mvc.perform(put("/sla/ALTA")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"horas":6}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].prioridad").value("ALTA"))
                .andExpect(jsonPath("$[1].horas").value(6));

        mvc.perform(post("/tickets/escalamiento")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escalados").isNumber());
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void actuatorHealthEsPublicoYElTraceIdSePropaga() throws Exception {
        mvc.perform(get("/actuator/health").header("X-Trace-Id", "trace-integracion-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "trace-integracion-123"))
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
