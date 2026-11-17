-- V3__add_evento_id_notificaciones.sql
-- Idempotencia de los consumidores de eventos: una notificacion por (evento, usuario).
-- evento_id es opcional (las notificaciones de alertas no lo usan) y la restriccion unica admite varios NULL.
ALTER TABLE notificaciones ADD COLUMN evento_id VARCHAR(64);
ALTER TABLE notificaciones ADD CONSTRAINT uk_notificacion_evento_usuario UNIQUE (evento_id, usuario_id);
