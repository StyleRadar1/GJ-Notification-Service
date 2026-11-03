# Documento de Análisis y Justificación de Decisiones Tecnológicas
## Microservicio de Notificaciones y Alertas (`ms-notificaciones` · Puerto 8088)
### Proyecto StyleRadar · Asignatura: DOSW 2026 · Squad: Guatoque Jiraguas (GJ)

---

## 1. Resumen Ejecutivo y Criterios Globales de Selección

El **Microservicio de Notificaciones y Alertas (`ms-notificaciones`)** es el componente del ecosistema StyleRadar encargado de:
1. Gestionar las alertas de prendas agotadas (`ITEM_AGOTADO`) y búsquedas guardadas (`BUSQUEDA_GUARDADA`) configuradas por los usuarios.
2. Procesar de forma asíncrona los eventos emitidos por otros dominios (catálogo, inventario, usuarios, donaciones, pagos y chat).
3. Entregar notificaciones multicanal (campana in-app en tiempo real mediante SSE y correos electrónicos transaccionales).

Para seleccionar el stack tecnológico, el equipo **Guatoque Jiraguas (GJ)** evaluó cada componente bajo cuatro criterios rectores de arquitectura:
* **Consistencia e Integridad:** Evitar notificaciones duplicadas, spam o condiciones de carrera en la reposición de stock.
* **Desacoplamiento Estricto:** Cumplir con los principios de *Database-per-Service* y arquitectura hexagonal sin dependencias circulares ni SQL cruzado.
* **Rendimiento y Tolerancia a Fallos:** Garantizar baja latencia en la entrega de notificaciones y aislamiento de caídas de otros microservicios mediante Dead Letter Queues (DLQ).
* **Estandarización Corporativa:** Mantener plena compatibilidad con las directrices tecnológicas del proyecto StyleRadar.

---

## 2. Matriz Resumen de Decisiones Tecnológicas

| Componente | Tecnología Seleccionada | Alternativas Descartadas | Decisión Clave ("Por qué esta y no otra") |
| :--- | :--- | :--- | :--- |
| **Base de Datos** | **PostgreSQL 16 (Relacional)** | MongoDB, DynamoDB, Redis standalone | Integridad transaccional ACID, restricción de unicidad compuesta `(alerta_id, item_id, ciclo_reposicion)` e índices parciales para campana no leída. |
| **Broker de Mensajería** | **RabbitMQ 3.13** | Apache Kafka, AWS SQS, Redis Pub/Sub | Enrutamiento flexible por topics (`TopicExchange`), reintentos y desvío nativo a Dead Letter Queues (DLQ), y bajo consumo de memoria. |
| **Lenguaje Core** | **Java 21 LTS** | Java 17, Node.js, Python, Go | Soporte corporativo LTS, Records inmutables, Pattern Matching para eventos de dominio y Virtual Threads para I/O escalable. |
| **Framework Base** | **Spring Boot 3.2.5** | Quarkus, Micronaut, Express, FastAPI | Estándar unificado de StyleRadar, ecosistema maduro de Spring Data, Spring AMQP, OpenFeign, Spring Security y Actuator. |
| **Tiempo Real (Campana)** | **Server-Sent Events (SSE)** | WebSockets (STOMP), Short Polling, Long Polling | Flujo unidireccional servidor hacia cliente, reconexión automática nativa (`EventSource`), menor sobrecarga y soporte nativo HTTP. |
| **Notificación por Email** | **JavaMailSender (SMTP)** | SendGrid SDK directo, Mailgun SDK, AWS SES SDK | Abstracción agnóstica de Spring que evita el *vendor lock-in*, soporte de plantillas HTML y desacoplamiento del proveedor en QA/PROD. |

---

## 3. Justificación Detallada por Componente

### 3.1. Base de Datos: ¿Usa BD? Tipo Relacional con PostgreSQL 16

#### Pregunta: ¿Usa Base de Datos?
**Sí.** El microservicio requiere persistencia propia e independiente (*Database-per-Service*) para almacenar el estado de las alertas de los usuarios, el historial de notificaciones recibidas, las copias sincronizadas de búsquedas guardadas y las preferencias de canales de aviso.

