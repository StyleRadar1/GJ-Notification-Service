# Mensajes para acordar las routing keys de los eventos que consume ms-notificaciones (GJ-115)

Tres mensajes, uno por equipo. No llevan credenciales. Las claves de enrutamiento son una propuesta de ms-notificaciones: si el equipo ya publica con otro nombre, el cambio en ms-notificaciones es solo una línea de configuración y el contenido del mensaje no cambia.

---

## Para el equipo de Donaciones

Hola. ms-notificaciones ya consume un evento nuevo de Donaciones para avisar a una fundación cuando le llega una solicitud de donación, y necesitamos acordar el nombre con el que lo publican.

**Propuesta**
- Exchange: `styleradar.exchange` (topic, durable).
- Routing key: `styleradar.donaciones.solicitud.creada`.
- Formato: JSON, con las fechas en ISO-8601.

**Campos del mensaje**

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` | texto | Sí | Identificador del evento. Debe mantenerse igual si reintentan el envío |
| `donacionId` | número | Sí | Id de la donación |
| `fundacionUsuarioId` | número | Sí | Id del usuario de la fundación que recibe el aviso |
| `nombreDonante` | texto | No | Nombre visible de quien dona |
| `resumen` | texto | No | Descripción corta de lo que se dona |
| `fechaEvento` | fecha y hora | No | Cuándo se creó la solicitud |

**Qué hace ms-notificaciones al recibirlo:** crea una notificación para el usuario `fundacionUsuarioId` con el enlace `/donaciones/{donacionId}` y la empuja a su campana en tiempo real. Si falta `fundacionUsuarioId` o `donacionId`, el mensaje va a la cola de errores. Si llega el mismo `eventoId` dos veces, la segunda se descarta.

Ejemplo:

```json
{
  "eventoId": "evt-don-7001",
  "donacionId": 40,
  "fundacionUsuarioId": 620,
  "nombreDonante": "Carlos",
  "resumen": "5 camisas",
  "fechaEvento": "2026-11-12T09:00:00"
}
```

Además, ms-notificaciones ya consume `styleradar.donaciones.estado.cambiado` (cambio de estado de una donación) con el contrato que habíamos acordado. Cuando tengan el nombre confirmado, lo dejamos documentado en `docs/CONTRATOS_EVENTOS_NOT21.md`.

---

## Para el equipo de Segunda Vida

Hola. ms-notificaciones ya consume un evento de venta para avisar a los demás interesados en una prenda de segunda mano de que ya no está disponible, y necesitamos acordar el nombre con el que lo publican.

**Propuesta**
- Exchange: `styleradar.exchange` (topic, durable).
- Routing key: `styleradar.segundavida.prenda.vendida`.
- Formato: JSON, con las fechas en ISO-8601.

**Campos del mensaje**

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` | texto | Sí | Identificador del evento. Debe mantenerse igual si reintentan el envío |
| `publicacionId` | número | Sí | Id de la publicación vendida |
| `nombrePrenda` | texto | No | Nombre de la prenda |
| `compradorId` | número | No | Usuario que compró; no recibe el aviso |
| `interesadosIds` | lista de números | No | Usuarios con conversación abierta sobre la prenda |
| `fechaEvento` | fecha y hora | No | Cuándo se vendió |

**Qué hace ms-notificaciones al recibirlo:** crea una notificación por cada usuario de `interesadosIds`, excepto el comprador, con el enlace `/segunda-mano/{publicacionId}`. Si falta `publicacionId`, el mensaje va a la cola de errores. Si llega el mismo `eventoId` dos veces, la segunda se descarta.

Ejemplo:

```json
{
  "eventoId": "evt-vta-8001",
  "publicacionId": 70,
  "nombrePrenda": "Chaqueta bomber",
  "compradorId": 505,
  "interesadosIds": [501, 502, 505],
  "fechaEvento": "2026-11-12T10:00:00"
}
```

---

## Para el equipo de Pagos

Hola. ms-notificaciones ya consume un evento de pago confirmado para avisar al usuario de que su plan o sus cupos quedaron activos, y necesitamos acordar el nombre con el que lo publican.

**Propuesta**
- Exchange: `styleradar.exchange` (topic, durable).
- Routing key: `styleradar.pagos.pago.confirmado`.
- Formato: JSON, con las fechas en ISO-8601.

**Campos del mensaje**

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` | texto | Sí | Identificador del evento. Debe mantenerse igual si reintentan el envío |
| `pagoId` | número | No | Id del pago |
| `usuarioId` | número | Sí | Usuario que pagó |
| `plan` | texto | No | Plan o producto pagado (por ejemplo, Radar+) |
| `fechaConfirmacion` | fecha y hora | No | Cuándo se confirmó el pago |

**Qué hace ms-notificaciones al recibirlo:** crea una notificación para `usuarioId` con el plan y el enlace `/planes/mi-plan`. Si el usuario no existe todavía en la copia local de cuentas de ms-notificaciones, el mensaje se rechaza y va a la cola de errores para revisarlo, en vez de perderse; por eso conviene que el evento de estado de cuenta del usuario se publique antes de su primer pago. Si falta `usuarioId`, también va a la cola de errores. Si llega el mismo `eventoId` dos veces, la segunda se descarta.

Ejemplo:

```json
{
  "eventoId": "evt-pago-9001",
  "pagoId": 900,
  "usuarioId": 501,
  "plan": "Radar+",
  "fechaConfirmacion": "2026-11-12T11:00:00"
}
```

---

## Cuando contesten

Cada equipo debe confirmar la routing key. Si es distinta, el cambio en ms-notificaciones es `application.messaging.routing-keys` en `application.yml` y la actualización de `docs/CONTRATOS_EVENTOS_NOT21.md`. Ese cambio se hace en un PR pequeño, solo cuando haya una respuesta por escrito.
