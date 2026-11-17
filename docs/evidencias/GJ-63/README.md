# GJ-63 · Publicar el evento AlertaCreada

Esta evidencia sale del CI y no de una captura local, porque la prueba usa un RabbitMQ real con Testcontainers y en la máquina de desarrollo no hay Docker.

- `resultado-ci.txt`: extracto del log de la corrida de `develop`. `AlertaCreadaPublicacionIntegrationTest` ejecutó **2 pruebas, 0 fallos y 0 omitidas**: al crear una alerta llega a una cola enlazada a `styleradar.notificaciones.alerta.creada` en `styleradar.exchange` un mensaje con `eventoId`, `alertaId`, `usuarioId`, `tipoAlerta`, `itemId`, `canal` y `fechaCreacion`; y si la alerta no se crea (prenda con stock, 422) no se publica nada.
- El contrato del evento está en `docs/CONTRATOS_EVENTOS_NOT21.md`, sección 2.1.

## Pendiente

La captura del mensaje dentro del panel de RabbitMQ real de QA no se tomó: requiere acceso de administración al broker. Si el líder la exige, la toma Oscar (o DevOps) en el panel.
