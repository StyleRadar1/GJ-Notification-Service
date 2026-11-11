# Contratos de Eventos Asíncronos — Microservicio de Notificaciones (NOT-21)

**Servicio:** `ms-notificaciones` (Puerto `8300`)  
**Broker:** RabbitMQ (`styleradar.exchange` — Tipo `topic`, Dead Letter Exchange `styleradar.dlx`)  
**Acuerdos de Integración:** Catálogo (`ms-inventario`, `ms-item-catalogo`), Identidad (`ms-usuarios`), Mensajes (`ms-chat`), Donaciones (`ms-segunda-vida-donaciones`) y Estadísticas (`ms-estadisticas`).

---

## 1. Eventos Consumidos por `ms-notificaciones`

### 1.1 `InventarioActualizado` (Emisor: `ms-inventario`)
- **Routing Key:** `styleradar.inventario.actualizado`
- **Cola Destino:** `styleradar.notificaciones.inventario-actualizado.q`
- **Propósito:** Notificar una reposición de stock o cambio de unidades por prenda/talla/sede. Solo se notifica cuando la prenda se repone de stock cero (`stockAnterior == 0` y `stockActual > 0`); cualquier otro cambio de stock se ignora. El `cicloReposicion` preserva la restricción de idempotencia `(alerta_id, item_id, ciclo_reposicion)`.

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` | `String (UUID)` | Sí | Identificador único del evento |
| `itemId` | `Long` | Sí | ID de la prenda o ítem de catálogo |
| `nombrePrenda` | `String` | Sí | Nombre descriptivo de la prenda |
| `categoria` | `String` | Sí | Categoría comercial (ej. `CHAQUETAS`, `CALZADO`) |
| `color` | `String` | No | Color principal de la prenda |
| `talla` | `String` | Sí | Talla repuesta (ej. `M`, `40`) |
| `marca` | `String` | No | Marca o diseñador |
| `estilo` | `String` | No | Arquetipo o estilo (`STREETWEAR`, `SASTRE`, etc.) |
| `precio` | `BigDecimal` | Sí | Precio unitario vigente |
| `stockAnterior` | `Integer` | Sí | Stock antes de la actualización |
| `stockActual` | `Integer` | Sí | Nuevo stock disponible (con `stockAnterior == 0` y `> 0` dispara la evaluación de alertas) |
| `cicloReposicion` | `Integer` | Sí | Número secuencial de ciclo de reposición del ítem |
| `tiendaId` | `Long` | Sí | ID de la tienda aliada |
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

### 1.2 `PrendaPublicada` (Emisor: `ms-item-catalogo`)
- **Routing Key:** `styleradar.catalogo.prenda.publicada`
- **Cola Destino:** `styleradar.notificaciones.prenda-publicada.q`
- **Propósito:** Evaluar en memoria si una prenda recién publicada coincide con las alertas de tipo `BUSQUEDA_GUARDADA` activas.

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` | `String (UUID)` | Sí | Identificador único del evento |
| `itemId` | `Long` | Sí | ID de la nueva prenda |
| `nombrePrenda` | `String` | Sí | Nombre de la prenda |
| `categoria` | `String` | Sí | Categoría comercial |
| `color` | `String` | No | Color principal |
| `tallasDisponibles` | `List<String>` | Sí | Lista de tallas con stock inicial `> 0` |
| `marca` | `String` | No | Marca o diseñador |
| `estilo` | `String` | No | Arquetipo o estilo |
| `precio` | `BigDecimal` | Sí | Precio de publicación |
| `stockTotal` | `Integer` | Sí | Stock inicial total |
| `tiendaId` | `Long` | Sí | ID de la tienda |
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

### 1.3 `UsuarioEstadoCambiado` (Emisor: `ms-usuarios`)
- **Routing Key:** `styleradar.usuarios.estado.cambiado`
- **Cola Destino:** `styleradar.notificaciones.usuario-estado.q`
- **Propósito:** Mantener actualizada la copia local del estado de cuenta (`ACTIVO`, `INACTIVO`, `BLOQUEADO`) para impedir que cuentas inactivas o bloqueadas reciban notificaciones (NOT-09).

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` | `String` | Sí | Identificador del evento |
| `usuarioId` | `Long` | Sí | ID del usuario |
| `email` | `String` | No | Correo electrónico del usuario |
| `nuevoEstado` | `String` | Sí | `ACTIVO`, `INACTIVO` o `BLOQUEADO` |
| `fechaCambio` | `LocalDateTime` | Sí | Fecha del cambio de estado |

**Ejemplo JSON:**
```json
{
  "eventoId": "evt-usr-7741",
  "usuarioId": 501,
  "email": "mateo@styleradar.co",
  "nuevoEstado": "INACTIVO",
  "fechaCambio": "2026-11-02T12:15:00"
}
```

---

### 1.4 `MensajeEnviado` (Emisor: `ms-chat`)
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

### 1.6 `PrendaRetirada` (Emisor: `ms-item-catalogo`)
- **Routing Key:** `styleradar.catalogo.prenda.retirada`
- **Cola Destino:** `styleradar.notificaciones.prenda-retirada.q`
- **Propósito:** Desactivar las alertas activas de una prenda que el catálogo retiró o eliminó, para que no queden alertas sobre un ítem que ya no existe.

| Campo | Tipo | Obligatorio | Descripción |
| :--- | :--- | :---: | :--- |
| `eventoId` | `String` | Sí | Identificador del evento |
| `itemId` | `Long` | Sí | ID de la prenda retirada |
| `motivo` | `String` | No | Motivo del retiro (eliminada por el dueño, moderación, etc.) |
| `fechaEvento` | `LocalDateTime` | Sí | Fecha y hora del retiro |

**Ejemplo JSON:**
```json
{
  "eventoId": "evt-cat-30188",
  "itemId": 1045,
  "motivo": "ELIMINADA_POR_DUENO",
  "fechaEvento": "2026-11-02T13:00:00"
}
```

---

## 2. Eventos Publicados por `ms-notificaciones`

### 2.1 `AlertaCreada` (Consumidor: `ms-estadisticas` — NOT-16)
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
| Colas consumidas | 8, todas durables y con la DLX configurada |
| Formato del mensaje | JSON (`application/json`), fechas en ISO-8601 |
| Virtual host | `RABBITMQ_VHOST` (por defecto `/`) |
| TLS | `RABBITMQ_SSL_ENABLED` (puerto `5671` cuando está activo) |

### Manejo de errores

- **Payload inválido** (campo obligatorio ausente, estado de cuenta desconocido o JSON que no se puede convertir): el mensaje va directo a la DLQ, sin reintentos.
- **Fallo transitorio** (por ejemplo, la base de datos no responde): se reintenta hasta 3 veces con espera exponencial (1 s, 2 s, hasta 10 s) y después va a la DLQ.
- **Duplicados:** el consumidor es idempotente; una entrega repetida del mismo evento no genera una segunda notificación.

### Nombres de eventos pendientes de confirmar

Los nombres de esta página son los que implementa `ms-notificaciones` hoy. Si el equipo de Inventario y Catálogo publica con otros nombres (por ejemplo `StockChangedEvent` o `NuevaPrendaCreadaEvent`), basta con cambiar la routing key en `application.messaging.routing-keys`; el payload no cambia.
