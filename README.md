# HelpFlow

Sistema de gestión de tickets de soporte técnico compuesto por **dos microservicios independientes** Spring Boot 4.1.2 / Java 21:

- **`support-ticket-service`** — núcleo del negocio: usuarios, técnicos, tickets, SLA, asignaciones, historial, autenticación JWT y persistencia en PostgreSQL.
- **`technician-dispatch-service`** — simulador determinista de disponibilidad técnica; no tiene base de datos ni estado propio.

El proyecto aplica **arquitectura hexagonal** en el servicio principal, contrato HTTP entre servicios, migraciones Flyway, observabilidad con Actuator/Micrometer/Prometheus y una suite de pruebas unitarias y end-to-end.

---

## Arquitectura

```
                         ┌──────────────────────────┐
   Cliente / Swagger     │  support-ticket-service  │
   ───────────────────►  │        :8080             │
                         │                          │
                         │  REST ──► Casos de uso   │
                         │            │             │
                         │            ▼             │
                         │        Dominio puro      │
                         │            │             │
                         │            ▼             │
                         │  Adaptadores de salida   │
                         └───────┬──────────┬───────┘
                                 │          │
                    JDBC/JPA     │          │  HTTP (best-effort)
                                 ▼          ▼
                         ┌────────────┐  ┌───────────────────────────┐
                         │ PostgreSQL │  │ technician-dispatch-service│
                         │   :5432    │  │           :8081            │
                         └────────────┘  └───────────────────────────┘

   EscalationScheduler ──► EscalarTicketsUseCase ──► tickets / historial
```

- `support-ticket-service` expone el puerto **8080**.
- `technician-dispatch-service` expone el puerto **8081**.
- PostgreSQL se publica en el host en el puerto **5437** (contenedor `5432`).
- La comunicación entre servicios es **exclusivamente HTTP**; el despacho es *best-effort*: si el servicio no responde, el ticket se crea y queda `ABIERTO`.

---

## Servicios

| Servicio | Puerto | Paquete base | Responsabilidad |
| --- | ---: | --- | --- |
| `support-ticket-service` | 8080 | `com.support.ticketservice` | Tickets, usuarios, técnicos, SLA, JWT, historial, métricas, PostgreSQL |
| `technician-dispatch-service` | 8081 | `com.technician.dispatchservice` | Sugerencia determinista de técnico, disponibilidad y horas estimadas |

No existe un `pom.xml` raíz. Cada servicio se compila y ejecuta desde su propio directorio con su `./mvnw`.

---

## Stack

| Área | Tecnología |
| --- | --- |
| Lenguaje / runtime | Java 21 |
| Framework | Spring Boot 4.1.2 (starters divididos: `webmvc`, `*-test`, etc.) |
| Persistencia | Spring Data JPA + Hibernate, PostgreSQL 16+ |
| Migraciones | Flyway (`ddl-auto=validate`) |
| Seguridad | Spring Security, JWT (jjwt 0.12.6), BCrypt |
| Mapeo | MapStruct 1.6.3 |
| Boilerplate | Lombok |
| Observabilidad | Spring Boot Actuator, Micrometer, Prometheus, logs JSON |
| Documentación API | springdoc-openapi 3.x (Swagger UI) |
| Pruebas | JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers |
| Cobertura | JaCoCo (mínimo 80% de instrucciones y ramas) |

---

## Reglas de negocio

### Roles

| Rol | Alcance |
| --- | --- |
| `ROLE_USUARIO` | Crea tickets y consulta/cierra/cancela los suyos |
| `ROLE_TECNICO` | Consulta sus tickets asignados y los mueve a `EN_PROGRESO` / `RESUELTO` |
| `ROLE_ADMIN` | Gestiona usuarios, técnicos, SLA, asignaciones, escalamiento y métricas |

Reglas de autorización destacadas:

- Solo un `ROLE_USUARIO` puede crear tickets (`POST /tickets`).
- Un usuario solo ve sus propios tickets; un técnico solo los asignados a él.
- Un técnico no puede cerrar tickets ni cancelarlos.
- Un usuario no puede reasignar tickets ni cambiar a `EN_PROGRESO`.
- `/actuator/metrics` y `/actuator/prometheus` requieren `ROLE_ADMIN`.

