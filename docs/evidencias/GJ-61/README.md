# GJ-61 · Avisos por cada tipo de evento

Cómo se generó: el servicio se levantó en local con la configuración de pruebas (`src/test/resources/application-test.yml`: base H2 en modo PostgreSQL y sin RabbitMQ) y las notificaciones se generaron llamando a los consumidores de eventos (`NotificacionesEventsListener`) con datos de ejemplo, porque en local no hay broker. Las peticiones de las capturas son reales, hechas desde Swagger UI (`/swagger-ui/index.html`) con un JWT de demostración firmado con la clave de pruebas. Los datos son de ejemplo (usuarios 101, 102 y 103).

| Captura | Qué muestra |
| :--- | :--- |
| `GJ-61_un_aviso_por_cada_tipo.png` | Un aviso por cada evento, con su texto y su enlace: mensaje nuevo, donación aprobada, rechazada con motivo y con cita programada, moderación de prenda, solicitud de donación y prenda vendida |
| `GJ-61_lista_de_notificaciones_en_swagger.png` | La respuesta real de `GET /api/v1/usuarios/me/notificaciones` con todos los tipos |
