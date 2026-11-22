[← Volver al índice](../../README.md) · [Todos los README](./)


## <img src="https://img.shields.io/badge/01-5600EF?style=for-the-badge" height="20" align="absmiddle" /> 1. NOMBRE DEL SERVICIO Y SU BOUNDED CONTEXT

* **Identificador de Servicio:** `ms-Notifications` (`GJ-Notification-Service`)
* **Paquete Arquitectónico:** `Paquete 3: Notificaciones y Chat`
* **Puerto de Escucha Oficial:** `8300` (definido en la matriz de puertos sin colisión)
* **Base de Datos Asignada:** `PostgreSQL 16` (`notificaciones_db`), aislada bajo el patrón *Database-per-Service*.
* **Bounded Context:** `Notificaciones, Alertas de Disponibilidad y Tiempo Real`

### Responsabilidades del Dominio

Este microservicio encapsula exclusivamente la lógica de negocio correspondiente a `Notificaciones, Alertas de Disponibilidad y Tiempo Real`.

| Qué HACE este Microservicio (Alcance)                             | Qué NO HACE este Microservicio (Límites)                                          |
| :----------------------------------------------------------------- | :---------------------------------------------------------------------------------- |
| • Gestiona alertas de prendas agotadas (`ITEM_AGOTADO`) y búsquedas guardadas (`BUSQUEDA_GUARDADA`) validando stock contra `ms-ItemCatalog`. | • NO autentica credenciales ni emite tokens iniciales (delegado a `ms-Auth` en `:8100`). |
| • Evalúa en memoria coincidencias entre eventos (`InventarioActualizado`, `PrendaPublicada`) y copias locales de búsquedas guardadas sin hacer SQL cruzado. | • NO gestiona transacciones monetarias ni pasarelas (delegado a `ms-Payment` en `:8400`). |
| • Sincroniza copias locales de búsquedas guardadas y estados de cuenta (`ACTIVO`, `INACTIVO`, `BLOQUEADO`, `SUSPENDIDO`) mediante eventos asíncronos. | • NO persiste conversaciones ni historial de mensajes STOMP (delegado a `ms-Chat` en `:8320`). |
| • Entrega notificaciones generalizadas (stock, mensajes, donaciones, moderación, solicitudes de donación, prendas vendidas, pagos confirmados y fundaciones desactivadas), las empuja en tiempo real por Server-Sent Events (SSE) con ticket de 60 s y reconexión (`Last-Event-ID`), y las envía además por correo si el usuario lo eligió por tipo de aviso o si la alerta se configuró con canal `EMAIL` o `AMBOS`. | • NO accede directamente a tablas ni bases de datos de otros microservicios (cero llaves foráneas externas). |
| • Garantiza persistencia ACID e idempotencia mediante las restricciones únicas `(alerta_id, item_id, ciclo_reposicion)` y `(evento_id, usuario_id)`, y desvío de fallos a Dead Letter Queue (`SR-RNF-15`). | • NO modifica el inventario ni el catálogo maestro de prendas. |
