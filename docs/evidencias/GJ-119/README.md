# GJ-119 · Reintentos de correo que sobreviven a un reinicio

## Descripción y Arquitectura
Persistencia de correos salientes en la base de datos relacional para garantizar que los intentos de envío pendientes o fallidos por indisponibilidad transitoria del servidor SMTP sobrevivan a reinicios de contenedor o caídas del microservicio.

## Componentes Técnicos
1. **Migración Flyway V5:** Creación de la tabla `correos_pendientes` con estados `PENDIENTE`, `EN_PROCESO`, `ENVIADO`, `FALLIDO` y contador de `intentos`.
2. **Componente de Recuperación (`CorreoPendienteRecuperador`):**
   * Tarea programada o al inicio de la aplicación que consulta registros en estado `PENDIENTE` o con fallos transitorios.
   * Ejecuta reintentos controlados con retroceso exponencial.
3. **Idempotencia:** Evita envíos duplicados mediante bloqueo pesimista/optimista al seleccionar lotes para procesamiento.

## Implementación y Pruebas
* **Branch:** `feature/backlog-notificaciones-gj117-118-119` (PR #26 hacia `develop`).
* **Pruebas de Integración:** `CorreoPendienteRecuperadorTest` con servidor SMTP GreenMail simulando caídas y recuperaciones de conexión.