### Estados del ticket

```
ABIERTO ──► ASIGNADO ──► EN_PROGRESO ──► RESUELTO ──► CERRADO
   │            │             │              │
   └────────────┴─────────────┴──────────────┴──► CANCELADO

RESUELTO ──► EN_PROGRESO   (reapertura)
ASIGNADO ──► ASIGNADO      (reasignación)
```

| Estado actual | Transiciones permitidas |
| --- | --- |
| `ABIERTO` | `ASIGNADO`, `CANCELADO` |
| `ASIGNADO` | `ASIGNADO`, `EN_PROGRESO`, `CANCELADO` |
| `EN_PROGRESO` | `RESUELTO`, `CANCELADO` |
| `RESUELTO` | `CERRADO`, `EN_PROGRESO` |
| `CERRADO` / `CANCELADO` | Ninguna |

Una transición inválida responde **409 Conflict**. Cada cambio queda registrado en el historial junto con la observación, el actor y la fecha.

### SLA

Valores iniciales (configurables por el administrador):

| Prioridad | Plazo por defecto |
| --- | ---: |
| `CRITICA` | 4 h |
| `ALTA` | 8 h |
| `MEDIA` | 24 h |
| `BAJA` | 72 h |

- La fecha límite se calcula como `fechaCreacion + plazo(prioridad)`.
- La política exige coherencia: una prioridad mayor **no** puede tener un plazo mayor que una menor.
- La API acepta entre 1 y 720 horas por prioridad.
- Al reabrir un ticket (`RESUELTO → EN_PROGRESO`) se limpia `fechaResolucion`; `fechaPrimeraRespuesta` se conserva.

### Asignación automática

Al crear un ticket:

1. Se consulta `technician-dispatch-service` para obtener un técnico sugerido, su disponibilidad y el tiempo estimado.
2. Se buscan candidatos internos que estén `ACTIVO`, tengan la **especialidad** del ticket y capacidad disponible (`ticketsActivos < cargaMaxima`).
3. Se elige el mejor candidato por: **misma sede** → **menor carga activa** → **menor id**.
4. Si el tiempo estimado supera el SLA restante, el ticket se **escala una vez** (sube un nivel de prioridad) y se anota en el historial.
5. Si el despacho no responde o no hay candidatos, el ticket permanece `ABIERTO` y puede asignarse manualmente por un administrador.

La asignación manual valida técnico activo, especialidad, capacidad y estado del ticket, desactiva la asignación anterior y guarda una nueva.

### Escalamiento

- Job programado cada **60 s** (con 30 s de retraso inicial) y umbral de **2 h** antes del vencimiento.
- También se puede disparar manualmente: `POST /tickets/escalamiento` (solo admin).
- Solo escala tickets no terminales, no escalados previamente y cuya fecha límite esté dentro del umbral o ya vencida.
- Sube la prioridad un nivel (`BAJA → MEDIA → ALTA → CRITICA`) y marca `escalado = true`; en `CRITICA` solo marca el flag.
- Registra el motivo en el historial con `realizadoPor = "sistema"`.

### Despacho determinista (`technician-dispatch-service`)

La respuesta depende únicamente de `ticketId|categoria|prioridad`:

```text
seed       = Integer.toUnsignedLong((ticketId|CATEGORIA|PRIORIDAD).hashCode())
tecnico    = "TEC-" + zeroPadded((seed % 20) + 1)     // TEC-001 .. TEC-020
horas      = (seed / 20) % 13                          // 0 .. 12
disponib.  = horas <= 2 ? ALTA : horas <= 8 ? MEDIA : BAJA
```

La misma combinación siempre devuelve el mismo resultado; no hay estado persistido.

---

## Puesta en marcha

### Requisitos

- Docker Engine 24+ con Compose v2
- (Opcional, desarrollo local) Java 21 — el wrapper de Maven descarga Maven automáticamente
- (Opcional, smoke test) `curl`, `jq`

### Con Docker Compose (recomendado)

```bash
# Desde la raíz del repositorio
docker compose up --build -d

# Ver estado y salud
docker compose ps
docker compose logs -f support-ticket-service
```

