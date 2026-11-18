# Documento Oficial de Endpoints y Requerimientos Funcionales
## Microservicio de Notificaciones y Alertas (`ms-notificaciones` · Puerto `8300`)
**Proyecto:** StyleRadar · **Equipo:** Guatoque Jiraguas (GJ) · **Asignatura:** DOSW 2026

---

## 1. Resumen Ejecutivo y Alineación con el Frontend

El microservicio **`ms-notificaciones`** gestiona el ciclo completo de alertas de disponibilidad (prendas agotadas y búsquedas guardadas) y la entrega de notificaciones en tiempo real e historial en la campana y por correo electrónico.

Todos los endpoints públicos de la API REST siguen el estándar canónico en inglés bajo **`/api/v1/users/{id}/...`**, con documentación OpenAPI 3.0 en español **100% alineada con los componentes visuales reales del Frontend de StyleRadar**:
* **Detalle de Prenda Agotada:** Botón **«AVISARME CUANDO LLEGUE»**, alerta de stock existente (*«Esta prenda ya tiene unidades, puedes reservarla ahora»* + botón **«RESERVAR EN TIENDA»**) y manejo de errores (*«No pudimos crear la alerta, intenta más tarde»* + botón **«Reintentar»**).
* **Sección «Mis búsquedas y alertas»:** Ubicada tanto en **Mi Perfil** como en la opción final del desplegable de la **Campana**, donde el comprador consulta y elimina/desactiva sus búsquedas guardadas y las prendas agotadas que espera.
* **Campana Reactiva en Barra Superior:** Icono de campana con **burbuja morada y número blanco** para notificaciones sin leer, bandeja desplegable paginada y actualización instantánea vía **Server-Sent Events (SSE)** sin recargar la página (`F5`).

---

## 2. Catálogo Completo de Endpoints (Swagger / OpenAPI 3.0)

### Tabla Resumen de Endpoints

| # | Método | Endpoint Canónico | Componente / Acción en el Frontend | Códigos HTTP | Req. Asociado |
| :-: | :---: | :--- | :--- | :---: | :---: |
| **1** | `POST` | `/api/v1/users/{id}/alerts` | Botón **«AVISARME CUANDO LLEGUE»** (crea alerta de prenda agotada o búsqueda guardada, con opción de copia por correo `canal: "AMBOS"`). | `201`, `400`, `401`, `403`, `409`, `422`, `503` | `SR-RF-28`, `SR-RF-29`, `NOT-05`, `NOT-10` |
| **2** | `GET` | `/api/v1/users/{id}/alerts/check` | Estado inicial del botón en detalle de prenda (`?itemId={uuid}&tallaObjetivo={talla}`): indica si el usuario ya está suscrito (`suscrita: true/false`). | `200`, `401`, `403` | `SR-RF-28`, `NOT-05` |
| **3** | `GET` | `/api/v1/users/{id}/alerts` | Vista **«Mis búsquedas y alertas»** (en **Mi Perfil** y al final de la **Campana**): lista búsquedas guardadas y prendas agotadas que espera. | `200`, `401`, `403` | `SR-RF-28`, `SR-RF-29`, `NOT-05` |
| **4** | `DELETE` | `/api/v1/users/{id}/alerts/{alertId}` | Desactivar alerta (**borrado lógico / soft-delete**) desde **«Mis búsquedas y alertas»**, pasando su estado a `DESACTIVADA`. | `200`, `401`, `403`, `404` | `SR-RF-28`, `SR-RF-29`, `NOT-05` |
| **5** | `GET` | `/api/v1/users/{id}/notifications` | Desplegable de la **Campana**: lista el historial de notificaciones recibidas (soporta paginación `?page=0&size=20` y filtro único `?read=false` o `true`). | `200`, `400`, `401`, `403` | `SR-RF-32`, `NOT-12`, `GJ-118` |
| **6** | `GET` | `/api/v1/users/{id}/notifications/unread/count` | **Burbuja morada con número blanco** sobre la campana: devuelve el total exacto de notificaciones sin leer (`cantidadNoLeidas`). | `200`, `401`, `403` | `SR-RF-32`, `NOT-13` |
| **7** | `PATCH` | `/api/v1/users/{id}/notifications/{notificationId}/read` | Clic sobre una notificación en la campana: la marca como leída (`leida: true`) y descuenta 1 de la burbuja morada. | `200`, `401`, `403`, `404` | `SR-RF-32`, `NOT-13` |
| **8** | `PATCH` | `/api/v1/users/{id}/notifications/read-all` | Botón **«Marcar todas como leídas»** en la campana: limpia todas las pendientes y deja la burbuja morada en `0`. | `200`, `401`, `403` | `SR-RF-32`, `NOT-13` |
| **9** | `GET` | `/api/v1/users/{id}/notifications/stream` | Conexión **SSE (`text/event-stream`)**: actualiza en tiempo real la campana y su burbuja morada sin recargar la página (`F5`). | `200`, `401`, `403` | `SR-RF-32`, `NOT-15`, `GJ-62` |
| **10** | `GET` | `/actuator/health` | Comprobación de salud operativa para Azure App Service y monitoreo continuo. | `200` | `SR-RNF-15` |