#### Tipo Seleccionado: Relacional (RDBMS) - PostgreSQL 16
Tablas principales gestionadas por el servicio:
1. `alertas`: Registro de alertas activas/desactivadas por usuario, item y talla.
2. `notificaciones`: Historial de avisos entregados, estado de lectura y fecha de expiración.
3. `preferencias_avisos`: Configuración de canales (IN_APP, EMAIL) por usuario.
4. `busquedas_guardadas_copia`: Copia local sincronizada por eventos del catálogo/usuarios.
5. `estados_cuenta_copia`: Cache persistente del estado de cuenta (`ACTIVO`, `BLOQUEADO`).

#### Justificación Técnica ("Por qué PostgreSQL y no otra"):
1. **Garantía ACID Estricta e Idempotencia:** 
   El caso de uso más crítico del servicio es notificar la reposición de una prenda agotada (`NOT-10`). Si una tienda actualiza el stock varias veces en pocos segundos, una base de datos relacional garantiza mediante una restricción única compuesta:
   ```sql
   CONSTRAINT uq_alerta_item_ciclo UNIQUE (alerta_id, item_id, ciclo_reposicion)
   ```
   que el usuario reciba **exactamente una notificación por ciclo de reposición**, eliminando cualquier riesgo de spam o duplicidad por concurrencia.
2. **Índices Parciales para la Campana en Tiempo Real:**
   La consulta más frecuente de la interfaz de usuario es `GET /api/v1/usuarios/me/notificaciones/no-leidas/contador`. PostgreSQL permite definir índices parciales:
   ```sql
   CREATE INDEX idx_notificaciones_usuario_no_leidas 
   ON notificaciones (usuario_id) 
   WHERE leida = FALSE;
   ```
   Este índice solo indexa las filas no leídas, reduciendo el tamaño del índice en disco en más del 80% y permitiendo lecturas en memoria en menos de 2 milisegundos.
3. **Soporte Híbrido Relacional + JSONB:**
   Las búsquedas guardadas contienen criterios de filtrado dinámicos (color, precio mínimo, precio máximo, talla, estilo). PostgreSQL permite almacenar estos criterios en columnas de tipo `JSONB`, combinando la integridad relacional de la tabla con la flexibilidad de atributos dinámicos sin requerir una base NoSQL separada.
4. **Control de Versiones y Migraciones con Flyway:**
   PostgreSQL se integra nativamente con Flyway, permitiendo que cada cambio en el esquema esté auditado mediante scripts reproducibles (`V1__init_schema.sql`, `V2__add_indexes.sql`), garantizando entornos idénticos en DEV, QA y PROD.

#### Alternativas Evaluadas y Descartadas:
* **MongoDB (NoSQL Documental):** Descartado porque no ofrece integridad referencial ni restricciones de unicidad compuesta entre colecciones con la misma simplicidad y rigidez de SQL. Requeriría implementar lógica de bloqueo distribuido a nivel de aplicación para evitar condiciones de carrera en la lectura y actualización de alertas.
* **Amazon DynamoDB / Apache Cassandra:** Descartados por sobrecosto operativo, complejidad innecesaria en el modelado de índices secundarios y dependencias propietarias de nube.
* **Redis como Base de Datos Principal:** Descartado porque Redis es primariamente un almacén clave-valor en memoria. Aunque ofrece persistencia RDB/AOF, no está diseñado para consultas relacionales complejas ni auditoría histórica duradera de notificaciones. Se utiliza únicamente como caché complementaria.

---

### 3.2. Broker de Mensajería: RabbitMQ 3.13

#### Propósito:
Consumir eventos asíncronos emitidos por los microservicios de StyleRadar:
* `styleradar.inventario.actualizado` (Inventario / Catálogo)
* `styleradar.catalogo.prenda.publicada` (Catálogo de Prendas)
* `styleradar.catalogo.prenda.retirada` (Catálogo de Prendas)
* `styleradar.usuarios.busqueda.sincronizada` (Usuarios)
* `styleradar.usuarios.estado.cambiado` (Identidad / Auth)
* `styleradar.chat.mensaje.enviado` (Chat de Segunda Mano)
* `styleradar.donaciones.estado.cambiado` (Donaciones)
* `styleradar.catalogo.moderacion.cambiada` (Moderación)

