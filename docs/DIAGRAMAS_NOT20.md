# DOCUMENTACIÓN ARQUITECTÓNICA Y DIAGRAMAS UML — GJ-Notification-Service (NOT-20)

- **Microservicio:** `GJ-Notification-Service` (`ms-notificaciones`, Puerto `8300`)
- **Equipo:** Guatoque Jiraguas (`GJ`)
- **Bounded Context:** Notificaciones, Alertas de Disponibilidad y Tiempo Real (`SSE`)
- **Ubicación de archivos fuente Draw.io:** [`docs/uml/`](./uml/)

> **Regla de Oro de Ingeniería StyleRadar:** Todo diagrama arquitectónico está acompañado de su **Explicación Técnica** (qué representa y cómo fluye la información) y su **Justificación Arquitectónica** (por qué se tomaron esas decisiones de diseño frente a otras alternativas).
>
> **Diagramas Oficiales Solicitados para el Microservicio:**
> 1. **Diagrama de Componentes con Arquitectura Hexagonal:** [`docs/uml/diagrama-componentes-especifico.drawio`](./uml/diagrama-componentes-especifico.drawio)
> 2. **Diagrama de Clases (Modelo de Dominio):** [`docs/uml/diagrama-clases.drawio`](./uml/diagrama-clases.drawio)
> 3. **Diagrama de Base de Datos (Modelo Relacional / ER):** [`docs/uml/diagrama-base-datos.drawio`](./uml/diagrama-base-datos.drawio)
> *(Diagramas complementarios de apoyo para el curso: Despliegue en `docs/uml/diagrama-despliegue.drawio` y Contexto C4 en `docs/uml/diagrama-contexto.drawio`)*

---

## 1. Diagrama de Contexto del Sistema (Nivel C4 - L1)

- **Archivo versionado en Draw.io:** [`docs/uml/diagrama-contexto.drawio`](./uml/diagrama-contexto.drawio)

<p align="center">
  <a href="./images/diagrama-contexto.png" target="_blank">
    <img src="./images/diagrama-contexto.png" alt="Diagrama de Contexto del Sistema" width="95%" style="border-radius: 8px; border: 1px solid #CBD5E1; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);" />
  </a>
</p>

### 1.1. Explicación Técnica
El Diagrama de Contexto delimita la frontera del microservicio **`GJ-Notification-Service` (`:8300`)** dentro del ecosistema distribuido de StyleRadar e identifica los actores y sistemas externos con los que intercambia información:
1. **Usuario Comprador / Donatario y API Gateway (`sr-api-gateway :8080`):** El usuario autenticado interactúa desde la SPA de StyleRadar a través del API Gateway mediante peticiones HTTPS REST (`/api/v1/usuarios/me/alertas` y `/api/v1/usuarios/me/notificaciones`) portando su token Bearer JWT, y mantiene una conexión abierta **Server-Sent Events (`SSE`)** en `/api/v1/usuarios/me/notificaciones/stream` para recibir notificaciones en la campana sin recargar la página.
2. **Microservicio de Catálogo e Inventario (`ms-item-catalogo :8220`):** Interactúa de forma dual:
   - **Síncrona (OpenFeign REST):** Al crear una alerta por ítem específico (`NOT-10`), `GJ-Notification-Service` consulta a Catálogo para verificar que la prenda esté realmente agotada (`stockTotal == 0`).
   - **Asíncrona (RabbitMQ AMQP):** Catálogo publica los eventos `InventarioActualizado`, `PrendaPublicada` y `ModeracionPrendaCambiada`, los cuales son consumidos por `GJ-Notification-Service`.
3. **Microservicio de Identidad y Búsquedas (`ms-usuarios :8120`):** Publica en RabbitMQ los eventos `UsuarioEstadoCambiado` y `BusquedaGuardadaSincronizada` para mantener actualizadas las copias locales en la base de datos de notificaciones (`NOT-08`, `NOT-09`).
4. **Microservicios de Mensajes y Donaciones (`ms-mensajes`, `ms-donaciones`):** Publican los eventos `MensajeEnviado` y `DonacionEstadoCambiado` (`NOT-14`) para generar notificaciones transaccionales con enlace directo.
5. **Microservicio de Estadísticas (`ms-estadisticas`):** Consume el evento de dominio `AlertaCreada` (`routingKey: alerta.creada`) publicado por `GJ-Notification-Service` cada vez que un usuario registra una nueva alerta (`NOT-16`).
6. **Base de Datos Propia (`PostgreSQL 16 — notificaciones_db`):** Almacena de forma aislada las alertas, notificaciones y copias locales sin compartir tablas con ningún otro microservicio.

