-- V2__add_indexes.sql
-- Performance indexes for fast lookup by user, item, status, and saved search

CREATE INDEX idx_alertas_usuario_id ON alertas (usuario_id);
CREATE INDEX idx_alertas_item_estado ON alertas (item_id, estado);
CREATE INDEX idx_alertas_tipo_estado ON alertas (tipo_alerta, estado);
CREATE INDEX idx_alertas_busqueda_estado ON alertas (busqueda_guardada_id, estado);

CREATE INDEX idx_notificaciones_usuario_leida ON notificaciones (usuario_id, leida, fecha_creacion DESC);
CREATE INDEX idx_busquedas_copia_usuario_activa ON busquedas_guardadas_copia (usuario_id, activa);