Servicios disponibles:

| Recurso | URL |
| --- | --- |
| API de soporte | http://localhost:8080 |
| Swagger soporte | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health soporte | http://localhost:8080/actuator/health |
| API de despacho | http://localhost:8081 |
| Swagger despacho | http://localhost:8081/swagger-ui.html |
| Health despacho | http://localhost:8081/actuator/health |
| PostgreSQL | `localhost:5437` (BD `helpflow`, usuario/clave `helpflow`) |

Credenciales iniciales del administrador (creadas al arrancar si no existen):

```text
usuario:  admin@gmail.com
password: admin123        # ADMIN_PASSWORD en Docker Compose
```

> En `application.properties` local el usuario es `admin@gmail.com` y la clave `${ADMIN_PASSWORD:admin123}`. En Compose también se usa `admin123` por defecto. Cámbiala en entornos reales.

Para reiniciar con datos limpios:

```bash
docker compose down -v
docker compose up --build -d
```

### Desarrollo local

```bash
# Solo la base de datos
docker compose up -d db

# En otra terminal
cd support-ticket-service
DB_URL=jdbc:postgresql://localhost:5437/helpflow ./mvnw spring-boot:run
```

El servicio de despacho puede ejecutarse aparte:

```bash
cd technician-dispatch-service
./mvnw spring-boot:run
```

La URL por defecto del despacho es `http://localhost:8081`; se puede cambiar con `DISPATCH_URL`.

### Variables de entorno

| Variable | Servicio | Valor por defecto | Descripción |
| --- | --- | --- | --- |
| `DB_URL` | support | `jdbc:postgresql://localhost:5437/helpflow` | URL JDBC |
| `DB_USER` | support | `helpflow` | Usuario de BD |
| `DB_PASSWORD` | support | `helpflow` | Clave de BD |
| `JWT_SECRET` | support | clave de ejemplo | Secreto HMAC, **mínimo 32 bytes** |
| `ADMIN_PASSWORD` | support | `admin123` | Clave del admin inicial |
| `DISPATCH_URL` | support | `http://localhost:8081` | URL de `technician-dispatch-service` |
| `SPRING_PROFILES_ACTIVE` | support | — | `json` activa logs estructurados |
| `API` / `DISPATCH` | smoke test | `http://localhost:8080` / `:8081` | Endpoints a verificar |
| `ADMIN_USER` / `ADMIN_PASS` | smoke test | `admin@gmail.com` / `admin123` | Credenciales usadas por el script |

Propiedades internas relevantes (en `support-ticket-service/src/main/resources/application.properties`):

```properties
app.jwt.expiration-minutes=60
app.dispatch.timeout-ms=3000
app.escalation.threshold=2h
app.escalation.interval-ms=60000
app.escalation.initial-delay-ms=30000
```

---

## API

La documentación interactiva está en Swagger UI. Todas las rutas salvo las públicas requieren:

```http
Authorization: Bearer <token>
```

### Endpoints

| Método | Ruta | Rol | Descripción |
| --- | --- | --- | --- |
| `POST` | `/auth/register` | público | Registra un usuario (`ROLE_USUARIO`) |
| `POST` | `/auth/login` | público | Devuelve un JWT |
| `POST` | `/tickets` | `USUARIO` | Crea un ticket y dispara la asignación automática |
| `GET` | `/tickets` | autenticado | Lista según el rol (todos / propios / asignados) |
| `GET` | `/tickets/{id}` | dueño, técnico asignado o admin | Consulta un ticket |
| `GET` | `/tickets/{id}/historial` | dueño, técnico asignado o admin | Historial de cambios |
| `PATCH` | `/tickets/{id}/estado` | según rol y estado | Cambia de estado con observación obligatoria |
| `PUT` | `/tickets/{id}/asignacion` | `ADMIN` | Asigna o reasigna un técnico |
| `POST` | `/tickets/escalamiento` | `ADMIN` | Ejecuta el escalamiento manualmente |
| `GET` | `/usuarios` | `ADMIN` | Lista usuarios |
| `GET` | `/usuarios/{id}` | `ADMIN` | Consulta usuario |
| `PUT` | `/usuarios/{id}` | `ADMIN` | Actualiza nombre, sede y área |
| `PATCH` | `/usuarios/{id}/estado` | `ADMIN` | Activa/inactiva usuario |
| `POST` | `/tecnicos` | `ADMIN` | Crea técnico y su credencial |
| `GET` | `/tecnicos` | `ADMIN` | Lista técnicos |
| `GET` | `/tecnicos/{id}` | `ADMIN` | Consulta técnico |
| `PUT` | `/tecnicos/{id}` | `ADMIN` | Actualiza datos del técnico |
| `PATCH` | `/tecnicos/{id}/estado` | `ADMIN` | Activa/inactiva técnico |
| `GET` | `/sla` | `ADMIN` | Consulta la política SLA |
| `PUT` | `/sla/{prioridad}` | `ADMIN` | Actualiza las horas de una prioridad |
| `GET` | `/actuator/health`, `/actuator/info` | público | Salud e información |
| `GET` | `/actuator/metrics`, `/actuator/prometheus` | `ADMIN` | Métricas |