### 1.2. Justificación Arquitectónica
- **Desacoplamiento por Eventos (Event-Driven Architecture) frente a Polling SQL:** En el monolito original, un `DetectorDisponibilidad` ejecutaba consultas periódicas sobre las tablas de inventario. Al extraer el microservicio, ese acoplamiento violaría el principio *Database-per-Service*. Reemplazar el detector por suscripción asíncrona a eventos AMQP en RabbitMQ elimina el acoplamiento temporal y de base de datos, reduciendo la latencia de notificación a milisegundos.
- **Validación Síncrona Puntual con OpenFeign (`NOT-10`):** Se utiliza comunicación síncrona únicamente en el comando `POST /alertas` porque el usuario necesita una respuesta inmediata (`201 Created` o `409 Conflict`) que confirme si el ítem está agotado antes de registrar la alerta.

---

## 2. Diagrama de Componentes Específico (Arquitectura Hexagonal)

- **Archivo versionado en Draw.io:** [`docs/uml/diagrama-componentes-especifico.drawio`](./uml/diagrama-componentes-especifico.drawio)

<p align="center">
  <a href="./images/diagrama-componentes-especifico.png" target="_blank">
    <img src="./images/diagrama-componentes-especifico.png" alt="Diagrama de Componentes Hexagonal" width="95%" style="border-radius: 8px; border: 1px solid #CBD5E1; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);" />
  </a>
</p>

### 2.1. Explicación Técnica
Representa la estructura interna de **`GJ-Notification-Service`** siguiendo estrictamente el patrón **Arquitectura Hexagonal (Puertos y Adaptadores)** dividido en tres anillos concéntricos con inversión de dependencias hacia el núcleo:
1. **Adaptadores de Entrada (`infrastructure.adapter.in` — Driving Adapters):**
   - `AlertaController` (que implementa la interfaz documentada `AlertaApi` de OpenAPI/Swagger) recibe las solicitudes HTTP bajo `/api/v1/usuarios/me`.
   - `JwtAuthenticationFilter` y `SecurityConfig` interceptan cada petición stateless, validan la firma HMAC-SHA256 del JWT y extraen el `usuarioId` autenticado (`NOT-22`).
   - `NotificacionesEventsListener` escucha la cola durable `styleradar.notificaciones.queue` en RabbitMQ y delega la carga útil de cada evento al puerto de entrada correspondiente.
2. **Capa de Aplicación y Puertos de Entrada (`domain.port.in`, `application.usecase`, `application.service`):**
   - Los controladores y listeners solo conocen las **interfaces de los casos de uso** (`CrearAlertaUseCase`, `ListarAlertasUseCase`, `DesactivarAlertaUseCase`, `ListarNotificacionesUseCase`, `MarcarNotificacionLeidaUseCase`, `ProcesarEventosExternosUseCase`).
   - Las implementaciones `*UseCaseImpl` y la capa `application/service` (`AlertaApplicationService` + `AlertaApplicationServiceImpl`, `NotificacionApplicationService` + `NotificacionApplicationServiceImpl`) coordinan el flujo transaccional, transforman DTOs inmutables (`record`) con **MapStruct** (`AlertaDtoMapper`, `NotificacionDtoMapper`) e invocan las reglas del dominio.
3. **Núcleo de Dominio Puro (`domain.model`, `domain.validator`, `domain.event`):**
   - Contiene las entidades puras (`Alerta`, `Notificacion`, `BusquedaGuardadaCopia`, `EstadoCuentaCopia`), objetos de valor (`CriterioBusquedaVO`, `DatosPrendaVO`), `AlertaValidator` y `EvaluadorCriteriosMatcher`, escritos en Java 21 puro sin una sola dependencia de Spring ni JPA.
4. **Puertos de Salida (`domain.port.out`) y Adaptadores de Salida (`infrastructure.adapter.out` — Driven Adapters):**
   - `AlertaRepositoryAdapter`, `NotificacionRepositoryAdapter`, `BusquedaGuardadaCopiaRepositoryAdapter` y `EstadoCuentaCopiaRepositoryAdapter` implementan los puertos de persistencia usando Spring Data JPA sobre PostgreSQL.
   - `CatalogoClientAdapter` implementa `CatalogoClientPort` mediante `CatalogoFeignClient`.
   - `RabbitMqEventPublisherAdapter` implementa `EventPublisherPort` publicando `AlertaCreadaDomainEvent`.
   - `SseRealTimeNotificationAdapter` implementa `RealTimeNotificationPort` gestionando los `SseEmitter` activos por usuario.

### 2.2. Justificación Arquitectónica
- **Aislamiento del Dominio y Testabilidad (> 85 % JaCoCo):** Al mantener `domain` libre de frameworks y exigir siempre el par `Interfaz + Implementación` tanto en casos de uso como en servicios de aplicación, las reglas críticas de negocio (`AlertaValidator` y `EvaluadorCriteriosMatcher`) se prueban unitariamente en milisegundos sin levantar el contexto de Spring ni bases de datos, y cualquier tecnología externa (PostgreSQL, RabbitMQ, Feign o SSE) puede sustituirse cambiando únicamente su adaptador en `infrastructure`.

