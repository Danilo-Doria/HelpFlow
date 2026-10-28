# HelpFlow

Two independent Spring Boot 4.1.2 / Java 21 Maven services. There is no root `pom.xml`: always run Maven from inside the service directory using that service's `./mvnw`.

| Service | Port | Package | Role |
| --- | --- | --- | --- |
| `support-ticket-service` | 8080 | `com.support.ticketservice` | Tickets, users, technicians, SLA, JWT auth, PostgreSQL |
| `technician-dispatch-service` | 8081 | `com.technician.dispatchservice` | Deterministic simulated technician dispatch; no DB |

## Commands

```bash
# From support-ticket-service
./mvnw test                         # unit tests only
./mvnw -Dtest=LoginServiceTest test # single unit test
./mvnw -Dtest=SupportTicketFlowIT test
./mvnw verify                       # unit + integration + JaCoCo check
./mvnw -DskipTests package
./mvnw spring-boot:run

# From technician-dispatch-service
./mvnw test
./mvnw spring-boot:run

# From repo root
docker compose up --build
```

Each Dockerfile expects its **own service directory** as build context; `docker-compose.yaml` is configured that way. Tests require a running Docker daemon: support-ticket integration tests start PostgreSQL via Testcontainers and apply Flyway migrations. `verify` enforces 80% instruction/branch coverage; the report is in `support-ticket-service/target/site/jacoco/index.html`.

There is no lint/formatter/typecheck setup and no CI config; Maven build + tests are the verification.

### support-ticket test layout

- `src/test/java/.../domain/` — pure domain models, policies and enum tests.
- `src/test/java/.../application/port/service/` — Mockito unit tests for every use case.
- `src/test/java/.../infrastructure/` — security, REST adapters, metrics, filters, scheduler, dispatch client and persistence adapter tests.
- `src/test/java/.../integration/SupportTicketFlowIT.java` — MockMvc end-to-end flow over a real Postgres Testcontainer: register → login → create ticket → assign → progress → resolve → close, plus 401/403/404/409/400 cases. It is transactional, so each test rolls back and starts from clean data.
- Test classes use `// ─────` separator comments between test methods; keep that style when adding tests.

## support-ticket-service

- Hexagonal architecture. Keep dependencies pointing inward:
  - `domain/` — pure Java models, policies, exceptions (no Spring/JPA).
  - `application/port/in/` — use-case interfaces; implementations live in `application/port/service/` (yes, despite the name).
  - `application/port/out/` — outbound ports.
  - `infrastructure/adapter/in|out/` — REST, scheduler, JPA, dispatch client, metrics.
- Controllers delegate to use cases; MapStruct mappers convert domain ↔ REST/persistence DTOs (`-Amapstruct.defaultComponentModel=spring` is set in the pom).
- Flyway owns the schema and `ddl-auto=validate`. Add a new `Vn__*.sql` migration for every schema change; never rely on Hibernate to create tables. Seed data is in `V3__datos_iniciales.sql`.
- DB default is `jdbc:postgresql://localhost:5437/helpflow`, but Compose publishes Postgres on **5432**. When running the service locally against `docker compose up db`, set `DB_URL=jdbc:postgresql://localhost:5432/helpflow`.
- Startup creates the admin credential if missing: `admin@gmail.com`; password is `${ADMIN_PASSWORD}` (`admin123` from `application.properties`, `Admin12345*` via Compose). JWT secret minimum is 32 bytes.
- Security: `/auth/**`, `/actuator/health`, `/actuator/info` are public; other `/actuator/**` requires `ADMIN`; everything else requires a Bearer JWT. Roles are `ROLE_USUARIO`, `ROLE_TECNICO`, `ROLE_ADMIN`.
- Errors are RFC 7807 `ProblemDetail` bodies with `traceId`; `X-Trace-Id` is set/propagated by `TraceIdFilter`. Preserve this format when adding endpoints/exceptions.
- Dispatch is best-effort: an unavailable dispatch service is logged and ignored, leaving the ticket `ABIERTO`. That fallback is intentional.
- Escalation scheduler runs every 60s after a 30s delay (`app.escalation.*`).
- Prometheus metrics are exposed at `/actuator/prometheus`; with `SPRING_PROFILES_ACTIVE=json`, logs are structured JSON.

## technician-dispatch-service

- `POST /technician-dispatch` returns a technician code, availability, and estimated hours.
- The result is simulated and deterministic from `ticketId|categoria|prioridad` (20 technicians, 0–12 hours), so retries for the same ticket return the same suggestion. There is no persisted dispatch state or database.
- Do not add support-service domain/internal dependencies here; the two services communicate only over HTTP.

## Conventions

- Domain names, comments, log messages, and error text are intentionally Spanish (`Ticket`, `Tecnico`, `Usuario`, `Asignacion`, `Historial`). Keep naming consistent; do not rename to English.
- Spring Boot 4 uses split starters (for example `spring-boot-starter-webmvc` and per-module `spring-boot-starter-*-test`). Follow the existing poms rather than substituting Boot 3 starter names.
