# Contratos de Eventos Asíncronos — Microservicio de Notificaciones (NOT-21)

**Servicio:** `ms-Notifications` (Puerto `8300`)  
**Broker:** RabbitMQ (`styleradar.exchange` — Tipo `topic`, Dead Letter Exchange `styleradar.dlx`)  
**Acuerdos de Integración:** Catálogo (`ms-Inventory`, `ms-ItemCatalog`), Identidad (`ms-Users`), Mensajes (`ms-Chat`), Donaciones (`ms-Donations`) y Estadísticas (`ms-Statistics`).

---

## 1. Eventos Consumidos por `ms-Notifications`

### 1.1 `InventarioActualizado` (Emisor: `ms-Inventory`)
- **Routing Key:** `styleradar.inventario.actualizado`
- **Cola Destino:** `styleradar.notificaciones.inventario-actualizado.q`
- **Propósito:** Notificar una reposición de stock o cambio de unidades por prenda/talla/sede. Se notifica cuando la prenda se repone (`stockAnterior == 0` o `stockAnterior == null` según backlog RES-133, y `stockActual > 0`); cualquier otro cambio de stock se ignora. El `cicloReposicion` preserva la restricción de idempotencia `(alerta_id, item_id, ciclo_reposicion)`.

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` | `String (UUID)` | Sí | Identificador único del evento |
| `itemId` | `String (UUID / texto)` | Sí | ID de la prenda o ítem de catálogo (UUID estándar o numérico como texto) |
| `nombrePrenda` | `String` | Sí | Nombre descriptivo de la prenda |
| `categoria` | `String` | Sí | Categoría comercial (ej. `CHAQUETAS`, `CALZADO`) |
| `color` | `String` | No | Color principal de la prenda |
| `talla` | `String` | Sí | Talla repuesta (ej. `M`, `40`) |
| `marca` | `String` | No | Marca o diseñador |
| `estilo` | `String` | No | Arquetipo o estilo (`STREETWEAR`, `SASTRE`, etc.) |
| `precio` | `BigDecimal` | Sí | Precio unitario vigente |
| `stockAnterior` | `Integer` | No | Stock antes de la actualización (opcional por backlog RES-133; si es `null` y `stockActual > 0`, se procesa) |
| `stockActual` | `Integer` | Sí | Nuevo stock disponible (con `> 0` dispara la evaluación de alertas) |
| `cicloReposicion` | `Integer` | Sí | Número secuencial de ciclo de reposición del ítem |
| `tiendaId` | `String (UUID / texto)` | Sí | ID de la tienda aliada (UUID estándar o numérico) |
| `localidad` | `String` | No | Localidad o zona geográfica de la sede en Bogotá |
| `fechaEvento` | `LocalDateTime` | Sí | Marca de tiempo ISO-8601 |

**Ejemplo JSON:**
```json
{
  "eventoId": "evt-inv-90210",
  "itemId": 1045,
  "nombrePrenda": "Chaqueta Bomber Oversize Negra",
  "categoria": "CHAQUETAS",
  "color": "NEGRO",
  "talla": "M",
  "marca": "BogotaStreet",
  "estilo": "STREETWEAR",
  "precio": 185000.00,
  "stockAnterior": 0,
  "stockActual": 8,
  "cicloReposicion": 2,
  "tiendaId": 12,
  "localidad": "CHAPINERO",
  "fechaEvento": "2026-11-02T11:30:00"
}
```

---

### 1.2 `PrendaPublicada` (Emisor: `ms-ItemCatalog`)
- **Routing Key:** `styleradar.catalogo.prenda.publicada`
- **Cola Destino:** `styleradar.notificaciones.prenda-publicada.q`
- **Propósito:** Evaluar en memoria si una prenda recién publicada coincide con las alertas de tipo `BUSQUEDA_GUARDADA` activas.

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` | `String (UUID)` | Sí | Identificador único del evento |
| `itemId` | `String (UUID / texto)` | Sí | ID de la nueva prenda (UUID estándar o numérico) |
| `nombrePrenda` | `String` | Sí | Nombre de la prenda |
| `categoria` | `String` | Sí | Categoría comercial |
| `color` | `String` | No | Color principal |
| `tallasDisponibles` | `List<String>` | Sí | Lista de tallas con stock inicial `> 0` |
| `marca` | `String` | No | Marca o diseñador |
| `estilo` | `String` | No | Arquetipo o estilo |
| `precio` | `BigDecimal` | Sí | Precio de publicación |
| `stockTotal` | `Integer` | Sí | Stock inicial total |
| `tiendaId` | `String (UUID / texto)` | Sí | ID de la tienda (UUID o numérico) |
| `localidad` | `String` | No | Localidad de la sede en Bogotá |
| `fechaPublicacion` | `LocalDateTime` | Sí | Fecha y hora de publicación |