---

## 3. Diagrama de Clases (Modelos de Dominio y Patrones)

- **Archivo versionado en Draw.io:** [`docs/uml/diagrama-clases.drawio`](./uml/diagrama-clases.drawio)

<p align="center">
  <a href="./images/diagrama-clases.png" target="_blank">
    <img src="./images/diagrama-clases.png" alt="Diagrama de Clases - Modelo de Dominio" width="95%" style="border-radius: 8px; border: 1px solid #CBD5E1; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);" />
  </a>
</p>

### 3.1. Explicación Técnica
Modela las clases del dominio de notificaciones, sus objetos de valor, enumeraciones y los tres patrones de diseño aplicados en el servicio:
1. **Entidades de Dominio (`Alerta`, `Notificacion`, `BusquedaGuardadaCopia`, `EstadoCuentaCopia`, `AlertaCreadaDomainEvent`):**
   - Reemplazan las referencias a entidades JPA del monolito (`Usuario`, `ItemCatalogo`, `BusquedaGuardada`) por **identificadores `UUID` inmutables** (`usuarioId`, `itemId`, `busquedaGuardadaId`) (`NOT-02`).
   - `Notificacion` está generalizada (`NOT-12`) con `alertaId` opcional (`nullable`), `enlace` de navegación, bandera `leida` y soporte para los 5 tipos de `TipoNotificacion` (`STOCK_REPUESTO`, `COINCIDENCIA_BUSQUEDA`, `MENSAJE_NUEVO`, `DONACION_ESTADO`, `MODERACION_PRENDA`).
2. **Value Objects y Enumeraciones (`CriterioBusquedaVO`, `DatosPrendaVO`, `EstadoAlerta`, `TipoAlerta`, `CanalNotificacion`, `TipoNotificacion`, `EstadoCuenta`):**
   - Encapsulan los criterios de filtrado y los atributos del evento de prenda para evaluarlos en memoria sin tocar la base de datos de Catálogo (`NOT-07`).
3. **Patrones Aplicados en el Dominio y Aplicación:**
   - **Cadena de Validaciones y Evaluación en Memoria (`AlertaValidator` + `EvaluadorCriteriosMatcher`):** Valida el origen exclusivo de la alerta (`ITEM_ESPECIFICO` XOR `BUSQUEDA_GUARDADA`), verifica stock cero (`NOT-10`), evalúa coincidencias de categoría, talla, color, marca, precio máximo, condición y donación en memoria (`NOT-07`), y comprueba que la cuenta del usuario esté `ACTIVA` (`NOT-09`).
   - **Patrón Strategy / Puertos de Procesamiento (`ProcesarEventosExternosUseCase` + `Impl`):** Desacopla la estrategia de reacción ante cada tipo de evento de dominio recibido desde RabbitMQ.
   - **Patrón Factory Method (`crearNueva`, `crearPorStockRepuesto`, `crearPorCoincidenciaBusqueda`, `crearGeneral`, `reconstituir`):** Garantiza que ninguna instancia de `Alerta` o `Notificacion` pueda crearse en un estado inválido.

### 3.2. Justificación Arquitectónica
- **Referencias por `UUID` en lugar de Entidades Externas (`NOT-02`):** En una arquitectura de microservicios, importar entidades de `ms-identidad` o `ms-item-catalogo` acoplaría los modelos de dominio. Usar `UUID` junto con los Value Objects `CriterioBusquedaVO` y `DatosPrendaVO` preserva la autonomía total del Bounded Context de Notificaciones.
- **Factory Methods Estáticos Semánticos:** A diferencia de constructores públicos genéricos, métodos como `Notificacion.crearPorStockRepuesto(...)` y `Notificacion.crearGeneral(...)` documentan explícitamente la intención de negocio y asignan automáticamente el tipo, estado inicial (`leida = false`) y marca de tiempo.

---

## 4. Diagrama de Base de Datos (Metamodelo Conceptual y Físico)

- **Archivo versionado en Draw.io:** [`docs/uml/diagrama-base-datos.drawio`](./uml/diagrama-base-datos.drawio)

<p align="center">
  <a href="./images/diagrama-base-datos.png" target="_blank">
    <img src="./images/diagrama-base-datos.png" alt="Diagrama de Base de Datos - Modelo Relacional" width="95%" style="border-radius: 8px; border: 1px solid #CBD5E1; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);" />
  </a>
</p>