#### Justificación Técnica ("Por qué RabbitMQ y no otra"):
1. **Modelo de Enrutamiento flexible con Topic Exchanges:**
   RabbitMQ utiliza el exchange `styleradar.exchange` de tipo `Topic`. Esto permite desacoplar totalmente al emisor del receptor. El servicio de notificaciones se suscribe mediante patrones de routing key (ej. `styleradar.inventario.*`, `styleradar.catalogo.#`) vinculando colas durables exclusivas para el dominio de notificaciones.
2. **Manejo Nativo de Dead Letter Queues (DLQ) para Resiliencia (`SR-RNF-15`):**
   Si llega un mensaje con formato corrupto, payload incompatible o falla la conexión transitoria con la base de datos, RabbitMQ redirige el mensaje tras fallar a un Dead Letter Exchange (`styleradar.dlx`) y lo encola en `styleradar.notificaciones.dlq`. Esto garantiza que un mensaje defectuoso (*poison pill*) nunca bloquee el procesamiento de los demás eventos.
3. **Consumo Ligero de Recursos y Simplicidad Operativa:**
   En entornos locales y contenedores OCI de prueba, RabbitMQ consume ~80 MB a 120 MB de memoria RAM y no requiere clusters de coordinación adicionales (como ZooKeeper o KRaft).
4. **Garantía de Entrega At-Least-Once con ACKs Automáticos/Manuales:**
   Spring AMQP confirma el procesamiento del mensaje solo cuando la transacción local de base de datos se completa con éxito (`COMMIT`). Si ocurre un fallo inesperado, el mensaje se reintenta o se envía a la DLQ.

#### Alternativas Evaluadas y Descartadas:
* **Apache Kafka:** Descartado por sobreingeniería (*overkill*). Kafka está concebido como una plataforma de streaming de registros distribuidos de alta retención para millones de eventos por segundo (análisis masivo, telemetría, Big Data). En StyleRadar, el objetivo es procesar tareas puntuales de notificación; RabbitMQ ofrece colas transaccionales, enrutamiento por topics y DLQ nativas con una fracción de la complejidad operativa de Kafka.
* **AWS SQS / Azure Service Bus:** Descartados para prevenir el *vendor lock-in* con proveedores de nube y permitir que la misma arquitectura corra de forma transparente en máquinas de desarrollo locales, pipelines de GitHub Actions y servidores de producción.
* **Redis Pub/Sub:** Descartado porque opera bajo el principio *fire-and-forget* (no tiene colas durables). Si el microservicio de notificaciones se reinicia o se encuentra en mantenimiento, todos los eventos emitidos durante ese lapso se perderían irreversiblemente.

---

### 3.3. Lenguaje y Framework: Java 21 LTS + Spring Boot 3.2.5

#### Justificación Técnica ("Por qué Java 21 y Spring Boot 3.2"):
1. **Alineación con el Estándar StyleRadar:**
   Todos los microservicios backend de StyleRadar (squads Portal8080, CADCHE, RST, NQS y Guatoque Jiraguas) están homologados en Java 21 y Spring Boot 3.2, garantizando coherencia en dependencias, herramientas de CI/CD y despliegue.
2. **Características Modernas de Java 21 LTS:**
   * **Java Records:** Utilizados para definir DTOs inmutables de entrada y salida (`CrearAlertaRequest`, `AlertaResponse`, `NotificacionResponse`) y payloads de eventos AMQP sin necesidad de boilerplate ni código mutador.
   * **Pattern Matching y Switch Expressions:** Utilizados en `EvaluadorCriteriosMatcher` y en los manejadores de eventos para clasificar tipos de notificación de manera exhaustiva y segura en tiempo de compilación.
   * **Virtual Threads (Project Loom):** Permite manejar miles de conexiones I/O concurrentes (consultas JDBC y suscripciones SSE abiertas) sin agotar el pool de hilos del sistema operativo.