**Ejemplo JSON:**
```json
{
  "eventoId": "evt-cat-30112",
  "itemId": 2099,
  "nombrePrenda": "Blazer Lino Sastre",
  "categoria": "SASTRERIA",
  "color": "BEIGE",
  "tallasDisponibles": ["S", "M"],
  "marca": "AtelierAndino",
  "estilo": "SASTRE",
  "precio": 320000.00,
  "stockTotal": 5,
  "tiendaId": 7,
  "localidad": "USAQUEN",
  "fechaPublicacion": "2026-11-02T12:00:00"
}
```

---

### 1.3 `UsuarioEstadoCambiado` (Emisor: `ms-Users`)
- **Routing Keys:** `styleradar.usuarios.estado.cambiado` y alias unificado v2 `user.status.changed` (la misma cola recibe ambas)
- **Cola Destino:** `styleradar.notificaciones.usuario-estado.q`
- **Propósito:** Mantener actualizada la copia local del estado de cuenta (`ACTIVO`, `INACTIVO`, `BLOQUEADO`, `SUSPENDIDO`; acepta equivalentes en femenino `ACTIVA`, `INACTIVA`, `BLOQUEADA`, `SUSPENDIDA` e inglés) para impedir que cuentas inactivas, bloqueadas o suspendidas creen alertas o reciban notificaciones (SR-RF-29, SR-RF-31, SR-RF-106 / NOT-09).

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` (alias `eventId`) | `String` | No | Identificador del evento |
| `usuarioId` (alias `userId`) | `Long` | Sí | ID del usuario |
| `email` | `String` | No | Correo electrónico del usuario |
| `nuevoEstado` (alias `estadoNuevo`, `newStatus`) | `String` | Sí | `ACTIVO`/`ACTIVA`/`ACTIVE`, `INACTIVO`/`INACTIVA`/`INACTIVE`, `BLOQUEADO`/`BLOQUEADA`/`BLOCKED` o `SUSPENDIDO`/`SUSPENDIDA`/`SUSPENDED` |
| `fechaCambio` (alias `changedAt`) | `LocalDateTime` | No | Fecha del cambio de estado (acepta ISO-8601 con o sin sufijo `Z`) |

**Ejemplo JSON (v2 en inglés):**
```json
{
  "eventId": "evt-usr-7741",
  "userId": 501,
  "email": "mateo@styleradar.co",
  "previousStatus": "ACTIVE",
  "newStatus": "INACTIVE",
  "changedBy": 1,
  "reason": "Incumplimiento de terminos",
  "changedAt": "2026-11-02T12:15:00Z"
}
```
*(También se siguen aceptando los campos en español de v1 y campos informativos adicionales como `previousStatus`, `changedBy` y `reason`.)*

---

### 1.4 `MensajeEnviado` (Emisor: `ms-Chat`)
- **Routing Key:** `styleradar.chat.mensaje.enviado`
- **Cola Destino:** `styleradar.notificaciones.mensaje-enviado.q`
- **Propósito:** Generar una notificación en tiempo real (`TIPO: MENSAJE_NUEVO`) con enlace directo a la conversación (`/chat/conversaciones/{conversacionId}`) (NOT-14).

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` | `String` | Sí | Identificador del evento |
| `mensajeId` | `Long` | Sí | ID del mensaje |
| `conversacionId` | `Long` | Sí | ID de la conversación |
| `remitenteId` | `Long` | Sí | ID del usuario que envía |
| `nombreRemitente` | `String` | Sí | Nombre visible del remitente |
| `destinatarioId` | `Long` | Sí | ID del usuario receptor |
| `resumenContenido` | `String` | Sí | Fragmento del mensaje |
| `fechaEnvio` | `LocalDateTime` | Sí | Fecha y hora del mensaje |

