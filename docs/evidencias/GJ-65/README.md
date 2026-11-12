# GJ-65 · Aviso de pago confirmado

Cómo se generó: el servicio se levantó en local con la configuración de pruebas (`src/test/resources/application-test.yml`: base H2 en modo PostgreSQL y sin RabbitMQ) y las notificaciones se generaron llamando a los consumidores de eventos (`NotificacionesEventsListener`) con datos de ejemplo, porque en local no hay broker. Las peticiones de las capturas son reales, hechas desde Swagger UI (`/swagger-ui/index.html`) con un JWT de demostración firmado con la clave de pruebas. Los datos son de ejemplo (usuarios 101, 102 y 103).

| Captura | Criterio de aceptación |
| :--- | :--- |
| `GJ-65_pago_confirmado_con_su_plan.png` | Al llegar `PagoConfirmado` se crea una notificación con el plan y el enlace `/planes/mi-plan` |
| `GJ-65_pago_repetido_no_se_duplica.png` | El mismo evento recibido dos veces genera una sola notificación (usuario 102) |
| `GJ-65_usuario_inexistente_va_a_la_cola_de_errores.png` | Un pago de un usuario que no existe en la copia local se rechaza sin reencolar y va a la cola de errores |
