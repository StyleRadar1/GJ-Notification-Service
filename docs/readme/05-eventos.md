[← Volver al índice](../../README.md) · [Todos los README](./)


## <img src="https://img.shields.io/badge/05-5600EF?style=for-the-badge" height="20" align="absmiddle" /> 5. EVENTOS QUE PUBLICA Y CONSUME

La comunicación asíncrona entre microservicios se orquesta a través de **RabbitMQ 3.13** utilizando Topic Exchanges durables y enrutamiento de fallos hacia una cola de errores (Dead Letter Queue) conforme a `SR-RNF-15`.

* **Exchange Principal de Dominio:** `styleradar.exchange` (`topic`, durable)
* **Dead Letter Exchange (DLX):** `styleradar.dlx` (`direct`, durable)
* **Dead Letter Queue (DLQ):** `styleradar.notificaciones.dlq`

### Matriz de Eventos AMQP

| Tipo | Routing Key | Cola (Queue) | Microservicio Emisor / Receptor | Cuándo se Dispara / Acción |
| :---: | :--- | :--- | :--- | :--- |
| **PUBLICA** | `styleradar.notificaciones.alerta.creada` | N/A (Exchange) | `ms-Notifications` &rarr; broker (sin consumidor fijo) | Publica `AlertaCreadaDomainEvent` al persistir una alerta activa (NOT-16). |
| **CONSUME** | `styleradar.inventario.actualizado` | `styleradar.notificaciones.inventario-actualizado.q` | `ms-Inventory` &rarr; `ms-Notifications` | Evalúa reposición de stock y criterios en memoria; genera notificación idempotente por ciclo (NOT-06, NOT-07, NOT-11). |
| **CONSUME** | `styleradar.catalogo.prenda.publicada` | `styleradar.notificaciones.prenda-publicada.q` | `ms-ItemCatalog` &rarr; `ms-Notifications` | Evalúa en memoria si la prenda nueva coincide con búsquedas guardadas activas (NOT-06, NOT-07). |
| **CONSUME** | `styleradar.usuarios.busqueda.sincronizada` | `styleradar.notificaciones.busqueda-guardada.q` | `ms-Users` &rarr; `ms-Notifications` | Mantiene copia local de búsquedas guardadas y desactiva alertas si se elimina (NOT-08). |
| **CONSUME** | `styleradar.usuarios.estado.cambiado` / `user.status.changed` | `styleradar.notificaciones.usuario-estado.q` | `ms-Users` &rarr; `ms-Notifications` | Actualiza copia local del estado de cuenta (`ACTIVO`/`ACTIVA`, `INACTIVO`/`INACTIVA`, `BLOQUEADO`/`BLOQUEADA`, `SUSPENDIDO`) y correo (NOT-09). |
| **CONSUME** | `styleradar.usuarios.cuenta.eliminada` | `styleradar.notificaciones.cuenta-eliminada.q` | `ms-Users` &rarr; `ms-Notifications` | Borra las notificaciones, alertas, preferencias, correos pendientes y búsquedas del usuario, cierra su campana en tiempo real y deja su cuenta marcada `ELIMINADA` sin correo (SR-RF-107). |
| **CONSUME** | `styleradar.chat.mensaje.enviado` | `styleradar.notificaciones.mensaje-enviado.q` | `ms-Chat` &rarr; `ms-Notifications` | Genera notificación `MENSAJE_NUEVO` con enlace a `/chat/conversaciones/{id}` y push SSE (NOT-14). |
| **CONSUME** | `styleradar.donaciones.estado.cambiado` | `styleradar.notificaciones.donacion-cambiada.q` | `ms-Donations` &rarr; `ms-Notifications` | Genera notificación `CAMBIO_DONACION` con enlace a `/donaciones/{id}` (NOT-14). |
| **CONSUME** | `styleradar.catalogo.moderacion.cambiada` | `styleradar.notificaciones.moderacion-cambiada.q` | `ms-ItemCatalog` &rarr; `ms-Notifications` | Genera notificación `MODERACION_PRENDA` y desactiva alertas si la prenda fue retirada (NOT-08, NOT-14). |
| **CONSUME** | `styleradar.catalogo.prenda.retirada` | `styleradar.notificaciones.prenda-retirada.q` | `ms-ItemCatalog` &rarr; `ms-Notifications` | Desactiva las alertas activas de la prenda retirada, borra las notificaciones no leídas de reposición y coincidencia y quita el enlace a las leídas (NOT-08, SR-RF-107). |
| **CONSUME** | `styleradar.donaciones.solicitud.creada` | `styleradar.notificaciones.solicitud-donacion.q` | `ms-Donations` &rarr; `ms-Notifications` | Genera notificación `SOLICITUD_DONACION` para la fundación destinataria (soporta alias `solicitudId` y `representanteId`, SR-RF-106 / GJ-61). |
| **CONSUME** | `styleradar.segundavida.prenda.vendida` / `styleradar.segunda-mano.prenda.vendida` | `styleradar.notificaciones.prenda-vendida.q` | `ms-Donations` &rarr; `ms-Notifications` | Genera notificación `PRENDA_VENDIDA` para cada interesado, excluyendo al comprador y al vendedor (SR-RF-106 / GJ-65). |
| **CONSUME** | `styleradar.pagos.pago.confirmado` | `styleradar.notificaciones.pago-confirmado.q` | `ms-Payment` &rarr; `ms-Notifications` | Genera notificación `PAGO_CONFIRMADO`; si el usuario no existe en la copia local, el mensaje va a la DLQ (SR-RF-106 / GJ-65). |
| **CONSUME** | `styleradar.tiendas.fundacion.desactivada` / `foundation.deactivated` | `styleradar.notificaciones.fundacion-desactivada.q` | `ms-Stores` &rarr; `ms-Notifications` | Genera notificación `CAMBIO_DONACION` para donantes afectados y/o representante cuando una fundación aliada se desactiva (SR-RF-106 / GJ-117). |
| **CONSUME** | `styleradar.tiendas.fundacion.reactivada` / `foundation.reactivated` | `styleradar.notificaciones.fundacion-reactivada.q` | `ms-Stores` &rarr; `ms-Notifications` | Genera notificación `CAMBIO_DONACION` para el representante cuando su fundación aliada vuelve a quedar activa (integración `ms-Stores` v2). |

