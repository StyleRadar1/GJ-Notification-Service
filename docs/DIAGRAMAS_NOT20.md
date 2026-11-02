# Diagramas Técnicos — Microservicio de Notificaciones (NOT-20)

**Sistema:** StyleRadar (DOSW · 2026)  
**Servicio:** `ms-notificaciones` (`edu.dosw.styleradar.notificaciones`)

---

## 1. Diagrama de Clases (Arquitectura Hexagonal)

```mermaid
classDiagram
    direction TB

    class Alerta {
        -Long id
        -Long usuarioId
        -TipoAlerta tipoAlerta
        -Long itemId
        -String tallaObjetivo
        -Long busquedaGuardadaId
        -CanalNotificacion canal
        -EstadoAlerta estado
        -LocalDateTime fechaCreacion
        -LocalDateTime fechaActualizacion
        +estaActiva() boolean
        +desactivar() void
        +marcarDisparada() void
    }

    class Notificacion {
        -Long id
        -Long usuarioId
        -Long alertaId
        -Long itemId
        -Integer cicloReposicion
        -TipoNotificacion tipo
        -CanalNotificacion canal
        -String titulo
        -String mensaje
        -String enlace
        -boolean leida
        -LocalDateTime fechaCreacion
        -LocalDateTime fechaLectura
        +marcarComoLeida() void
    }

    class BusquedaGuardadaCopia {
        -Long busquedaId
        -Long usuarioId
        -String nombre
        -CriterioBusquedaVO criterios
        -boolean activa
        -LocalDateTime fechaSincronizacion
    }

    class EstadoCuentaCopia {
        -Long usuarioId
        -String email
        -EstadoCuenta estado
        -LocalDateTime fechaActualizacion
        +puedeRecibirNotificaciones() boolean
    }

    class AlertaValidator {
        +validarCreacionAlerta(Alerta, EstadoCuentaCopia) void
        +validarItemAgotadoEnCatalogo(boolean, Long) void
        +validarBusquedaGuardadaActiva(BusquedaGuardadaCopia, Long) void
        +validarNoDuplicada(boolean) void
        +validarPropiedadAlerta(Alerta, Long) void
    }

    class EvaluadorCriteriosMatcher {
        +coincideConBusqueda(CriterioBusquedaVO, DatosPrendaVO) boolean
        +coincideItemAgotado(Long, String, DatosPrendaVO) boolean
    }

    class CrearAlertaUseCase {
        <<interface>>
        +crearAlerta(Alerta) Alerta
    }

    class ProcesarEventosExternosUseCase {
        <<interface>>
        +procesarDisponibilidadPrenda(DatosPrendaVO) List~Notificacion~
        +sincronizarBusquedaGuardada(BusquedaGuardadaCopia, boolean) void
        +actualizarEstadoCuenta(Long, String, EstadoCuenta) void
        +generarNotificacionDirecta(...) Notificacion
    }

    class AlertaRepositoryPort {
        <<interface>>
    }

    class NotificacionRepositoryPort {
        <<interface>>
    }

    class CatalogoClientPort {
        <<interface>>
    }

    class AlertaController {
        +crearAlerta(CrearAlertaRequest) ResponseEntity
        +listarMisAlertas() ResponseEntity
        +desactivarAlerta(Long) ResponseEntity
        +listarMisNotificaciones(Boolean) ResponseEntity
        +marcarNotificacionComoLeida(Long) ResponseEntity
        +marcarTodasComoLeidas() ResponseEntity
        +contarNoLeidas() ResponseEntity
        +suscribirCampanaTiempoReal() SseEmitter
    }

    class NotificacionesEventsListener {
        +onInventarioActualizado(InventarioActualizadoEventPayload) void
        +onPrendaPublicada(PrendaPublicadaEventPayload) void
        +onUsuarioEstadoCambiado(UsuarioEstadoCambiadoEventPayload) void
        +onBusquedaGuardadaSincronizada(BusquedaGuardadaSincronizadaEventPayload) void
        +onMensajeEnviado(MensajeEnviadoEventPayload) void
        +onDonacionEstadoCambiado(DonacionEstadoCambiadoEventPayload) void
        +onModeracionPrendaCambiada(ModeracionPrendaCambiadaEventPayload) void
    }

    AlertaController --> CrearAlertaUseCase
    NotificacionesEventsListener --> ProcesarEventosExternosUseCase
    CrearAlertaUseCase ..> Alerta
    CrearAlertaUseCase --> AlertaValidator
    CrearAlertaUseCase --> AlertaRepositoryPort
    CrearAlertaUseCase --> CatalogoClientPort
    ProcesarEventosExternosUseCase --> EvaluadorCriteriosMatcher
    ProcesarEventosExternosUseCase --> NotificacionRepositoryPort
    ProcesarEventosExternosUseCase ..> Notificacion
    ProcesarEventosExternosUseCase ..> BusquedaGuardadaCopia
    ProcesarEventosExternosUseCase ..> EstadoCuentaCopia
```

