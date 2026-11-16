# GJ-114 · Evidencias de GJ-60, GJ-61, GJ-63 y GJ-65

Consolidado oficial de evidencias funcionales correspondientes a las historias del Sprint 3 integradas en el código y probadas en `develop` vía PR #14. Cada subconjunto respalda con capturas de Swagger UI, integración con RabbitMQ y suite de pruebas los criterios de aceptación de cada requerimiento.

---

## 1. GJ-60: Marcar como leídas y contador (5 capturas)

| Archivo | Criterio de aceptación validado |
| :--- | :--- |
| `GJ-60_01_contador_de_no_leidas.png` | `GET /contador-no-leidas` retorna el número exacto de notificaciones sin leer. |
| `GJ-60_02_marcar_una_como_leida.png` | `PATCH /{id}/leida` cambia el estado de una notificación a leída. |
| `GJ-60_03_el_contador_baja_en_uno.png` | Tras marcar una como leída, el contador decrementa en 1 de forma consistente. |
| `GJ-60_04_marcar_todas_como_leidas.png` | `PATCH /marcar-todas-leidas` marca todos los avisos y deja el contador en 0. |
| `GJ-60_05_403_notificacion_de_otro_usuario.png` | Intentar marcar una notificación que pertenece a otro usuario retorna HTTP 403 Forbidden. |

---

## 2. GJ-61: Avisos por cada tipo de evento (2 capturas)

| Archivo | Criterio de aceptación validado |
| :--- | :--- |
| `GJ-61_un_aviso_por_cada_tipo.png` | Generación y persistencia de notificaciones diferenciadas por tipo: `MENSAJE_NUEVO`, `CAMBIO_DONACION` (aprobada, rechazada con motivo, cita), `MODERACION_PRENDA`, `SOLICITUD_DONACION` y `PRENDA_VENDIDA`. |
| `GJ-61_lista_de_notificaciones_en_swagger.png` | Respuesta real de Swagger `GET /api/v1/usuarios/me/notificaciones` con payload, títulos y enlaces directos (`/chat/...`, `/donaciones/...`, etc.). |

---

## 3. GJ-63: Publicación del evento AlertaCreada (1 captura + log CI)

| Archivo | Criterio de aceptación validado |
| :--- | :--- |
| `GJ-63_01_publicacion_evento_alerta_creada.png` | Ejecución de `AlertaCreadaPublicacionIntegrationTest` con Testcontainers y RabbitMQ real; publicación hacia `styleradar.notificaciones.alerta.creada` en `styleradar.exchange`. |
| `resultado-ci.txt` | Extracto del log de CI en GitHub Actions confirmando 2/2 pruebas en verde y 0 fallos. |

---

## 4. GJ-65: Aviso de pago confirmado (3 capturas)

| Archivo | Criterio de aceptación validado |
| :--- | :--- |
| `GJ-65_pago_confirmado_con_su_plan.png` | Consumo del evento `PagoConfirmado` generando la notificación con el enlace `/planes/mi-plan`. |
| `GJ-65_pago_repetido_no_se_duplica.png` | Idempotencia: el mismo `eventoId` recibido múltiples veces se procesa una sola vez sin duplicar la notificación en base de datos. |
| `GJ-65_usuario_inexistente_va_a_la_cola_de_errores.png` | Control de integridad: evento recibido para un usuario no registrado en la copia local se rechaza sin reencolar y se deriva a la Dead Letter Queue (`styleradar.notificaciones.dlq`). |