**Ejemplo JSON:**
```json
{
  "eventoId": "evt-msg-8812",
  "mensajeId": 9901,
  "conversacionId": 340,
  "remitenteId": 412,
  "nombreRemitente": "Nora Vintage",
  "destinatarioId": 501,
  "resumenContenido": "Hola, todavia tienes disponible la chaqueta en talla M?",
  "fechaEnvio": "2026-11-02T12:20:00"
}
```

---

### 1.5 Eventos Complementarios de Sincronización, Donación y Moderación (NOT-08, NOT-14)
- **`BusquedaGuardadaSincronizada` (`styleradar.usuarios.busqueda.sincronizada`):** Crea, actualiza o elimina la copia local de una búsqueda guardada (`busquedas_guardadas_copia`). Si se elimina (`accion = "ELIMINADA"`), desactiva automáticamente las alertas asociadas.
- **`DonacionEstadoCambiado` (`styleradar.donaciones.estado.cambiado`):** Genera notificación de tipo `CAMBIO_DONACION` con enlace `/donaciones/{donacionId}`.
- **`ModeracionPrendaCambiada` (`styleradar.catalogo.moderacion.cambiada`):** Genera notificación de tipo `MODERACION_PRENDA` con enlace `/catalogo/mis-prendas/{itemId}`.

### 1.6 `PrendaRetirada` (Emisor: `ms-ItemCatalog`)
- **Routing Key:** `styleradar.catalogo.prenda.retirada`
- **Cola Destino:** `styleradar.notificaciones.prenda-retirada.q`
- **Propósito:** Desactivar las alertas activas de una prenda que el catálogo retiró o eliminó, para que no queden alertas sobre un ítem que ya no existe.

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` | `String` | Sí | Identificador del evento |
| `itemId` | `String (UUID / texto)` | Sí | ID de la prenda retirada (UUID o numérico) |
| `motivo` | `String` | No | Motivo del retiro (eliminada por el dueño, moderación, etc.) |
| `fechaEvento` | `LocalDateTime` | Sí | Fecha y hora del retiro |

**Ejemplo JSON:**
```json
{
  "eventoId": "evt-cat-30188",
  "itemId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "motivo": "ELIMINADA_POR_DUENO",
  "fechaEvento": "2026-11-02T13:00:00"
}
```

### 1.7 `SolicitudDonacionCreada` (Emisor: `ms-Donations` / Donaciones) — GJ-61
> **Contrato Oficial Verificado** con la Ficha Técnica de Integración de `ms-Donations` (Portal 8080 · Juan David Munar).
- **Routing Key:** `styleradar.donaciones.solicitud.creada`
- **Cola Destino:** `styleradar.notificaciones.solicitud-donacion.q`
- **Propósito:** Avisar a la fundación destinataria de que le llegó una solicitud de donación. Genera una notificación `SOLICITUD_DONACION` con enlace `/donaciones/{donacionId}`.

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` | `String` | Sí | Identificador del evento (clave de idempotencia) |
| `donacionId` (alias `solicitudId`) | `Long` | Sí | ID de la donación o de la solicitud |
| `fundacionUsuarioId` (alias `representanteId`) | `Long` | Sí | ID de usuario en ms-Auth del representante de la fundación que recibe el aviso. No es el `fundacionId` de ms-Stores |
| `nombreDonante` | `String` | No | Nombre visible de quien dona |
| `resumen` | `String` | No | Descripción corta de lo que se dona |
| `fechaEvento` | `LocalDateTime` | No | Fecha y hora de la solicitud |

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