---

### Detalle Funcional de Cada Endpoint

#### 1. `POST /api/v1/users/{id}/alerts` — Crear Alerta (Botón «AVISARME CUANDO LLEGUE»)
* **¿Para qué sirve?:** Registra una alerta cuando el usuario quiere que le avisen apenas llegue stock de una prenda agotada (`ITEM_AGOTADO`) o cuando se publique una prenda que coincida con una búsqueda guardada (`BUSQUEDA_GUARDADA`).
* **Cuerpo de la petición (`CrearAlertaRequest`):**
  ```json
  {
    "tipoAlerta": "ITEM_AGOTADO",
    "itemId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "tallaObjetivo": "M",
    "busquedaGuardadaId": null,
    "canal": "IN_APP"
  }
  ```
  *(Si el usuario marca el checkbox de recibir copia por correo electrónico al crear la alerta, el frontend envía `"canal": "AMBOS"`).*
* **Flujos de Respuesta alineados con el Frontend:**
  * **`201 Created`:** La prenda efectivamente está agotada; la alerta queda `ACTIVA` y se publica el evento `AlertaCreada` hacia `ms-estadisticas`.
  * **`422 Unprocessable Entity`:** El microservicio consultó a `ms-item-catalogo` y detectó que la prenda **ya tiene unidades disponibles**. El frontend muestra: *«Esta prenda ya tiene unidades, puedes reservarla ahora»* y el botón **«RESERVAR EN TIENDA»**.
  * **`409 Conflict`:** El usuario ya tenía una alerta activa para esa misma prenda y talla.
  * **`503 Service Unavailable`:** Si `ms-item-catalogo` no responde o hay un fallo de red, el frontend muestra: *«No pudimos crear la alerta, intenta más tarde»* junto con el botón **«Reintentar»**.

#### 2. `GET /api/v1/users/{id}/alerts/check?itemId={uuid}&tallaObjetivo={talla}` — Comprobar Suscripción de Alerta
* **¿Para qué sirve?:** Cuando el comprador abre la página de una prenda agotada, el frontend consulta este endpoint ligero (usando el parámetro unificado `tallaObjetivo`, sin colisionar con `size` de paginación) para saber de inmediato si debe pintar el botón **«AVISARME CUANDO LLEGUE»** (`suscrita: false`) o mostrar que la alerta ya está activa (`suscrita: true`, devolviendo además el `alertaId` por si desea desactivarla con un clic), evitando descargar toda la lista de alertas del usuario.
* **Ejemplo de respuesta (`200 OK`):**
  ```json
  {
    "suscrita": true,
    "alertaId": 501,
    "itemId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "tallaObjetivo": "M",
    "canal": "IN_APP"
  }
  ```