### Ejemplo de flujo

```bash
API=http://localhost:8080

# 1) Registro
curl -s -X POST $API/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "documento":"900123",
    "nombre":"Ana Pérez",
    "correo":"ana@helpflow.com",
    "sede":"BOGOTA",
    "area":"TI",
    "password":"Secreta123"
  }'

# 2) Login
TOKEN=$(curl -s -X POST $API/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"ana@helpflow.com","password":"Secreta123"}' | jq -r .token)

# 3) Crear ticket (asignación automática si el despacho responde)
curl -s -X POST $API/tickets \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "titulo":"No enciende el equipo",
    "descripcion":"Pantalla negra al encender",
    "categoria":"HARDWARE",
    "prioridad":"ALTA",
    "sede":"BOGOTA",
    "activoAfectado":"PC-001"
  }'

# 4) Login del administrador
ADMIN=$(curl -s -X POST $API/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin@gmail.com","password":"admin123"}' | jq -r .token)

# 5) Asignar manualmente (id de ticket y de técnico)
curl -s -X PUT $API/tickets/1/asignacion \
  -H "Authorization: Bearer $ADMIN" \
  -H 'Content-Type: application/json' \
  -d '{"tecnicoId":3,"observacion":"Asignación manual"}'

# 6) Técnico: ASIGNADO → EN_PROGRESO → RESUELTO
TEC=$(curl -s -X POST $API/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"tecnico@helpflow.com","password":"Secreta123"}' | jq -r .token)

curl -s -X PATCH $API/tickets/1/estado \
  -H "Authorization: Bearer $TEC" \
  -H 'Content-Type: application/json' \
  -d '{"estado":"EN_PROGRESO","observacion":"Diagnóstico iniciado"}'

curl -s -X PATCH $API/tickets/1/estado \
  -H "Authorization: Bearer $TEC" \
  -H 'Content-Type: application/json' \
  -d '{"estado":"RESUELTO","observacion":"Equipo reparado"}'

# 7) Usuario: RESUELTO → CERRADO
curl -s -X PATCH $API/tickets/1/estado \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"estado":"CERRADO","observacion":"Conforme"}'

# 8) Historial
curl -s $API/tickets/1/historial -H "Authorization: Bearer $TOKEN"
```

Respuesta de login:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEnSegundos": 3600
}
```

Respuesta de ticket:

```json
{
  "id": 1,
  "titulo": "No enciende el equipo",
  "descripcion": "Pantalla negra al encender",
  "categoria": "HARDWARE",
  "prioridad": "ALTA",
  "sede": "BOGOTA",
  "activoAfectado": "PC-001",
  "fechaCreacion": "2026-10-27T20:00:00Z",
  "fechaLimiteSla": "2026-10-28T04:00:00Z",
  "estado": "ASIGNADO",
  "escalado": false,
  "usuarioId": 1,
  "tecnicoId": 3,
  "tecnicoNombre": "Andrés Leon",
  "fechaPrimeraRespuesta": null,
  "fechaResolucion": null
}
```

### Despacho

```bash
curl -s -X POST http://localhost:8081/technician-dispatch \
  -H 'Content-Type: application/json' \
  -d '{"ticketId":1,"categoria":"RED","prioridad":"ALTA"}'
