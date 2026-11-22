# GJ-117 · Avisar cuando una fundación se desactiva

## 1. Contexto y Origen Técnico
* **Microservicio Emisor:** `ms-tiendas` (`ms-stores`, Puerto `8140`).
* **Operación Emisora:** `PATCH /api/v1/admin/fundaciones/{id}/desactivar` (requiere `ROLE_ADMIN_STYLERADAR`).
* **Exchange:** `styleradar.exchange` (`topic`, durable).
* **Routing Key:** `styleradar.tiendas.fundacion.desactivada`.
* **Cola Consumidora en `ms-notificaciones`:** `styleradar.notificaciones.fundacion-desactivada.q`.
* **Dead Letter Exchange / Queue:** `styleradar.dlx` $\rightarrow$ `styleradar.notificaciones.dlq`.

---

## 2. Contrato del Evento (`FundacionDesactivadaEventPayload`)

```json
{
  "eventoId": "evt-fund-9901",
  "fundacionId": 3,
  "nombreFundacion": "Fundacion Abrigo Solidario",
  "donantesAfectadosIds": [101, 102],
  "motivo": "Desactivacion administrativa por cambio de sede",
  "fechaEvento": "2026-11-18T10:15:00"
}
```

---

## 3. Capturas de Evidencia en esta Carpeta (Criterios de Aceptación)

| Archivo de Captura | Criterio de Aceptación que Demuestra |
| :--- | :--- |
| **`GJ-117_01_notificacion_fundacion_desactivada.png`** | **Criterio 1 (Notificación a donantes afectados):** Al llegar el evento `styleradar.tiendas.fundacion.desactivada`, cada donante en `donantesAfectadosIds` recibe la notificación tipo `CAMBIO_DONACION` con título `"Fundacion desactivada"`, el motivo y enlace directo a sus donaciones (`/donaciones`). |
| **`GJ-117_02_evento_repetido_no_se_duplica.png`** | **Criterio 2 (Idempotencia ante evento repetido):** Al publicar dos veces el mismo `eventoId` (`evt-fund-9901`), la consulta `GET /api/v1/users/101/notifications` devuelve **una única notificación** gracias a la restricción única compuesta `UNIQUE (evento_id, usuario_id)` (`uk_notificaciones_evento_usuario`). |
| **`GJ-117_03_evento_invalido_va_a_la_cola_de_errores.png`** | **Criterio 3 (Desvío a DLQ ante evento inválido):** Si el payload llega nulo, sin `fundacionId` o sin `eventoId`, `NotificacionesEventsListener` lanza `AmqpRejectAndDontRequeueException` y envía el mensaje sin reintentos a la cola de errores `styleradar.notificaciones.dlq` (`SR-RNF-15`). |
| **`GJ-117_04_pruebas_consumidor_en_verde.png`** | **Pruebas del consumidor en verde:** Ejecución de las 8 pruebas unitarias y de integración (`ConsumidoresEventosExternosTest` y `NotificacionesEventsListenerTest`) verificando recepción, cuentas inactivas, no duplicación y envío a DLQ (`BUILD SUCCESS`, cobertura JaCoCo 94.0%). |

---

## 4. Archivos de Código y Pruebas
* **DTO del Evento:** `src/main/java/edu/dosw/styleradar/notificaciones/application/dto/event/FundacionDesactivadaEventPayload.java`
* **Consumidor AMQP:** `src/main/java/edu/dosw/styleradar/notificaciones/infrastructure/adapter/in/amqp/NotificacionesEventsListener.java` (`onFundacionDesactivada`)
* **Configuración de Cola y DLQ:** `src/main/java/edu/dosw/styleradar/notificaciones/infrastructure/config/RabbitMqConfig.java` (`foundationDeactivatedQueue`)
* **Pruebas de Integración:** `src/test/java/edu/dosw/styleradar/notificaciones/integration/messaging/ConsumidoresEventosExternosTest.java`
* **Pruebas Unitarias:** `src/test/java/edu/dosw/styleradar/notificaciones/unit/infrastructure/NotificacionesEventsListenerTest.java`
* **Pull Requests:** [PR #26](https://github.com/StyleRadar1/GJ-Notification-Service/pull/26) (fusionado a `develop`) y [PR #33](https://github.com/StyleRadar1/GJ-Notification-Service/pull/33).