3. **Ecosistema Spring Boot 3.2 Integrado:**
   * **Spring Web MVC:** Controladores REST limpios desacoplados mediante interfaces de documentación (`AlertaApi`).
   * **Spring Data JPA & Hibernate 6:** Gestión de transacciones declarativas (`@Transactional`), mapeo entidad-relacional y prevención del problema N+1.
   * **Spring Cloud OpenFeign:** Cliente HTTP declarativo para consultar la disponibilidad de stock en `ms-item-catalogo` (`GET /api/v1/catalogo/items/{id}/disponibilidad`) con timeout configurable.
   * **Spring Security 6:** Filtro stateless que valida la firma criptográfica HS256 del token JWT en cada petición sin consultar bases de datos externas.
   * **Springdoc OpenAPI 2.5:** Generación automática de contratos OpenAPI 3.0 en `/v3/api-docs` e interfaz Swagger en `/swagger-ui/index.html`.

#### Alternativas Evaluadas y Descartadas:
* **Node.js con Express / NestJS:** Descartado porque JavaScript/TypeScript carece de tipado estricto en tiempo de ejecución. Adicionalmente, el ecosistema de ORMs en Node.js (Prisma, TypeORM) no ofrece el mismo nivel de madurez en control de transacciones y optimizaciones de consultas que JPA/Hibernate.
* **Python con FastAPI:** Descartado debido a que la implementación de Arquitectura Hexagonal pura y Domain-Driven Design (DDD) en Python carece de un marco estándar unificado de inyección de dependencias como Spring BeanConfig y MapStruct, dificultando el mantenimiento de contratos corporativos.
* **Go (Golang):** Descartado por la mayor curva de desarrollo para modelar patrones de diseño ricos en el dominio (Chain of Responsibility, Strategy y Factory) y menor soporte de abstracciones integradas para AMQP y migraciones automáticas.

---

### 3.4. Mecanismo de Notificación en Tiempo Real: Server-Sent Events (SSE) para la Campana

#### Propósito:
Actualizar en tiempo real el ícono de la campana en la barra de navegación del frontend (incremento del contador de no leídas y recepción de alertas emergentes) a través del endpoint `GET /api/v1/usuarios/me/notificaciones/stream`.

#### Justificación Técnica ("Por qué SSE y no otra"):
1. **Flujo de Datos Unidireccional (Servidor -> Cliente):**
   La campana de notificaciones solo necesita que el servidor envíe eventos hacia el navegador cuando ocurra una novedad. El cliente **no necesita enviar datos de vuelta por ese canal**; cuando el usuario lee una notificación, realiza una petición estándar `PATCH /api/v1/usuarios/me/notificaciones/{id}/leer`.
2. **Reconexión Automática Nativa con `EventSource`:**
   El estándar HTML5 incluye la API nativa del navegador `EventSource`, la cual maneja automáticamente la pérdida de señal, reintentos con *backoff* exponencial y propagación del encabezado `Last-Event-ID` para recuperar eventos perdidos sin código JavaScript complejo.
3. **Compatibilidad Total con HTTP/1.1 y HTTP/2:**
   SSE opera sobre conexiones HTTP estándar con MIME `text/event-stream`. Atraviesa sin inconvenientes balanceadores de carga, firewalls corporativos, proxies reversos y API Gateways (como Spring Cloud Gateway en puerto 8080) que suelen cerrar o tener problemas con WebSockets.
4. **Mínimo Consumo de Memoria:**
   En Spring Boot se gestiona mediante `SseEmitter`. Un emitter inactivo solo retiene un puntero en memoria sin requerir hilos dedicados del servidor web gracias a la arquitectura no bloqueante.