```

```json
{
  "ticketId": 1,
  "tecnicoId": "TEC-008",
  "disponibilidad": "ALTA",
  "tiempoEstimadoHoras": 0
}
```

---

## Manejo de errores (RFC 7807)

Todos los errores usan `ProblemDetail` con `type`, `title`, `status`, `detail`, `instance`, `timestamp` y `traceId`:

```json
{
  "type": "https://helpflow.com/problems/credenciales-invalidas",
  "title": "Credenciales inválidas",
  "status": 401,
  "detail": "Usuario o contraseña incorrectos",
  "instance": "/auth/login",
  "timestamp": "2026-10-27T20:00:00Z",
  "traceId": "prueba-trace-0001"
}
```

| HTTP | Tipo (`/problems/...`) | Causa |
| ---: | --- | --- |
| 400 | `validacion` | Campos inválidos, JSON mal formado, id no numérico |
| 401 | `credenciales-invalidas`, `no-autenticado` | Login fallido, token ausente/inválido/expirado |
| 403 | `acceso-denegado`, `operacion-no-permitida` | Rol insuficiente o ticket ajeno |
| 404 | `recurso-no-encontrado` | Ticket, usuario o técnico inexistente |
| 409 | `duplicado`, `transicion-invalida`, `integridad-datos` | Duplicados y transiciones no permitidas |
| 422 | `regla-de-negocio` | Usuario/técnico inactivo, especialidad, carga máxima, SLA incoherente |
| 500 | `error-interno` | Error no controlado |

Los errores de validación incluyen además:

```json
{
  "errors": [
    { "campo": "correo", "mensaje": "must be a well-formed email address" }
  ]
}
```

### Trazabilidad

- `TraceIdFilter` lee `X-Trace-Id` (8–64 caracteres alfanuméricos o `-`), o genera uno de 32 caracteres hexadecimales.
- El identificador se devuelve en el header `X-Trace-Id`, se incluye en los logs y en el `ProblemDetail`.
- Los jobs de escalamiento usan un traceId con prefijo `job-`.

---

## Observabilidad

| Endpoint | Acceso |
| --- | --- |
| `/actuator/health` | público |
| `/actuator/info` | público |
| `/actuator/metrics` | `ADMIN` |
| `/actuator/prometheus` | `ADMIN` |

Métricas expuestas:

| Métrica Prometheus | Tipo | Descripción |
| --- | --- | --- |
| `helpflow_tickets_opened_total` | contador | Tickets creados por categoría y prioridad |
| `helpflow_tickets_first_response_seconds` | timer | Creación → primer `EN_PROGRESO` |
| `helpflow_tickets_resolution_seconds` | timer | Creación → `RESUELTO` |
| `helpflow_tickets_escalated_total` | contador | Escalamientos por prioridad resultante |
| `helpflow_tickets_vencidos` | gauge | Tickets activos vencidos según SLA |
| `helpflow_auth_failures_total` | contador | Fallos de login o de token |
| `helpflow_access_denied_total` | contador | Accesos denegados por rol o propiedad |

Logs:

```bash
# Modo legible con traceId
./mvnw spring-boot:run

