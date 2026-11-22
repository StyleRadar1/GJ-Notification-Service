# GJ-118 · Paginación de la bandeja de notificaciones

## 1. Descripción Funcional y Acuerdo con Frontend
Implementación de paginación con metadatos (`PaginaResponse<NotificacionResponse>`) para el endpoint de la bandeja desplegable de la campana:
* **Endpoint Canónico:** `GET /api/v1/users/{id}/notifications`
* **Parámetros Expuestos en Swagger:**
  * `page` (`Integer`, por defecto `0`, mínimo `0`).
  * `size` (`Integer`, por defecto `20`, mínimo `1`, máximo `100`).
  * `read` (`Boolean`, opcional: `false` = no leídas, `true` = leídas).
* **Acuerdo de Contrato con Frontend:**
  * Cuando se envían parámetros de paginación/filtro (`page`, `size` o `read`), devuelve el objeto paginado con `content`, `page`, `size`, `totalElements`, `totalPages`, `hasNext` y `hasPrevious`.
  * Si se omite `size` al paginar (ej. `?page=0`), se aplica el tamaño por defecto razonable **`size = 20`**.
  * El contador de la burbuja morada (`GET /api/v1/users/{id}/notifications/unread/count`) y el stream en tiempo real (`GET /api/v1/users/{id}/notifications/stream`) permanecen sin cambios.

---

## 2. Capturas de Evidencia en esta Carpeta (Criterios de Aceptación)

| Archivo de Captura | Criterio de Aceptación que Demuestra |
| :--- | :--- |
| **`GJ-118_01_swagger_respuesta_paginada_page_size.png`** | **Criterio 1 (Respuesta paginada en Swagger):** Consulta `GET /api/v1/users/101/notifications?page=0&size=2&read=false` en Swagger UI devolviendo `200 OK` con `content`, `page`, `size`, `totalElements`, `totalPages`, `hasNext` y `hasPrevious`. |
| **`GJ-118_02_tamano_por_defecto_y_validacion_limites_400.png`** | **Criterio 2 (Tamaño por defecto `size=20` y validación `400`):** Demuestra que sin `size` se aplica `TAMANO_POR_DEFECTO = 20`, y que valores inválidos (`size > 100`, `size < 1` o `page < 0`) se rechazan con `400 Bad Request` (`ProblemDetail`). |
| **`GJ-118_03_contador_burbuja_y_stream_sse_intactos.png`** | **Criterio 3 (El stream SSE y el contador no cambian):** Verifica que `GET /api/v1/users/101/notifications/unread/count` y `GET /api/v1/users/101/notifications/stream` mantienen su contrato intacto. |
| **`GJ-118_04_pruebas_integracion_paginacion_en_verde.png`** | **Pruebas automatizadas en verde:** Ejecución de la suite de pruebas de `GJ-118` en `AlertaControllerAndInfrastructureTest` y `NotificacionesUseCasesTest` pasando al 100% (`BUILD SUCCESS`, cobertura JaCoCo 94.0%). |

---

## 3. Archivos de Código y Pull Request
* **DTO de Página:** `src/main/java/edu/dosw/styleradar/notificaciones/application/dto/response/PaginaResponse.java`
* **Modelo de Dominio:** `src/main/java/edu/dosw/styleradar/notificaciones/domain/model/Pagina.java`
* **Controlador y Contrato OpenAPI:** `AlertaController.java` y `AlertaApi.java` (`listarMisNotificaciones`)
* **Pruebas de Integración:** `src/test/java/edu/dosw/styleradar/notificaciones/integration/adapter/in/rest/AlertaControllerAndInfrastructureTest.java` (`bandejaPaginaConMetadatos`, `bandejaFiltraPorLeidaYMantieneLaListaCompleta`, `bandejaRechazaParametrosDePaginaInvalidos`)
* **Pull Requests:** [PR #26](https://github.com/StyleRadar1/GJ-Notification-Service/pull/26) (fusionado a `develop`) y [PR #33](https://github.com/StyleRadar1/GJ-Notification-Service/pull/33).
