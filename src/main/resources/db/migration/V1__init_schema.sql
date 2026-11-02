-- V1__init_schema.sql
-- Database-per-Service Schema for ms-notificaciones (NOT-04)
-- No foreign keys to external microservices (usuarios, items, busquedas)

CREATE TABLE alertas (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    tipo_alerta VARCHAR(40) NOT NULL,
    item_id BIGINT,
    talla_objetivo VARCHAR(20),
    busqueda_guardada_id BIGINT,
    canal VARCHAR(20) NOT NULL DEFAULT 'IN_APP',
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notificaciones (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    alerta_id BIGINT,
    item_id BIGINT,
    ciclo_reposicion INTEGER,
    tipo VARCHAR(40) NOT NULL,
    canal VARCHAR(20) NOT NULL DEFAULT 'IN_APP',
    titulo VARCHAR(200) NOT NULL,
    mensaje VARCHAR(1000) NOT NULL,
    enlace VARCHAR(300),
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_lectura TIMESTAMP,
    CONSTRAINT uk_notificacion_alerta_item_ciclo UNIQUE (alerta_id, item_id, ciclo_reposicion)
);

CREATE TABLE busquedas_guardadas_copia (
    busqueda_id BIGINT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    texto_libre VARCHAR(250),
    categoria VARCHAR(80),
    color VARCHAR(60),
    talla VARCHAR(20),
    marca VARCHAR(100),
    estilo VARCHAR(80),
    precio_min NUMERIC(12, 2),
    precio_max NUMERIC(12, 2),
    localidad VARCHAR(100),
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_sincronizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE estados_cuenta_copia (
    usuario_id BIGINT PRIMARY KEY,
    email VARCHAR(180),
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