# Modo JSON (una línea por evento, campo traceId)
SPRING_PROFILES_ACTIVE=json ./mvnw spring-boot:run
```

---

## Base de datos

Flyway es la única fuente del esquema; Hibernate solo valida (`ddl-auto=validate`). Cada cambio de esquema requiere una nueva migración `Vn__*.sql` en `support-ticket-service/src/main/resources/db/migration/`.

| Migración | Contenido |
| --- | --- |
| `V1__schema.sql` | `usuarios`, `tecnicos`, `credenciales`, `tickets` e índices |
| `V2__asignaciones_historial_sla.sql` | `historial_ticket`, `asignaciones`, `sla_config` y SLA inicial |
| `V3__datos_iniciales.sql` | 5 técnicos de ejemplo |

Tablas principales:

- `usuarios` — datos del solicitante y estado (`ACTIVO` / `INACTIVO`).
- `tecnicos` — especialidad, sede, salario, carga máxima y estado.
- `credenciales` — `username`, hash BCrypt, rol y vínculo a usuario o técnico.
- `tickets` — estado, prioridad, fechas de SLA, primera respuesta y resolución.
- `historial_ticket` — auditoría de cada transición.
- `asignaciones` — asignación activa (índice único parcial) y datos del despacho.
- `sla_config` — horas por prioridad.

---

## Pruebas y calidad

### Comandos

Ejecutar desde `support-ticket-service/`:

```bash
./mvnw test                          # pruebas unitarias
./mvnw -Dtest=LoginServiceTest test  # una clase unitaria
./mvnw -Dtest=SupportTicketFlowIT test
./mvnw verify                        # unitarias + integración + JaCoCo
./mvnw -DskipTests package
```

En `technician-dispatch-service/`:

```bash
./mvnw test
./mvnw spring-boot:run
```

> Las pruebas de integración usan **Testcontainers** y requieren un Docker daemon activo. Levantan un PostgreSQL temporal y aplican las migraciones Flyway. `mvn test` no ejecuta los `*IT`; `mvn verify` sí.

### Cobertura

`verify` exige **80% de instrucciones y 80% de ramas** y genera el reporte en:

```text
support-ticket-service/target/site/jacoco/index.html
```

Resultado de la última ejecución verificada:

| Métrica | Cobertura |
| --- | ---: |
| Instrucciones | 97,18% (5230/5382) |
| Ramas | 86,99% (214/246) |
| Líneas | 95,80% (958/1000) |
| Complejidad | 90,34% |
| Pruebas | 157 unitarias + 10 end-to-end |

### Organización de las pruebas

```text
src/test/java/com/support/ticketservice/
├── domain/
│   ├── model/                 # Ticket, Usuario, Tecnico, Actor…
│   ├── model/enums/           # Transiciones y prioridades
│   └── service/               # SlaPolicy, TecnicoSelector, TicketAccessPolicy
├── application/port/service/  # Casos de uso con Mockito
├── infrastructure/
│   ├── security/              # JWT, BCrypt
│   ├── config/                # TraceIdFilter, AdminInitializer, MetricsConfig
│   ├── adapter/in/scheduler/  # EscalationScheduler
│   └── adapter/out/           # Persistencia, despacho y métricas
├── integration/
│   └── SupportTicketFlowIT.java
└── support/
    └── DomainFixtures.java
```

Características de la suite:

- Casos felices, errores y límites en cada clase.
- Mockito con `@Mock`, `@Spy` sobre políticas reales, `ArgumentCaptor`, `verifyNoInteractions` y stubs que simulan fallos.
- Tests de integración con `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Transactional` (rollback por test).
- Flujo E2E: registro → login → ticket → asignación → progreso → resolución → cierre → historial.
- Cobertura explícita de **token expirado**, 400, 401, 403, 404, 409 y reglas de negocio 422.
- Convención: comentario separador `// ─────` entre cada test.

### Smoke test de punta a punta

`smoke-test.sh` valida el sistema completo contra contenedores en ejecución:

```bash
# Recomendado: base de datos limpia
docker compose down -v
docker compose up --build -d

./smoke-test.sh | tee resultado.txt
```

Cubre 13 secciones: salud, autenticación, CRUD de usuarios/técnicos, tickets y asignación automática, reasignación, ciclo de vida, cancelación, carga máxima, SLA, escalamiento, resiliencia con el despacho caído, métricas y `ProblemDetail`.

Variables opcionales: `API`, `DISPATCH`, `ADMIN_USER`, `ADMIN_PASS`, `DB_CONTAINER`, `DISPATCH_CONTAINER`, `SKIP_DOCKER=1` (omite las secciones que inspeccionan la BD o detienen contenedores).

El script guarda las métricas recolectadas en `evidencia-metricas.txt`. Si se renombran métricas o cambian las reglas de despacho, actualiza las aserciones correspondientes.

---

## Estructura del repositorio

