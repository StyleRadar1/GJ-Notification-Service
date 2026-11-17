-- GJ-64 / SR-RF-28: canal elegido por el usuario para cada tipo de aviso.
-- Sin fila para un (usuario, tipo) se aplica "solo plataforma".
CREATE TABLE preferencias_avisos (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    canal VARCHAR(30) NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL,
    CONSTRAINT uk_preferencia_usuario_tipo UNIQUE (usuario_id, tipo)
);
