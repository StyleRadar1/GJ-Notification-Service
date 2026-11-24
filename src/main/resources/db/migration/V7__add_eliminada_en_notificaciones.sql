-- V7__add_eliminada_en_notificaciones.sql
-- Borrado logico de notificaciones: el usuario las elimina de su bandeja pero la fila se conserva para que
-- la restriccion de entregas repetidas (evento_id y alerta+item+ciclo) siga evitando que un evento reentregado
-- la vuelva a crear. Las filas con eliminada_en no se listan ni se cuentan.
ALTER TABLE notificaciones ADD COLUMN eliminada_en TIMESTAMP;

CREATE INDEX idx_notificaciones_usuario_eliminada ON notificaciones (usuario_id, eliminada_en);
