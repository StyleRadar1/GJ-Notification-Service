-- V6__alter_item_id_to_varchar.sql
-- Cambiar item_id de BIGINT a VARCHAR(100) en alertas y notificaciones
-- para soportar identificadores UUID (o texto) emitidos por ms-item-catalogo, ms-inventario y ms-playlist

ALTER TABLE alertas ALTER COLUMN item_id TYPE VARCHAR(100);
ALTER TABLE notificaciones ALTER COLUMN item_id TYPE VARCHAR(100);