#### Alternativas Evaluadas y Descartadas:
* **WebSockets con STOMP:** Descartado para la campana de notificaciones. WebSockets es un protocolo bidireccional y persistente (*stateful*), indispensable para aplicaciones de interacción cruzada como el chat de segunda mano (`ms-chat` en puerto 8089). Utilizar WebSockets para una simple campana de alertas introduce sobrecarga innecesaria de memoria, complejidad en el handshake, gestión de sesiones y dificultades de balanceo en el API Gateway.
* **Short Polling (Peticiones cada N segundos):** Descartado por ineficiencia crítica. Enviar peticiones HTTP cada 5 segundos desde miles de clientes saturaría la red y la base de datos, donde el 99% de las respuestas serían arrays vacíos o códigos `304 Not Modified`.
* **Long Polling:** Descartado porque mantiene un hilo bloqueado por cliente esperando una respuesta y obliga a reiniciar la conexión HTTP con todo el overhead de cabeceras en cada notificación recibida.

---

### 3.5. Notificaciones Externas: JavaMailSender para Correo Electrónico

#### Propósito:
Enviar correos electrónicos transaccionales para alertas de alta prioridad (ej. reposición de prenda esperada, cambio crítico en el estado de moderación o alerta de seguridad).

#### Justificación Técnica ("Por qué JavaMailSender y no otra"):
1. **Abstracción Agnóstica de Proveedor (Cero Vendor Lock-in):**
   `JavaMailSender` (proporcionado por `spring-boot-starter-mail`) es una interfaz agnóstica que desacopla el código Java del proveedor SMTP subyacente. Permite utilizar:
   * **Desarrollo / Pruebas:** Contenedores de emulación SMTP (MailHog o GreenMail) sin enviar correos reales ni requerir conexión a internet.
   * **QA:** Servidor SMTP de pruebas o SendGrid en sandbox.
   * **Producción:** Servicios corporativos de alta entregabilidad (Azure Communication Services, Amazon SES o SendGrid).
   El cambio de proveedor se realiza **únicamente mediante variables de entorno** (`SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`), sin tocar una sola línea de código.
2. **Soporte de Mensajes MIME Enriquecidos y Plantillas:**
   Permite estructurar correos visuales con HTML responsive, hojas de estilo corporativas de StyleRadar y caracteres UTF-8 mediante `MimeMessageHelper`.
3. **Ejecución Asíncrona Desacoplada (`@Async` / Eventos):**
   El envío de correos tiene una latencia típica de 500 ms a 2000 ms. Al integrarse con Spring, el envío se ejecuta en hilos secundarios desacoplados, impidiendo que una demora en el servidor de correo degrade la respuesta REST hacia el usuario.

#### Alternativas Evaluadas y Descartadas:
* **SDK Propietario Directo (ej. SendGrid Java SDK, Mailgun API Client):** Descartado porque acopla el código a una API propietaria, rompiendo los principios de la Arquitectura Hexagonal y obligando a reescribir adaptadores si la organización decide cambiar de proveedor de correo.
* **Llamadas HTTP Síncronas Manuales:** Descartado por carecer de pooling de conexiones, reintentos automáticos y soporte estructurado para codificación MIME multipart.

---

## 4. Conclusión Técnica

La arquitectura tecnológica adoptada para el **Microservicio de Notificaciones y Alertas (`ms-notificaciones`)** garantiza el equilibrio óptimo entre:
1. **Persistencia robusta e idempotente** con PostgreSQL 16 para evitar fallos de negocio (duplicación de alertas).
2. **Comunicación reactiva asíncrona** con RabbitMQ 3.13 para tolerar caídas de servicios externos sin pérdida de eventos mediante DLQ.
3. **Entrega ágil en tiempo real** con Server-Sent Events (SSE), minimizando la carga en el servidor y optimizando la experiencia del usuario.
4. **Mantenibilidad y desacoplamiento** gracias a Java 21, Spring Boot 3.2 y Arquitectura Hexagonal pura.

---

*Documento versionado oficialmente en el repositorio del proyecto: `docs/ANALISIS_DECISIONES_TECNOLOGICAS.md`.*
*Equipo Guatoque Jiraguas (GJ) · Asignatura DOSW 2026.*

