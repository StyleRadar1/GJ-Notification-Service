-- GJ-119: correos que no se pudieron enviar y se reintentan aunque el servicio se reinicie.
-- cuerpo_html guarda el texto del correo tal como se envia (hoy en texto plano).
CREATE TABLE correos_pendientes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    destinatario VARCHAR(255) NOT NULL,
    asunto VARCHAR(255) NOT NULL,
    cuerpo_html TEXT NOT NULL,
    intentos INT NOT NULL DEFAULT 0,
    max_intentos INT NOT NULL DEFAULT 3,
    estado VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE',
    ultimo_error TEXT,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_correos_pendientes_estado ON correos_pendientes(estado);