> **Routing keys:** los nombres están acordados con los equipos emisores (matriz de contrastación en la sección 4 de [docs/CONTRATOS_EVENTOS_NOT21.md](../CONTRATOS_EVENTOS_NOT21.md)). Si alguno cambia, solo hay que editar `application.messaging.routing-keys`. La especificación de cada payload está en [docs/CONTRATOS_EVENTOS_NOT21.md](../CONTRATOS_EVENTOS_NOT21.md). Todos los consumidores son idempotentes por `eventoId`.

### Conexiones de ms-Notifications con otros servicios

Resumen de con quién se comunica el servicio y por qué canal. Es la fuente única que deben seguir los diagramas y los contratos.

| Servicio o sistema | Canal | Sentido | Detalle |
| :--- | :--- | :---: | :--- |
| `ms-Inventory` | RabbitMQ | Entra | `styleradar.inventario.actualizado` |
| `ms-ItemCatalog` | RabbitMQ | Entra | `styleradar.catalogo.prenda.publicada`, `.prenda.retirada` y `.moderacion.cambiada` |
| `ms-Users` | RabbitMQ | Entra | `styleradar.usuarios.estado.cambiado` y `styleradar.usuarios.busqueda.sincronizada` |
| `ms-Chat` | RabbitMQ | Entra | `styleradar.chat.mensaje.enviado` |
| `ms-Donations` | RabbitMQ | Entra | `styleradar.donaciones.estado.cambiado`, `.solicitud.creada` y `styleradar.segundavida.prenda.vendida` |
| `ms-Payment` | RabbitMQ | Entra | `styleradar.pagos.pago.confirmado` |
| `ms-Stores` | RabbitMQ | Entra | `styleradar.tiendas.fundacion.desactivada` |
| `ms-Statistics` | RabbitMQ | Sale | `styleradar.notificaciones.alerta.creada` |
| `ms-ItemCatalog` | HTTP (OpenFeign) | Sale | Comprueba que la prenda siga agotada al crear una alerta |
| `ms-Auth` | Clave JWT compartida | No hay llamada | `ms-Auth` firma el token y este servicio valida la firma con `JWT_SECRET` |
| Frontend | REST con JWT y SSE | Entra y sale | Alertas, bandeja y campana en tiempo real |
| Servidor SMTP | SMTP | Sale | Correos opcionales |
| PostgreSQL | JDBC | Sale | Base propia `notificaciones_db` |

Todos los eventos de entrada usan el exchange `styleradar.exchange` y las 13 colas del servicio. Redis no interviene.

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