### 1.8 `PrendaVendida` (Emisor: `ms-Donations`) — GJ-65
> **Contrato Oficial Verificado** con las Fichas Técnicas de Integración de `ms-Donations` y `ms-Chat` (Pág. 4).
- **Routing Key:** `styleradar.segundavida.prenda.vendida` y `styleradar.segunda-mano.prenda.vendida` (la cola está vinculada a las dos; Donaciones publica la segunda). `interesadosIds` lo resuelve quien publica; vacío no avisa a nadie. No se avisa al comprador ni al vendedor (`vendedorId`).
- **Cola Destino:** `styleradar.notificaciones.prenda-vendida.q`
- **Propósito:** Avisar a los demás interesados en una prenda de segunda mano de que ya se vendió. Se genera una notificación `PRENDA_VENDIDA` por cada interesado, excepto el comprador, con enlace `/segunda-mano/{publicacionId}`.

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` | `String` | Sí | Identificador del evento (clave de idempotencia) |
| `publicacionId` | `String (UUID / texto)` | Sí | ID de la publicación vendida (UUID o numérico) |
| `nombrePrenda` | `String` | No | Nombre de la prenda |
| `compradorId` | `Long` | No | Usuario que compró (no recibe el aviso) |
| `vendedorId` | `Long` | No | Usuario que vendió (no recibe el aviso; si falta se ignora sin error) |
| `interesadosIds` | `List<Long>` | No | Usuarios con conversación abierta sobre la prenda |
| `fechaEvento` | `LocalDateTime` | No | Fecha y hora de la venta |

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

### 1.9 `PagoConfirmado` (Emisor: `ms-Payment`) — GJ-65
> **Contrato Oficial Verificado** con la Ficha Técnica de Integración de `ms-Payment` (Webhook Mercado Pago).
- **Routing Key:** `styleradar.pagos.pago.confirmado` (con binding a `pago.confirmado`)
- **Cola Destino:** `styleradar.notificaciones.pago-confirmado.q`
- **Propósito:** Confirmar al usuario que su pago (plan Radar+ o cupos de almacén aliado) quedó aprobado. Genera una notificación `PAGO_CONFIRMADO` con enlace `/planes/mi-plan`.
- **Usuario desconocido:** si `usuarioId` no existe en la copia local de cuentas (`estados_cuenta_copia`), el mensaje se rechaza y va a la DLQ para revisarlo, en vez de perderse.

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` | `String` | Sí | Identificador del evento (clave de idempotencia) |
| `pagoId` | `Long` | No | ID del pago |
| `usuarioId` | `Long` | Sí | Usuario que pagó |
| `plan` | `String` | No | Plan o producto pagado |
| `fechaConfirmacion` | `LocalDateTime` | No | Fecha y hora de la confirmación |

```json
{
  "eventoId": "evt-pago-9001",
  "pagoId": 900,
  "usuarioId": 501,
  "plan": "Radar+",
  "fechaConfirmacion": "2026-11-12T11:00:00"
}
```

### 1.10 Mensajes de la donación según el estado
`DonacionEstadoCambiado` genera un texto distinto según `nuevoEstado`: contiene `APROB` → "fue aprobada"; `RECHAZ` → "fue rechazada"; `CITA` → "se programó una cita"; cualquier otro → "cambió a estado X". Si viene `motivo`, se agrega al final.