#### 3. `GET /api/v1/users/{id}/alerts` — Listar «Mis búsquedas y alertas»
* **¿Para qué sirve?:** Alimenta la sección **«Mis búsquedas y alertas»**, a la cual el usuario accede desde **Mi Perfil** o desde el botón ubicado al final del desplegable de la **Campana**. Retorna todas las alertas de prendas agotadas que espera y las alertas de búsquedas guardadas.

#### 4. `DELETE /api/v1/users/{id}/alerts/{alertId}` — Desactivar Alerta (Borrado Lógico / Soft-Delete)
* **¿Para qué sirve?:** Realiza un **borrado lógico (soft-delete)** cuando el usuario quita una búsqueda guardada o una prenda agotada en espera desde la sección **«Mis búsquedas y alertas»** (o desde el detalle de la prenda). No elimina físicamente el registro en la base de datos para conservar la trazabilidad e historial, sino que cambia su estado a `DESACTIVADA` para no volver a emitir avisos sobre ese recurso.

#### 5. `GET /api/v1/users/{id}/notifications` — Bandeja Desplegable de la Campana
* **¿Para qué sirve?:** Devuelve los avisos recibidos por el usuario cuando abre el menú de la campana (reposiciones de stock, prendas nuevas de búsquedas guardadas, mensajes nuevos de chat, cambios de estado de donaciones, moderación de prendas, prendas vendidas y pagos confirmados).
* **Parámetros expuestos en Swagger (sin duplicidad):**
  * `?read=false` (o `true`): Único filtro de estado de lectura (`false` = no leídas, `true` = leídas).
  * `?page=0&size=20`: Devuelve la respuesta paginada con metadatos (`content`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`, `hasPrevious`).

#### 6. `GET /api/v1/users/{id}/notifications/unread/count` — Contador de la Burbuja Morada
* **¿Para qué sirve?:** Retorna un JSON ultraligero (`{"usuarioId": 101, "cantidadNoLeidas": 3}`) diseñado exclusivamente para pintar la **burbuja morada con número blanco** sobre el icono de la campana en el encabezado del frontend.

#### 7. `PATCH /api/v1/users/{id}/notifications/{notificationId}/read` — Marcar Notificación como Leída
* **¿Para qué sirve?:** Cuando el usuario hace clic sobre una notificación en la campana para ir al detalle (ej. abrir el chat o ver la prenda repuesta), marca esa notificación como `leida: true` con su `fechaLectura` y reduce en 1 el contador de la burbuja morada.

#### 8. `PATCH /api/v1/users/{id}/notifications/read-all` — Marcar Todas como Leídas
* **¿Para qué sirve?:** Marca en una sola operación todas las notificaciones pendientes del usuario como leídas y devuelve el contador de la burbuja morada en `0`.

#### 9. `GET /api/v1/users/{id}/notifications/stream` — Flujo en Tiempo Real (SSE)
* **¿Para qué sirve?:** Abre un canal persistente **Server-Sent Events (`text/event-stream`)**. Cuando ocurre cualquier evento en RabbitMQ que genera una notificación para el usuario, el backend empuja el evento `notificacion` en tiempo real al navegador, actualizando la burbuja morada y la lista de la campana al instante sin recargar con `F5`. Soporta la cabecera estándar `Last-Event-ID` para reenviar notificaciones perdidas tras microcortes de red.

---

## 3. Matriz Completa de Requerimientos del Microservicio

### 3.1. Requerimientos Funcionales Globales (`SR-RF`)

| Código | Nombre del Requerimiento Funcional | Descripción y Reglas de Negocio Implementadas | Endpoints / Eventos Relacionados | Estado |
| :---: | :--- | :--- | :--- | :---: |
| **`SR-RF-28`** | **Alertas de disponibilidad sobre prendas agotadas («AVISARME CUANDO LLEGUE»)** | Permite al comprador suscribirse a una prenda agotada (UUID) y talla específica eligiendo si desea notificación en plataforma (`IN_APP`) o también copia por correo (`AMBOS`). Valida síncronamente contra `ms-item-catalogo` vía OpenFeign que la prenda no tenga stock (`422` si ya tiene unidades para activar el botón *«RESERVAR EN TIENDA»*, `503` si catálogo no responde para mostrar *«No pudimos crear la alerta, intenta más tarde»*) e impide duplicados (`409`). Incluye consulta rápida de estado (`GET /alerts/check`). | • `POST /api/v1/users/{id}/alerts`<br>• `GET /api/v1/users/{id}/alerts/check`<br>• `GET /api/v1/users/{id}/alerts`<br>• `DELETE /api/v1/users/{id}/alerts/{alertId}` | ✅ Cumplido |
| **`SR-RF-29`** | **Gestión de «Mis búsquedas y alertas» y sincronización de búsquedas guardadas** | Mantiene sincronizada una copia local de las búsquedas guardadas del usuario (`BusquedaGuardadaSincronizada`), permite activar/desactivar alertas sobre ellas desde la sección **«Mis búsquedas y alertas»** (en el perfil y al final de la campana) y desactiva automáticamente las alertas si la búsqueda o la prenda es eliminada del catálogo (`PrendaRetirada`). | • `POST /api/v1/users/{id}/alerts`<br>• `GET /api/v1/users/{id}/alerts`<br>• `DELETE /api/v1/users/{id}/alerts/{alertId}`<br>• `styleradar.usuarios.busqueda.sincronizada`<br>• `styleradar.catalogo.prenda.retirada` | ✅ Cumplido |
| **`SR-RF-30`** | **Motor reactivo de evaluación de reposición de stock y nuevas prendas** | Procesa asíncronamente `InventarioActualizado` (soportando `stockAnterior == 0` o `stockAnterior == null` por backlog `RES-133` cuando `stockActual > 0`) y `PrendaPublicada`. Evalúa en memoria (`EvaluadorCriteriosMatcher`) todas las alertas activas por ítem/talla y por criterios multicampo (categoría, color, talla, marca, estilo, precio mín/máx y localidad) sin hacer SQL cruzado. | • `styleradar.inventario.actualizado`<br>• `styleradar.catalogo.prenda.publicada` | ✅ Cumplido |
| **`SR-RF-31`** | **Notificaciones transversales del ecosistema (Chat, Donaciones, Moderación, Segunda Vida, Tiendas y Pagos)** | Genera notificaciones automáticas en plataforma y por correo (con redirección contextual) cuando:<br>1. Llega un mensaje nuevo en `ms-chat` (`MENSAJE_NUEVO`).<br>2. Cambia el estado de una donación o se crea una solicitud en `ms-donaciones` (`CAMBIO_DONACION`, `SOLICITUD_DONACION`).<br>3. Se desactiva una fundación aliada en `ms-tiendas` (`CAMBIO_DONACION`).<br>4. Se aprueba/rechaza una prenda en `ms-item-catalogo` (`MODERACION_PRENDA`) o se vende una prenda de segunda mano (`PRENDA_VENDIDA`).<br>5. Se confirma un pago o suscripción Radar+ en `ms-pagos` (`PAGO_CONFIRMADO`). | • `styleradar.chat.mensaje.enviado`<br>• `styleradar.donaciones.estado.cambiado`<br>• `styleradar.donaciones.solicitud.creada`<br>• `styleradar.tiendas.fundacion.desactivada`<br>• `styleradar.catalogo.moderacion.cambiada`<br>• `styleradar.segundavida.prenda.vendida`<br>• `styleradar.pagos.pago.confirmado` | ✅ Cumplido |
| **`SR-RF-32`** | **Campana de notificaciones, burbuja morada de no leídas y streaming SSE** | Provee la bandeja paginada de notificaciones, el contador ligero para la **burbuja morada con número blanco** sobre la campana, el marcado individual y masivo como leídas, y el canal reactivo **Server-Sent Events (SSE)** con reconexión automática (`Last-Event-ID`). | • `GET /api/v1/users/{id}/notifications`<br>• `GET /api/v1/users/{id}/notifications/unread/count`<br>• `PATCH .../notifications/{id}/read`<br>• `PATCH .../notifications/read-all`<br>• `GET /api/v1/users/{id}/notifications/stream` | ✅ Cumplido |
| **`SR-RF-33`** | **Publicación de eventos de analítica hacia Estadísticas** | Publica en `styleradar.exchange` el evento `AlertaCreadaDomainEvent` (`styleradar.notificaciones.alerta.creada`) cada vez que se registra una alerta, permitiendo a `ms-estadisticas` construir métricas de demanda por prenda y talla. | • `styleradar.notificaciones.alerta.creada` | ✅ Cumplido |

---

### 3.2. Desglose de Historias Técnicas e Historias de Usuario (`NOT-01` a `NOT-22` / `GJ`)

| Código Interno | Nombre de la Historia / Requerimiento Específico | Qué garantiza en el Microservicio | Estado |
| :---: | :--- | :--- | :---: |
| **`NOT-01` / `NOT-02`** | Modelo de dominio puro y Arquitectura Hexagonal | Separación estricta en capas (`domain`, `application`, `infrastructure`) sin dependencias de Spring ni JPA en el núcleo de dominio. | ✅ Cumplido |
| **`NOT-03` / `NOT-04`** | Persistencia relacional PostgreSQL 16 y migraciones Flyway | Esquema versionado con Flyway (`V1` a `V6`), soporte de `itemId` tipo `VARCHAR(100)` para UUIDs de catálogo/inventario/playlist e índices compuestos para la campana. | ✅ Cumplido |
| **`NOT-05`** | API REST de Alertas (`POST`, `GET`, `GET /check`, `DELETE`) | Gestión completa del botón «AVISARME CUANDO LLEGUE» y la sección «Mis búsquedas y alertas». | ✅ Cumplido |
| **`NOT-06` / `NOT-07`** | Consumidores de `InventarioActualizado` y `PrendaPublicada` + Matcher en memoria | Evaluación de criterios en memoria (`EvaluadorCriteriosMatcher`) y tolerancia a `stockAnterior == null` (backlog `RES-133`). | ✅ Cumplido |
| **`NOT-08`** | Desactivación automática por retiro de prenda o eliminación de búsqueda | Baja automática de alertas huérfanas al consumir `PrendaRetirada`, `ModeracionPrendaCambiada (RETIRADA)` o `BusquedaGuardadaSincronizada (ELIMINADA)`. | ✅ Cumplido |
| **`NOT-09`** | Sincronización de estado de cuenta (`UsuarioEstadoCambiado`) | Bloquea la creación de alertas y la emisión de notificaciones a cuentas en estado `INACTIVO`, `SUSPENDIDO` o `BLOQUEADO`. | ✅ Cumplido |
| **`NOT-10`** | Validación síncrona de stock agotado contra `ms-item-catalogo` | Cliente declarativo OpenFeign con manejo de errores (`422` si hay stock disponible, `503` si catálogo no responde). | ✅ Cumplido |
| **`NOT-11`** | Idempotencia por ciclo de reposición y por `eventoId` | Restricciones únicas `uk_notificacion_alerta_item_ciclo` y `uk_notificaciones_evento_usuario` para evitar spam o duplicados ante concurrencia. | ✅ Cumplido |
| **`NOT-12` / `GJ-118`** | Bandeja de notificaciones con paginación y filtros | Soporte de consulta simple y paginada (`page`, `size` de 1 a 100, `read`, `unreadOnly`) ordenada por fecha descendente. | ✅ Cumplido |
| **`NOT-13`** | Contador de la burbuja morada y lectura individual/masiva | Endpoints de bajo costo para el badge de no leídas y actualización de estado de lectura. | ✅ Cumplido |
| **`NOT-14` / `GJ-61` / `GJ-65`** | Consumidores de Chat, Donaciones, Moderación, Tiendas y Pagos | Generación de avisos con enlaces de navegación profundos (`/chat/conversaciones/{id}`, `/donaciones/{id}`, `/segunda-mano/{id}`). | ✅ Cumplido |
| **`NOT-15` / `GJ-62`** | Streaming en tiempo real con Server-Sent Events (SSE) | Registro de emisores por usuario (`SseEmitterRegistry`), empuje inmediato y recuperación de eventos perdidos con `Last-Event-ID`. | ✅ Cumplido |
| **`NOT-16`** | Publicación de `AlertaCreada` en RabbitMQ | Emisión de eventos de dominio hacia `ms-estadisticas`. | ✅ Cumplido |
| **`GJ-119`** | Envío de correos SMTP resiliente con cola de reintentos | Envío asíncrono con `JavaMailSender` y tabla `correos_pendientes` para reintentar automáticamente si el servidor SMTP falla. | ✅ Cumplido |
| **`NOT-22` / `GJ-35`** | Seguridad JWT Stateless y protección BOLA/IDOR | Filtro `JwtAuthenticationFilter` (HS256) y `SecurityContextUserExtractor` que garantiza que cada usuario solo acceda a su propio `{id}`. | ✅ Cumplido |

---

### 3.3. Requerimientos No Funcionales (`SR-RNF`)

| Código | Atributo de Calidad | Especificación Técnica Implementada | Estado |
| :---: | :--- | :--- | :---: |
| **`SR-RNF-01`** | **Seguridad y Autorización (JWT + RBAC + Anti-IDOR)** | Tokens JWT firmados con HMAC-SHA256 (`JWT_SECRET`). Validación estricta de pertenencia del recurso `{id}` contra el claim `sub`/`userId` del token (`401 Unauthorized` sin token, `403 Forbidden` si intenta acceder a otro usuario sin rol `ROLE_ADMIN_STYLERADAR`). | ✅ Cumplido |
| **`SR-RNF-08`** | **Desacoplamiento y Database-per-Service** | Base de datos PostgreSQL 16 exclusiva (`notificaciones_db`). Cero consultas SQL cruzadas hacia otros microservicios; toda comunicación externa ocurre por RabbitMQ o REST OpenFeign. | ✅ Cumplido |
| **`SR-RNF-15`** | **Resiliencia, Reintentos y Dead Letter Queue (DLQ)** | Payloads inválidos se rechazan sin reencolar (`AmqpRejectAndDontRequeueException`) directo a `styleradar.notificaciones.dlq` vía `styleradar.dlx`. Fallos transitorios se reintentan hasta 3 veces con backoff exponencial (`RabbitRetryConfig`). | ✅ Cumplido |
| **`SR-RNF-18`** | **Calidad de Código y Cobertura de Pruebas ($\ge 85\%$)** | Verificación obligatoria con JaCoCo en Maven (`mvn verify`) y pipelines CI/CD de GitHub Actions. Cobertura actual: **94.0%** con suite completa de pruebas unitarias, de concurrencia, WireMock, GreenMail y Testcontainers. | ✅ Cumplido |
| **`SR-RNF-20`** | **Estandarización de Errores HTTP (RFC 7807)** | Todas las respuestas de error (`400`, `401`, `403`, `404`, `409`, `422`, `503`) se serializan bajo el estándar `application/problem+json` (`ProblemDetail`). | ✅ Cumplido |
