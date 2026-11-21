[← Volver al índice](../../README.md) · [Todos los README](./)


## <img src="https://img.shields.io/badge/08-5600EF?style=for-the-badge" height="20" align="absmiddle" /> 8. VARIABLES DE ENTORNO

Todas las configuraciones sensibles y dependientes de entorno se externalizan mediante variables de sistema conformes al estándar **Twelve-Factor App**.

| Variable | Descripción | Valor por Defecto (`dev`) | Requerido en Prod |
| :--- | :--- | :--- | :---: |
| `PORT` | Puerto de escucha HTTP del microservicio | `8300` | Sí |
| `SPRING_PROFILES_ACTIVE` | Perfil de configuración Spring Boot activo | `dev` | Sí |
| `DB_HOST` | Hostname o IP del servidor PostgreSQL | `localhost` | Sí |
| `DB_PORT` | Puerto de conexión a PostgreSQL | `5432` | Sí |
| `DB_NAME` | Nombre de la base de datos aislada | `notificaciones_db` | Sí |
| `DB_USER` | Usuario de conexión a la base de datos | `styleradar_user` | Sí |
| `DB_PASSWORD` | Contraseña del usuario de base de datos | `styleradar_secret` | Sí |
| `RABBITMQ_HOST` | Hostname del broker de mensajería | `localhost` | Sí |
| `RABBITMQ_PORT` | Puerto AMQP de RabbitMQ | `5672` | Sí |
| `RABBITMQ_USER` | Usuario de conexión a RabbitMQ | `guest` | Sí |
| `RABBITMQ_PASSWORD` | Contraseña de conexión a RabbitMQ | `guest` | Sí |
| `RABBITMQ_VHOST` | Virtual host del broker (solo perfil `prod`) | `/` | No |
| `RABBITMQ_SSL_ENABLED` | Activa TLS hacia el broker; con TLS se usa el puerto `5671` (solo perfil `prod`, donde viene encendido) | `true` en PROD | No |
| `RABBITMQ_SSL_VERIFY_HOSTNAME` | Verifica que el certificado del broker coincida con el host (solo perfil `prod`) | `true` | No |
| `REDIS_HOST` | Hostname del servidor de caché Redis | `localhost` | No |
| `CATALOG_SERVICE_URL` | URL base de `ms-item-catalogo` para OpenFeign | `http://localhost:8220` | Sí |
| `JWT_SECRET` | Clave secreta HMAC-SHA256 (mínimo 256 bits) para validar JWT | `clave_secreta_minimo_256_bits...` | Sí |
| `CORS_ALLOWED_ORIGINS` | Orígenes del frontend permitidos por CORS, separados por comas | `http://localhost:5173` | No |
| `RATE_LIMIT_ENABLED` | Enciende o apaga el límite de solicitudes por usuario (OWASP A06); al pasarse responde `429` con `Retry-After` | `true` | No |
| `RATE_LIMIT_PER_MINUTE` | Solicitudes por minuto y por usuario (por IP si no está autenticado) | `120` | No |
| `SSE_TICKET_TTL_SECONDS` | Vida del ticket con el que se abre el stream de la campana | `60` | No |
| `SWAGGER_ENABLED` | Enciende Swagger UI y `/v3/api-docs` en PROD, donde vienen apagados (OWASP A02) | `false` en PROD | No |
| `SPRING_MAIL_HOST` | Servidor SMTP para los avisos por correo. Sin esta variable no se envían correos (los avisos en plataforma siguen funcionando) | `smtp.ejemplo.co` | No |
| `SPRING_MAIL_PORT` | Puerto del servidor SMTP | `587` | No |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | Credenciales del SMTP. Se inyectan como variables de entorno o secretos; no se guardan en el repositorio | — | No |
| `MAIL_FROM` | Dirección remitente de los correos | `no-reply@styleradar.co` | No |
| `MAIL_PENDING_MAX_ATTEMPTS` | Cuántas veces se reintenta un correo guardado como pendiente antes de marcarlo `FALLIDO` (GJ-119) | `3` | No |
| `MAIL_RETRY_DELAY_MS` | Cada cuántos milisegundos se reintentan los correos pendientes (además de al arrancar el servicio) | `60000` | No |

<details>
<summary><b>Click para copiar la Plantilla de Archivo .env.example</b></summary>

<br>

```dotenv
# ==============================================================================
# STYLERADAR · VARIABLES DE ENTORNO LOCALES (.env) - ms-notificaciones
# ==============================================================================
PORT=8300
SPRING_PROFILES_ACTIVE=dev

# Persistencia PostgreSQL
DB_HOST=localhost
DB_PORT=5432
DB_NAME=notificaciones_db
DB_USER=styleradar_user
DB_PASSWORD=styleradar_secret

# Event Broker RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/
RABBITMQ_SSL_ENABLED=false
RABBITMQ_SSL_VERIFY_HOSTNAME=true
RABBITMQ_EXCHANGE=styleradar.exchange
RABBITMQ_DLX=styleradar.dlx

# Caché Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Cliente Feign Catálogo
CATALOG_SERVICE_URL=http://localhost:8220

# Seguridad JWT
JWT_SECRET=clave_secreta_minimo_256_bits_para_firmado_seguro_hs256_styleradar_2026
JWT_EXPIRATION_MS=86400000
```

</details>