### 4.1. Explicación Técnica
Representa el metamodelo de persistencia relacional en **PostgreSQL 16 (`notificaciones_db`)** gestionado mediante migraciones **Flyway (`V1__init_schema.sql` y `V2__add_indexes.sql`)**:
1. **Capa Conceptual (4 Entidades + 2 Bloques Embebidos de Reglas):**
   - `EstadoCuentaCopia` (`/usuario`), `BusquedaGuardadaCopia` (`/busqueda` con composición `1` hacia `CriterioBusqueda`), `Alerta` (`/alerta`) y `Notificacion` (con composición `1` hacia `CicloReposicionSnapshot`).
2. **Capa Física SQL (`SQL Table`):**
   - `estado_cuenta_copia`: Réplica local ligera del estado del usuario (`ACTIVA`, `INACTIVA`, `SUSPENDIDA`, `ELIMINADA`) actualizada por el evento `UsuarioEstadoCambiado`.
   - `busquedas_guardadas_copia`: Réplica local de los filtros de búsqueda guardada del usuario actualizada por `BusquedaGuardadaSincronizada`.
   - `alertas`: Tabla maestra de suscripciones de disponibilidad con control de `ciclo_reposicion_actual`.
   - `notificaciones`: Tabla histórica y de bandeja de entrada con la restricción única compuesta `CONSTRAINT uk_notificacion_alerta_item_ciclo UNIQUE (alerta_id, item_id, ciclo_reposicion)`.

### 4.2. Justificación Arquitectónica
- **Ausencia Deliberada de Llaves Foráneas Externas (`NOT-04`):** Ninguna tabla tiene `FOREIGN KEY` hacia `usuarios` ni `items`, ya que esas tablas residen en servidores de base de datos independientes (`ms-identidad` y `ms-item-catalogo`). Para evitar llamadas HTTP en bucle cada vez que llega un evento de stock, se mantienen las copias locales `busquedas_guardadas_copia` y `estado_cuenta_copia` sincronizadas por eventos (CQRS / Event-Carried State Transfer).
- **Idempotencia en Base de Datos mediante Restricción Única (`NOT-04`, `NOT-11`):** Como RabbitMQ garantiza entrega *At-Least-Once* (un mismo evento `InventarioActualizado` puede llegar duplicado por reintentos de red), la restricción única `(alerta_id, item_id, ciclo_reposicion)` impide físicamente que se dupliquen notificaciones para el mismo ciclo de reposición.

---

## 5. Diagrama de Despliegue (CI/CD, Docker Hub y Ambientes Azure QA/PROD)

- **Archivo versionado en Draw.io:** [`docs/uml/diagrama-despliegue.drawio`](./uml/diagrama-despliegue.drawio)

### 5.1. Explicación Técnica
Describe la topología física de ejecución y el flujo automatizado de integración y entrega continua (`NOT-18`):
1. **Flujo CI/CD (`Dispositivo Desarrollador` -> `GitHub` -> `GitHub Actions Runner`):**
   - El desarrollador sube cambios con `git push` al repositorio `GJ-Notification-Service`.
   - El runner ejecuta `ci-qa.yml` y `cd-prod.yml` (`mvn clean verify`, validación de cobertura JaCoCo >= 85 % y análisis SonarQube).
   - Construye la imagen Docker multi-stage (`eclipse-temurin:21-jre-alpine`, usuario sin privilegios `spring:spring`) y la publica en **Docker Hub Registry** (`styleradar/ms-notificaciones:qa` y `:latest`).
2. **Ambientes `Microsoft Azure — Ambiente QA` y `Microsoft Azure — Ambiente PROD`:**
   - **Azure App Service** descarga la imagen desde Docker Hub y ejecuta el contenedor en el **puerto oficial `8300`**.
   - El contenedor se comunica por **JDBC (`:5432`)** con **Azure Database for PostgreSQL 16 (`notificaciones_db`)**, por **AMQP/AMQPS (`:5672` / `:5671`)** con **RabbitMQ 3.13** (incluyendo la cola principal y la Dead Letter Queue `.dlq`), y por **OpenFeign REST/HTTPS (`:8220`)** con `ms-item-catalogo`.
   - Los clientes externos y el API Gateway consumen el servicio por `HTTPS (443 -> 8300)` tanto para llamadas REST como para el flujo continuo `SSE`.

### 5.2. Justificación Arquitectónica
- **Imagen OCI Multi-Stage Inmutable:** Compilar con Maven en una primera etapa (`jdk-alpine`) y copiar únicamente el `app.jar` a una segunda etapa (`jre-alpine`) reduce la superficie de ataque y disminuye el tamaño de la imagen en más de un 60 %, acelerando el `docker pull` en Azure App Service.
- **Separación Estricta de Entornos QA y PROD:** Permite validar las colecciones automatizadas de Postman (`NOT-19`) y los contratos de eventos en QA antes de promover la misma imagen inmutable a Producción con conexiones cifradas TLS (`JDBC TLS` y `AMQPS`).
