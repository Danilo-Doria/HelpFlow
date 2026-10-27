CREATE TABLE historial_ticket
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id       BIGINT       NOT NULL,
    estado_anterior VARCHAR(15),
    estado_nuevo    VARCHAR(15)  NOT NULL,
    tecnico_id      BIGINT,
    observacion     VARCHAR(500),
    realizado_por   VARCHAR(150) NOT NULL,
    fecha           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_historial_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id),
    CONSTRAINT fk_historial_tecnico FOREIGN KEY (tecnico_id) REFERENCES tecnicos (id)
);

CREATE INDEX idx_historial_ticket ON historial_ticket (ticket_id, fecha);

CREATE TABLE asignaciones
(
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id                BIGINT       NOT NULL,
    tecnico_id               BIGINT       NOT NULL,
    tecnico_sugerido_externo VARCHAR(20),
    disponibilidad           VARCHAR(10),
    tiempo_estimado_horas    INTEGER,
    fecha_asignacion         TIMESTAMPTZ  NOT NULL,
    activa                   BOOLEAN      NOT NULL DEFAULT TRUE,
    asignado_por             VARCHAR(150) NOT NULL,
    CONSTRAINT fk_asignaciones_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id),
    CONSTRAINT fk_asignaciones_tecnico FOREIGN KEY (tecnico_id) REFERENCES tecnicos (id),
    CONSTRAINT ck_asignaciones_disp CHECK (disponibilidad IS NULL OR disponibilidad IN ('ALTA', 'MEDIA', 'BAJA'))
);

-- Solo una asignación activa por ticket
CREATE UNIQUE INDEX uk_asignaciones_activa ON asignaciones (ticket_id) WHERE activa;
CREATE INDEX idx_asignaciones_tecnico ON asignaciones (tecnico_id);

CREATE TABLE sla_config
(
    prioridad           VARCHAR(10) PRIMARY KEY,
    horas               INTEGER     NOT NULL,
    actualizado_por     VARCHAR(150),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_sla_prioridad CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT ck_sla_horas CHECK (horas > 0)
);

INSERT INTO sla_config (prioridad, horas)
VALUES ('CRITICA', 4),
       ('ALTA', 8),
       ('MEDIA', 24),
       ('BAJA', 72);