---

## 2. Diagrama Entidad-Relación de Base de Datos (`notificaciones_db`)

> **Aislamiento Database-per-Service:** Sin llaves foráneas hacia tablas de otros microservicios (`usuarios`, `items`, `busquedas_guardadas`), conservando la restricción única de idempotencia `(alerta_id, item_id, ciclo_reposicion)`.

```mermaid
erDiagram
    ALERTAS {
        BIGSERIAL id PK
        BIGINT usuario_id
        VARCHAR tipo_alerta
        BIGINT item_id
        VARCHAR talla_objetivo
        BIGINT busqueda_guardada_id
        VARCHAR canal
        VARCHAR estado
        TIMESTAMP fecha_creacion
        TIMESTAMP fecha_actualizacion
    }

    NOTIFICACIONES {
        BIGSERIAL id PK
        BIGINT usuario_id
        BIGINT alerta_id "UK(alerta_id, item_id, ciclo_reposicion)"
        BIGINT item_id "UK(alerta_id, item_id, ciclo_reposicion)"
        INTEGER ciclo_reposicion "UK(alerta_id, item_id, ciclo_reposicion)"
        VARCHAR tipo
        VARCHAR canal
        VARCHAR titulo
        VARCHAR mensaje
        VARCHAR enlace
        BOOLEAN leida
        TIMESTAMP fecha_creacion
        TIMESTAMP fecha_lectura
    }

    BUSQUEDAS_GUARDADAS_COPIA {
        BIGINT busqueda_id PK
        BIGINT usuario_id
        VARCHAR nombre
        VARCHAR texto_libre
        VARCHAR categoria
        VARCHAR color
        VARCHAR talla
        VARCHAR marca
        VARCHAR estilo
        NUMERIC precio_min
        NUMERIC precio_max
        VARCHAR localidad
        BOOLEAN activa
        TIMESTAMP fecha_sincronizacion
    }

    ESTADOS_CUENTA_COPIA {
        BIGINT usuario_id PK
        VARCHAR email
        VARCHAR estado
        TIMESTAMP fecha_actualizacion
    }

    ALERTAS ||--o{ NOTIFICACIONES : "genera (referencia logica alerta_id)"
    BUSQUEDAS_GUARDADAS_COPIA ||--o{ ALERTAS : "evaluada por (busqueda_guardada_id)"
    ESTADOS_CUENTA_COPIA ||--o{ ALERTAS : "valida elegibilidad (usuario_id)"
```

---

## 3. Diagrama de Secuencia 7.8 Actualizado con Eventos Asíncronos y SSE

```mermaid
sequenceDiagram
    autonumber
    actor Comprador
    participant GW as sr-api-gateway
    participant NOT as ms-notificaciones
    participant CAT as ms-item-catalogo
    participant INV as ms-inventario
    participant MQ as RabbitMQ (styleradar.exchange)
    participant DB as PostgreSQL (notificaciones_db)

    Comprador->>GW: POST /api/v1/usuarios/me/alertas (Bearer JWT)
    GW->>NOT: Forward POST /api/v1/usuarios/me/alertas
    NOT->>DB: Consulta estado de cuenta local y duplicados
    NOT->>CAT: GET /api/v1/catalogo/items/{itemId}/disponibilidad (OpenFeign)
    CAT-->>NOT: stockTotal = 0 (Agotado)
    NOT->>DB: INSERT INTO alertas (estado = ACTIVA)
    NOT->>MQ: Publish AlertaCreada (styleradar.notificaciones.alerta.creada)
    NOT-->>Comprador: 201 Created (AlertaResponse)

    Note over Comprador,NOT: El comprador abre la campana en tiempo real (SSE)
    Comprador->>NOT: GET /api/v1/usuarios/me/notificaciones/stream
    NOT-->>Comprador: SseEmitter abierto (event: connected)

    Note over INV,MQ: La tienda repone stock de la prenda agotada
    INV->>MQ: Publish InventarioActualizado (stockActual > 0, cicloReposicion = 2)
    MQ->>NOT: @RabbitListener onInventarioActualizado
    NOT->>DB: Busca alertas activas para itemId y evalua criterios en memoria
    NOT->>DB: Verifica UK (alerta_id, item_id, ciclo_reposicion) e inserta Notificacion
    NOT-->>Comprador: Push SSE en tiempo real (event: notificacion)
```

