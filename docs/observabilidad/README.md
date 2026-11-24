# Observabilidad de ms-Notifications

Metricas y alertas del servicio (OWASP A09, registro y alertas).

## Donde se leen

| Ruta | Quien puede leerla | Que trae |
| :--- | :--- | :--- |
| `/actuator/health` | Publica (la usa el health check de Azure) | Estado del servicio, la base de datos y el broker |
| `/actuator/prometheus` | Solo JWT con rol `ADMIN` o `ADMIN_STYLERADAR` | Todas las metricas en formato Prometheus |
| `/actuator/metrics` y `/actuator/metrics/{nombre}` | Solo `ADMIN` o `ADMIN_STYLERADAR` | Las mismas metricas, una por una, en JSON |
| `/actuator/info` | Cualquier usuario autenticado | Informacion de la aplicacion |

Sin token responden 401; con un usuario sin rol de administrador, 403. Prometheus puede leerlas con
`authorization: credentials: <JWT de administrador>` en su `scrape_config`.

## Metricas propias

| Metrica | Tipo | Para que sirve |
| :--- | :--- | :--- |
| `notificaciones_sse_conexiones_activas` | Gauge | Campanas conectadas en esta instancia |
| `notificaciones_correos_pendientes` | Gauge | Correos que no se pudieron enviar y esperan un reintento (cuenta hasta 1.000) |
| `notificaciones_ratelimit_rechazadas_total` | Contador | Solicitudes rechazadas con 429 por pasar el limite por minuto |
| `spring_rabbitmq_listener_seconds_*` | Timer | Mensajes procesados por cada cola, con la etiqueta `error` cuando fallan |
| `http_server_requests_seconds_*`, `jvm_*`, `hikaricp_*` | Estandar | Peticiones HTTP, memoria y conexiones a la base de datos |

## Alertas

Las reglas estan en [`alertas-prometheus.yml`](alertas-prometheus.yml): servicio caido, eventos con error,
correos pendientes, exceso de respuestas 429, errores 5xx y memoria alta. Se cargan en Prometheus
(`rule_files`), en Grafana Alerting o en Azure Managed Prometheus.

## Variables relacionadas

| Variable | Por defecto | Efecto |
| :--- | :--- | :--- |
| `RATE_LIMIT_ENABLED` | `true` | Enciende o apaga el limite de solicitudes por usuario |
| `RATE_LIMIT_PER_MINUTE` | `120` | Solicitudes por minuto y por usuario (o por IP si no esta autenticado) |
| `SSE_TICKET_TTL_SECONDS` | `60` | Vida del ticket con el que se abre el stream de la campana |
| `SWAGGER_ENABLED` | `false` en PROD | Enciende Swagger y `/v3/api-docs` en PROD |
| `RABBITMQ_SSL_ENABLED` | `true` en PROD | TLS hacia RabbitMQ |
