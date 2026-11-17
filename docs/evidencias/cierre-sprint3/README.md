# Cierre del Sprint 3: pendientes del review de avance

Respuesta a lo que el review de avance del 16 de noviembre de 2026 dejó en «Parcial» para ms-notificaciones.

| Pendiente del review | Qué se hizo | Evidencia |
| :--- | :--- | :--- |
| **A02**: Swagger y `/v3/api-docs` públicos en PROD | `application-prod.yml` los apaga por defecto (`SWAGGER_ENABLED=false`); se encienden con la variable. Las métricas (`/actuator/metrics`, `/actuator/prometheus`) exigen rol `ADMIN` o `ADMIN_STYLERADAR`. | `ConfiguracionProduccionTest`, `MetricasActuatorHttpTest` |
| **A03**: sin análisis de dependencias ni Dependabot | `.github/dependabot.yml` (Maven, GitHub Actions y Docker, cada lunes) y el workflow `dependency-review.yml`, que bloquea en cada PR las dependencias con vulnerabilidades altas o críticas. CodeQL ya estaba. | Archivos en `.github/` |
| **A04**: el TLS de RabbitMQ en PROD dependía de una variable que valía `false` | `RABBITMQ_SSL_ENABLED` vale `true` por defecto en `application-prod.yml`; el pipeline de PROD también lo fija en `true`. | `ConfiguracionProduccionTest.tlsEncendidoPorDefecto` |
| **A06**: sin límite de solicitudes por usuario | Filtro de ventana de un minuto: 120 solicitudes por usuario (por IP si no está autenticado). Al pasarse responde 429 con `ProblemDetail` y `Retry-After`. Variables `RATE_LIMIT_ENABLED` y `RATE_LIMIT_PER_MINUTE`. | `RateLimitFilterTest` (6), `LimiteDeSolicitudesHttpTest` |
| **A07**: decidir el PR #32 (token en la URL) | Se descarta el JWT de sesión en la URL. Ahora `POST /api/v1/users/{id}/notifications/stream-ticket` entrega un ticket de 60 s que solo abre el stream (`?ticket=`). Un JWT de sesión en la URL se rechaza y el ticket no abre ningún otro endpoint ni sirve en la cabecera. | `TicketStreamHttpTest` (9) |
| **A09**: sin métricas ni alertas | Prometheus en `/actuator/prometheus`, con métricas propias (campanas conectadas, correos pendientes, solicitudes rechazadas con 429) y las de los consumidores de RabbitMQ. Reglas de alerta en `docs/observabilidad/alertas-prometheus.yml`. | `MetricasActuatorHttpTest`, `docs/observabilidad/` |
| **Despliegue en PROD** | El pipeline de PROD de `main` seguía en una versión vieja que iniciaba sesión en Docker Hub; el de `develop` ya publica en el registro de Azure como QA. Al fusionar `develop` en `main` se despliega el Sprint 3. El origen CORS de PROD sale de la variable de repositorio `CORS_ALLOWED_ORIGINS_PROD`. | PR de release `develop` → `main` |
| **Fichas de requerimientos** | Rutas actualizadas a `/api/v1/users/{id}/...`, filtros `leida`, `page` y `size`, y el flujo del ticket del stream. | `Requerimientos_GJ_Notificaciones_y_Chat_FINAL` (docx y PDF) |
| **Ficha técnica Backend–Frontend** | Versión 4 con las rutas canónicas, la paginación, el ticket, el 403 por `{id}`, el 429 y los ids UUID. | `ms-notificaciones_v4` (docx y PDF) |

## Resultado de `mvn clean verify`

- 206 pruebas, 0 fallos, 11 omitidas (las de RabbitMQ real, que corren en el CI con Docker).
- Cobertura de líneas de 93,1 %, con un mínimo exigido de 85 %.

## Para el frontend

El `EventSource` nativo ya sirve sin librerías adicionales:

1. `POST /api/v1/users/me/notifications/stream-ticket` con `Authorization: Bearer <jwt>` devuelve `{ "ticket": "...", "expiraEnSegundos": 60 }`.
2. `new EventSource("/api/v1/users/me/notifications/stream?ticket=" + ticket)`.
3. Al reconectar, pedir un ticket nuevo: el anterior dura 60 segundos.

El stream también sigue aceptando `Authorization` para clientes que envían cabeceras (por ejemplo `fetch-event-source`).
