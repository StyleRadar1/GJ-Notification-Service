[← Volver al índice](../../README.md) · [Todos los README](./)


## <img src="https://img.shields.io/badge/05-5600EF?style=for-the-badge" height="20" align="absmiddle" /> 5. EVENTOS QUE PUBLICA Y CONSUME

La comunicación asíncrona entre microservicios se orquesta a través de **RabbitMQ 3.13** utilizando Topic Exchanges durables y enrutamiento de fallos hacia una cola de errores (Dead Letter Queue) conforme a `SR-RNF-15`.

* **Exchange Principal de Dominio:** `styleradar.exchange` (`topic`, durable)
* **Dead Letter Exchange (DLX):** `styleradar.dlx` (`direct`, durable)
* **Dead Letter Queue (DLQ):** `styleradar.notificaciones.dlq`

### Matriz de Eventos AMQP

| Tipo | Routing Key | Cola (Queue) | Microservicio Emisor / Receptor | Cuándo se Dispara / Acción |
| :---: | :--- | :--- | :--- | :--- |
| **PUBLICA** | `styleradar.notificaciones.alerta.creada` | N/A (Exchange) | `ms-notificaciones` &rarr; broker (sin consumidor fijo) | Publica `AlertaCreadaDomainEvent` al persistir una alerta activa (NOT-16). |
| **CONSUME** | `styleradar.inventario.actualizado` | `styleradar.notificaciones.inventario-actualizado.q` | `ms-inventario` &rarr; `ms-notificaciones` | Evalúa reposición de stock y criterios en memoria; genera notificación idempotente por ciclo (NOT-06, NOT-07, NOT-11). |
| **CONSUME** | `styleradar.catalogo.prenda.publicada` | `styleradar.notificaciones.prenda-publicada.q` | `ms-item-catalogo` &rarr; `ms-notificaciones` | Evalúa en memoria si la prenda nueva coincide con búsquedas guardadas activas (NOT-06, NOT-07). |
| **CONSUME** | `styleradar.usuarios.busqueda.sincronizada` | `styleradar.notificaciones.busqueda-guardada.q` | `ms-usuarios` &rarr; `ms-notificaciones` | Mantiene copia local de búsquedas guardadas y desactiva alertas si se elimina (NOT-08). |
| **CONSUME** | `styleradar.usuarios.estado.cambiado` | `styleradar.notificaciones.usuario-estado.q` | `ms-usuarios` &rarr; `ms-notificaciones` | Actualiza copia local del estado de cuenta para no enviar notificaciones a cuentas inactivas (NOT-09). |
| **CONSUME** | `styleradar.chat.mensaje.enviado` | `styleradar.notificaciones.mensaje-enviado.q` | `ms-chat` &rarr; `ms-notificaciones` | Genera notificación `MENSAJE_NUEVO` con enlace a `/chat/conversaciones/{id}` y push SSE (NOT-14). |
| **CONSUME** | `styleradar.donaciones.estado.cambiado` | `styleradar.notificaciones.donacion-cambiada.q` | `ms-segunda-vida-donaciones` &rarr; `ms-notificaciones` | Genera notificación `CAMBIO_DONACION` con enlace a `/donaciones/{id}` (NOT-14). |
| **CONSUME** | `styleradar.catalogo.moderacion.cambiada` | `styleradar.notificaciones.moderacion-cambiada.q` | `ms-item-catalogo` &rarr; `ms-notificaciones` | Genera notificación `MODERACION_PRENDA` y desactiva alertas si la prenda fue retirada (NOT-08, NOT-14). |
| **CONSUME** | `styleradar.catalogo.prenda.retirada` | `styleradar.notificaciones.prenda-retirada.q` | `ms-item-catalogo` &rarr; `ms-notificaciones` | Desactiva las alertas activas de la prenda retirada (NOT-08). |
| **CONSUME** | `styleradar.donaciones.solicitud.creada` | `styleradar.notificaciones.solicitud-donacion.q` | `ms-segunda-vida-donaciones` &rarr; `ms-notificaciones` | Genera notificación `SOLICITUD_DONACION` para la fundación destinataria (soporta alias `solicitudId` y `representanteId`, SR-RF-106 / GJ-61). |
| **CONSUME** | `styleradar.segundavida.prenda.vendida` / `styleradar.segunda-mano.prenda.vendida` | `styleradar.notificaciones.prenda-vendida.q` | `ms-segunda-vida-donaciones` &rarr; `ms-notificaciones` | Genera notificación `PRENDA_VENDIDA` para cada interesado, excluyendo al comprador y al vendedor (SR-RF-106 / GJ-65). |
| **CONSUME** | `styleradar.pagos.pago.confirmado` | `styleradar.notificaciones.pago-confirmado.q` | `ms-pagos` &rarr; `ms-notificaciones` | Genera notificación `PAGO_CONFIRMADO`; si el usuario no existe en la copia local, el mensaje va a la DLQ (SR-RF-106 / GJ-65). |
| **CONSUME** | `styleradar.tiendas.fundacion.desactivada` | `styleradar.notificaciones.fundacion-desactivada.q` | `ms-tiendas` &rarr; `ms-notificaciones` | Genera notificación `CAMBIO_DONACION` para cada donante afectado cuando una fundación aliada se desactiva (SR-RF-106 / GJ-117). |

> **Routing keys:** los nombres están acordados con los equipos emisores (matriz de contrastación en la sección 4 de [docs/CONTRATOS_EVENTOS_NOT21.md](../CONTRATOS_EVENTOS_NOT21.md)). Si alguno cambia, solo hay que editar `application.messaging.routing-keys`. La especificación de cada payload está en [docs/CONTRATOS_EVENTOS_NOT21.md](../CONTRATOS_EVENTOS_NOT21.md). Todos los consumidores son idempotentes por `eventoId`.

<details>
<summary><b>Click para ver Esquema JSON del Evento Publicado (AlertaCreada - NOT-16)</b></summary>

<br>

```json
{
  "eventoId": "evt-alr-1001",
  "alertaId": 75,
  "usuarioId": 501,
  "tipoAlerta": "ITEM_AGOTADO",
  "itemId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "busquedaGuardadaId": null,
  "canal": "IN_APP",
  "fechaCreacion": "2026-11-02T12:30:00"
}
```

</details>
