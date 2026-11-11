# Tabla de pruebas de ms-notificaciones (GJ-36 y GJ-51)

## Aclaración sobre el origen de las pruebas

Las tareas GJ-36 y GJ-51 pedían una tabla de equivalencia entre las 7 pruebas HTTP y del validador y las 33 pruebas de persistencia y transacciones del monolito, y las pruebas nuevas. Se confirmó que **no existe un repositorio del monolito del que salgan esas pruebas**: no hay un repositorio de ese tipo en la organización de GitHub y las pruebas no están en `GJ-Notification-Service`. Por eso no hay una columna "prueba original" que se pueda enlazar.

Esta tabla reemplaza a la de equivalencia: lista **todas las pruebas que tiene hoy el servicio**, agrupadas por lo que verifican y por la tarea de Jira que respaldan. Si el equipo consigue las pruebas originales, se agrega una columna con su correspondencia.

## Resumen

| Dato | Valor |
| :--- | :--- |
| Pruebas | 67 (33 unitarias y 34 de integración) |
| Cobertura de líneas (JaCoCo) | 91,6 %, con un mínimo exigido de 85 % en `mvn verify` |
| Pruebas con RabbitMQ real (Testcontainers) | 9; se omiten sin Docker y las ejecuta el CI |

Ruta de las pruebas: `src/test/java/edu/dosw/styleradar/notificaciones/`.

## Pruebas unitarias (`unit/`)

| Clase | Pruebas | Qué verifica | Tarea |
| :--- | :---: | :--- | :--- |
| `domain/AlertaValidatorAndModelTest` | 8 | Alerta válida, incompleta, con cuenta inactiva, con combinaciones excluyentes, ítem con stock, búsqueda guardada activa, duplicados, propiedad ajena y modelos de dominio | GJ-31, GJ-36 |
| `domain/EvaluadorCriteriosMatcherTest` | 3 | Una prenda coincide con todos los criterios, no coincide si falla alguno, coincidencia de ítem agotado | GJ-46 |
| `application/NotificacionesUseCasesTest` | 7 | Crear alerta de ítem y de búsqueda, rechazo por stock o duplicado, listar y desactivar, lectura de notificaciones, procesar disponibilidad con idempotencia y estado de cuenta, sincronizar copias | GJ-32, GJ-36 |
| `infrastructure/NotificacionesEventsListenerTest` | 7 | Reposición se procesa; stock previo o sin cambio se ignora; payload sin datos o con estado desconocido es inválido; prenda retirada | GJ-45, GJ-47, GJ-48 |
| `config/RabbitRetryConfigTest` | 4 | Fallo transitorio se intenta 3 veces; payload inválido e ilegible no se reintentan; un fallo que se recupera no se rinde | GJ-50 |
| `config/RabbitMqConfigTest` | 1 | La cola de prendas retiradas está enlazada y con cola de errores | GJ-47 |
| `config/JwtSecretConfigurationTest` | 3 | El servicio exige `JWT_SECRET` fuera del perfil de desarrollo y usa el del entorno | GJ-35 |

## Pruebas de integración (`integration/`)

| Clase | Pruebas | Qué verifica | Tarea |
| :--- | :---: | :--- | :--- |
| `adapter/in/rest/AlertaControllerAndInfrastructureTest` | 4 | Flujo HTTP con JWT: 201, 409, listado, 403, desactivación, 400, 422, 404, notificaciones y lectura; consumidores y cola de errores | GJ-34, GJ-36 |
| `adapter/in/rest/SeguridadJwtHttpTest` | 5 | 401 sin token, vencido, con otra firma y manipulado; 403 con recursos de otro usuario | GJ-35 |
| `adapter/in/rest/CatalogoNoDisponibleHttpTest` | 1 | Si Catálogo no responde, 503 y no se crea la alerta | GJ-49 |
| `adapter/in/rest/SwaggerUiRoutesTest` | 3 | Swagger UI y el contrato OpenAPI se sirven sin token | GJ-25 |
| `adapter/in/rest/ActuatorSecurityTest` | 3 | `health` es público; `metrics` e `info` exigen autenticación | GJ-35 |
| `adapter/in/rest/RutaInexistenteTest` | 1 | Una ruta inexistente responde 404 | GJ-34 |
| `adapter/out/CatalogoClientAdapterWireMockTest` | 5 | Cliente de Catálogo con WireMock: agotado, con stock, inexistente, servicio caído y conexión cortada | GJ-49 |
| `adapter/out/NotificacionesConcurrenciaTest` | 3 | Entregas simultáneas generan una sola notificación; evento repetido no duplica y un ciclo nuevo sí; el perdedor de la carrera descarta sin fallar | GJ-33, GJ-50, GJ-51 |
| `messaging/RabbitMqEventosIntegrationTest` | 9 | Con RabbitMQ real: reposición, evento que no repone, reintentos y cola de errores, payload inválido e ilegible, prenda retirada, cuenta inactiva y reactivada, prenda nueva con búsqueda guardada, búsqueda eliminada | GJ-45, GJ-47, GJ-48, GJ-50, GJ-51 |

## Cómo se ejecutan

```bash
mvn verify
```

Genera el reporte de JaCoCo en `target/site/jacoco/index.html` y falla si la cobertura baja de 85 %.
