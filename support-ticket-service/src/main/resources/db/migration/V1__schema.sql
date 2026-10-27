CREATE TABLE usuarios
(
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    documento VARCHAR(20)  NOT NULL,
    nombre    VARCHAR(120) NOT NULL,
    correo    VARCHAR(150) NOT NULL,
    sede      VARCHAR(60)  NOT NULL,
    area      VARCHAR(80)  NOT NULL,
    estado    VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT uk_usuarios_documento UNIQUE (documento),
    CONSTRAINT uk_usuarios_correo UNIQUE (correo),
    CONSTRAINT ck_usuarios_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE TABLE tecnicos
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    documento        VARCHAR(20)    NOT NULL,
    nombre           VARCHAR(120)   NOT NULL,
    salario          NUMERIC(12, 2) NOT NULL,
    fecha_afiliacion DATE           NOT NULL,
    estado           VARCHAR(10)    NOT NULL DEFAULT 'ACTIVO',
    especialidad     VARCHAR(20)    NOT NULL,
    sede             VARCHAR(60)    NOT NULL,
    carga_maxima     INTEGER        NOT NULL,
    CONSTRAINT uk_tecnicos_documento UNIQUE (documento),
    CONSTRAINT ck_tecnicos_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_tecnicos_especialidad CHECK (especialidad IN ('HARDWARE', 'SOFTWARE', 'RED', 'ACCESOS', 'OTRO')),
    CONSTRAINT ck_tecnicos_salario CHECK (salario >= 0),
    CONSTRAINT ck_tecnicos_carga CHECK (carga_maxima > 0)
);

CREATE TABLE credenciales
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    rol           VARCHAR(20)  NOT NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    usuario_id    BIGINT,
    tecnico_id    BIGINT,
    CONSTRAINT uk_credenciales_username UNIQUE (username),
    CONSTRAINT uk_credenciales_usuario UNIQUE (usuario_id),
    CONSTRAINT uk_credenciales_tecnico UNIQUE (tecnico_id),
    CONSTRAINT fk_credenciales_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_credenciales_tecnico FOREIGN KEY (tecnico_id) REFERENCES tecnicos (id),
    CONSTRAINT ck_credenciales_rol CHECK (rol IN ('ROLE_USUARIO', 'ROLE_TECNICO', 'ROLE_ADMIN')),
    CONSTRAINT ck_credenciales_vinculo CHECK (
        (rol = 'ROLE_USUARIO' AND usuario_id IS NOT NULL AND tecnico_id IS NULL) OR
        (rol = 'ROLE_TECNICO' AND tecnico_id IS NOT NULL AND usuario_id IS NULL) OR
        (rol = 'ROLE_ADMIN' AND usuario_id IS NULL AND tecnico_id IS NULL)
        )
);

CREATE TABLE tickets
(
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    titulo                  VARCHAR(150) NOT NULL,
    descripcion             TEXT         NOT NULL,
    categoria               VARCHAR(20)  NOT NULL,
    prioridad               VARCHAR(10)  NOT NULL,
    sede                    VARCHAR(60)  NOT NULL,
    activo_afectado         VARCHAR(120),
    fecha_creacion          TIMESTAMPTZ  NOT NULL,
    fecha_limite_sla        TIMESTAMPTZ  NOT NULL,
    estado                  VARCHAR(15)  NOT NULL,
    escalado                BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_primera_respuesta TIMESTAMPTZ,
    fecha_resolucion        TIMESTAMPTZ,
    usuario_id              BIGINT       NOT NULL,
    tecnico_id              BIGINT,
    CONSTRAINT fk_tickets_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_tickets_tecnico FOREIGN KEY (tecnico_id) REFERENCES tecnicos (id),
    CONSTRAINT ck_tickets_categoria CHECK (categoria IN ('HARDWARE', 'SOFTWARE', 'RED', 'ACCESOS', 'OTRO')),
    CONSTRAINT ck_tickets_prioridad CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT ck_tickets_estado
        CHECK (estado IN ('ABIERTO', 'ASIGNADO', 'EN_PROGRESO', 'RESUELTO', 'CERRADO', 'CANCELADO')),
    CONSTRAINT ck_tickets_sla CHECK (fecha_limite_sla > fecha_creacion)
);

CREATE INDEX idx_tickets_usuario ON tickets (usuario_id);
CREATE INDEX idx_tickets_tecnico_est ON tickets (tecnico_id, estado);
CREATE INDEX idx_tickets_estado_sla ON tickets (estado, fecha_limite_sla);
CREATE INDEX idx_tickets_cat_prio ON tickets (categoria, prioridad);