### 1.11 `FundacionDesactivada` (Emisor: `ms-Stores`) — GJ-117
> **Contrato Oficial Verificado** con la Ficha Técnica de Integración de `ms-Stores` (`ms-Stores.pdf` · Pág. 6 y 18) y esquema v2 CADCHE.
- **Routing Keys:** `styleradar.tiendas.fundacion.desactivada` y `foundation.deactivated` (la cola está vinculada a ambas en `styleradar.exchange`)
- **Cola Destino:** `styleradar.notificaciones.fundacion-desactivada.q`
- **Exchange:** `styleradar.exchange` (`topic`, durable)
- **Propósito:** Avisar a los donantes afectados (`donantesAfectadosIds`) y/o al representante de la fundación (`representanteId`/`representativeId`) cuando una fundación aliada es desactivada administrativamente (`activa: false`). Genera notificación `CAMBIO_DONACION` con enlace `/donaciones`.

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` (alias `eventId`) | `String` | Sí | Identificador único del evento (clave de idempotencia) |
| `fundacionId` (alias `foundationId`) | `Long` | Sí | ID de la fundación desactivada |
| `nombreFundacion` (alias `foundationName`) | `String` | No | Nombre oficial de la fundación aliada |
| `donantesAfectadosIds` | `List<Long>` | No | Lista de IDs de donantes con solicitudes activas |
| `representanteId` (alias `representativeId`) | `Long` | No | ID de usuario del representante de la fundación |
| `motivo` (alias `reason`) | `String` | No | Motivo de la desactivación administrativa |
| `fechaEvento` (alias `deactivatedAt`) | `LocalDateTime` | No | Marca de tiempo ISO-8601 (con o sin `Z`) |

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "foundationId": 3,
  "foundationName": "Fundación Abrigo Solidario",
  "representativeId": 24,
  "deactivatedAt": "2026-11-21T22:30:00Z",
  "reason": "Cierre temporal de operaciones"
}
```

### 1.12 `FundacionReactivada` (Emisor: `ms-Stores`)
- **Routing Keys:** `styleradar.tiendas.fundacion.reactivada` y `foundation.reactivated`
- **Cola Destino:** `styleradar.notificaciones.fundacion-reactivada.q`
- **Exchange:** `styleradar.exchange` (`topic`, durable)
- **Propósito:** Avisar al representante de la fundación (`representanteId`/`representativeId`) cuando su fundación vuelve a quedar activa para recibir donaciones. Genera notificación `CAMBIO_DONACION` con título «Fundacion reactivada» y enlace `/donaciones`.

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` (alias `eventId`) | `String` | Sí | Identificador único del evento (clave de idempotencia) |
| `fundacionId` (alias `foundationId`) | `Long` | Sí | ID de la fundación reactivada |
| `nombreFundacion` (alias `foundationName`) | `String` | No | Nombre oficial de la fundación aliada |
| `representanteId` (alias `representativeId`) | `Long` | No | ID de usuario del representante destinatario |
| `motivo` (alias `reason`) | `String` | No | Motivo o nota de reapertura |
| `fechaEvento` (alias `reactivatedAt`) | `LocalDateTime` | No | Marca de tiempo ISO-8601 (con o sin `Z`) |

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440001",
  "foundationId": 3,
  "foundationName": "Fundación Abrigo Solidario",
  "representativeId": 24,
  "reactivatedAt": "2026-11-21T22:35:00Z",
  "reason": "Reapertura de recepción de donaciones"
}
```

---

## 2. Eventos Publicados por `ms-Notifications`

### 2.1 `AlertaCreada` (Consumidor: `ms-Statistics` — NOT-16)
- **Routing Key:** `styleradar.notificaciones.alerta.creada`
- **Ejemplo JSON:**
```json
{
  "eventoId": "evt-alr-1001",
  "alertaId": 75,
  "usuarioId": 501,
  "tipoAlerta": "ITEM_AGOTADO",
  "itemId": 1045,
  "busquedaGuardadaId": null,
  "canal": "IN_APP",
  "fechaCreacion": "2026-11-02T12:30:00"
}
```

---

## 3. Propiedades del Broker

| Propiedad | Valor |
| :--- | :--- |
| Exchange | `styleradar.exchange` (tipo `topic`, durable) |
| Dead Letter Exchange | `styleradar.dlx` |
| Cola de errores (DLQ) | `styleradar.notificaciones.dlq` (routing key `styleradar.notificaciones.error`) |
| Colas consumidas | 13 (11 de Sprint 3 + 2 de Backlog GJ-117 / CADCHE v2), todas durables y con DLX configurada |
| Formato del mensaje | JSON (`application/json`), fechas ISO-8601 (con o sin sufijo `Z`) |
| Virtual host | `RABBITMQ_VHOST` (por defecto `/`) |
| TLS | `RABBITMQ_SSL_ENABLED` (puerto `5671` cuando está activo) |