```text
HelpFlow/
├── docker-compose.yaml            # PostgreSQL + ambos servicios
├── .dockerignore
├── AGENTS.md                      # Guía para agentes/colaboradores
├── README.md
├── smoke-test.sh                  # Verificación E2E por HTTP
├── resultado.txt                  # Salida de la última ejecución del smoke test
├── evidencia-metricas.txt         # Métricas helpflow_* recolectadas
├── support-ticket-service/
│   ├── Dockerfile                 # Build multietapa (Maven → JRE alpine)
│   ├── pom.xml                    # JaCoCo + Failsafe
│   └── src/
│       ├── main/java/.../
│       │   ├── domain/            # Modelos, políticas, enums, excepciones
│       │   ├── application/       # Puertos de entrada/salida y servicios
│       │   └── infrastructure/    # REST, seguridad, JPA, despacho, métricas
│       ├── main/resources/
│       │   ├── application.properties
│       │   ├── application-json.properties
│       │   └── db/migration/
│       └── test/java/.../
└── technician-dispatch-service/
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/technician/dispatchservice/
        ├── controller/            # POST /technician-dispatch
        ├── service/               # Cálculo determinista
        ├── dto/                   # Request/Response
        └── config/                # OpenAPI
```

### Arquitectura hexagonal en `support-ticket-service`

| Carpeta | Contenido | Regla de dependencia |
| --- | --- | --- |
| `domain/` | `Ticket`, `Usuario`, `Tecnico`, `SlaPolicy`, `TicketAccessPolicy` | Java puro, sin Spring/JPA |
| `application/port/in/` | Interfaces de casos de uso | Dependen del dominio |
| `application/port/out/` | Puertos de repositorio, hasher, token, métricas, despacho | Dependen del dominio |
| `application/port/service/` | Implementación de casos de uso | Orquestan puertos |
| `infrastructure/adapter/in/` | Controladores REST, scheduler | Entrada |
| `infrastructure/adapter/out/` | JPA, REST client, Micrometer | Salida |

Las dependencias siempre apuntan hacia el dominio. Los controladores delegan en casos de uso y los mappers MapStruct convierten dominio ↔ REST/persistencia.

---

## Decisiones de diseño

- **Dominio rico**: las reglas de estados, SLA, asignación y permisos viven en el dominio y se prueban sin Spring.
- **Despacho best-effort**: una caída de `technician-dispatch-service` no bloquea la creación del ticket; queda `ABIERTO` para asignación manual.
- **Despacho determinista**: mismas entradas → misma sugerencia, lo que facilita pruebas y reintentos idempotentes.
- **Asignación en dos pasos**: la sugerencia externa aporta disponibilidad/estimación, pero la elegibilidad final la decide el dominio interno.
- **Auditoría completa**: todo cambio de estado, asignación y escalamiento se registra en `historial_ticket`.
- **Errores uniformes**: `ProblemDetail` con `traceId` en todas las respuestas de error.
- **Migraciones versionadas**: el esquema nunca lo crea Hibernate.
- **Pruebas con datos limpios**: rollback transaccional en integración y mocks aislados en unitarias.

---

## Solución de problemas

| Síntoma | Causa probable | Solución |
| --- | --- | --- |
| `WeakKeyException` al arrancar | `JWT_SECRET` menor a 32 bytes | Usa un secreto de al menos 32 caracteres |
| `Connection refused` a PostgreSQL | URL/puerto incorrecto | Con Compose usa `localhost:5437`; el contenedor es `5432` |
| `Flyway validate failed` | Esquema modificado sin migración | Crea `Vn__*.sql` o recrea la BD con `docker compose down -v` |
| Los tests de integración fallan | Docker no está corriendo | Inicia Docker y vuelve a ejecutar `./mvnw verify` |
| Tickets quedan `ABIERTO` | Despacho caído o sin técnicos elegibles | Revisa `docker compose logs technician-dispatch-service`; asigna manualmente |
| `403` al consultar métricas | El token no es de admin | Inicia sesión con el usuario administrador |
| `401` con token válido | Token expirado (60 min por defecto) | Vuelve a hacer login |
| El smoke test falla en métricas | Nombres de métricas desactualizados | Compara con `/actuator/prometheus` y ajusta `smoke-test.sh` |

---

## Documentación adicional

- `AGENTS.md` — comandos, convenciones, arquitectura y organización de pruebas.
- `smoke-test.sh` — suite HTTP de extremo a extremo.
- Swagger UI de cada servicio para contratos detallados.