### Manejo de errores

- **Payload inválido** (campo obligatorio ausente, estado de cuenta desconocido o JSON que no se puede convertir): el mensaje va directo a la DLQ, sin reintentos.
- **Fallo transitorio** (por ejemplo, la base de datos no responde): se reintenta hasta 3 veces con espera exponencial (1 s, 2 s, hasta 10 s) y después va a la DLQ.
- **Duplicados:** el consumidor es idempotente. Las notificaciones directas guardan el `eventoId` y la base de datos tiene una restricción única `(evento_id, usuario_id)`: una entrega repetida del mismo evento no genera una segunda notificación, ni siquiera si dos consumidores lo procesan a la vez.

---

## 4. Matriz de Contrastación Oficial de Eventos (0 Pendientes)

Todos los contratos fueron contrastados y verificados contra las Fichas Técnicas Oficiales de Integración compartidas por los equipos en Google Drive (`15wFfjZFzp1_Px9CEuXbxsgM8BpNCF8XP`):

| Microservicio Emisor | Evento de Negocio | Routing Key Oficial Emisor | Routing Key Consumida en `ms-Notifications` | Estado de Acuerdo |
| :--- | :--- | :--- | :--- | :---: |
| **`ms-Inventory`** | Reposición de Stock | `styleradar.inventario.actualizado` | `styleradar.inventario.actualizado` | **Confirmado 100%** |
| **`ms-ItemCatalog`** | Prenda Publicada | `styleradar.catalogo.prenda.publicada` | `styleradar.catalogo.prenda.publicada` | **Confirmado 100%** |
| **`ms-ItemCatalog`** | Prenda Retirada | `styleradar.catalogo.prenda.retirada` | `styleradar.catalogo.prenda.retirada` | **Confirmado 100%** |
| **`ms-ItemCatalog`** | Moderación Prenda | `styleradar.catalogo.moderacion.cambiada` | `styleradar.catalogo.moderacion.cambiada` | **Confirmado 100%** |
| **`ms-Users`** | Estado de Usuario | `user.status.changed` / `styleradar.usuarios.estado.cambiado` | `styleradar.usuarios.estado.cambiado` + `user.status.changed` | **Confirmado 100%** |
| **`ms-Chat`** | Mensaje STOMP Enviado | `styleradar.chat.mensaje.enviado` | `styleradar.chat.mensaje.enviado` | **Confirmado 100%** |
| **`ms-Donations`** | Estado de Donación | `styleradar.donaciones.estado.cambiado` | `styleradar.donaciones.estado.cambiado` | **Confirmado 100%** |
| **`ms-Donations`** | Solicitud Donación | `styleradar.donaciones.solicitud.creada` | `styleradar.donaciones.solicitud.creada` | **Confirmado 100%** |
| **`ms-Donations`** | Prenda C2C Vendida | `segunda-mano.prenda.vendida` / `styleradar.segundavida.prenda.vendida` | `styleradar.segundavida.prenda.vendida` | **Confirmado 100%** |
| **`ms-Payment`** | Pago Aprobado Webhook | `pago.confirmado` / `styleradar.pagos.pago.confirmado` | `styleradar.pagos.pago.confirmado` | **Confirmado 100%** |
| **`ms-Stores` (Tiendas)** | Fundación Desactivada | `foundation.deactivated` / `styleradar.tiendas.fundacion.desactivada` | `styleradar.tiendas.fundacion.desactivada` + `foundation.deactivated` | **Confirmado 100%** |
| **`ms-Stores` (Tiendas)** | Fundación Reactivada | `foundation.reactivated` / `styleradar.tiendas.fundacion.reactivada` | `styleradar.tiendas.fundacion.reactivada` + `foundation.reactivated` | **Confirmado 100%** |
