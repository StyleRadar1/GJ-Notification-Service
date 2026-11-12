# GJ-60 · Marcar como leídas y contador

Cómo se generó: el servicio se levantó en local con la configuración de pruebas (`src/test/resources/application-test.yml`: base H2 en modo PostgreSQL y sin RabbitMQ) y las notificaciones se generaron llamando a los consumidores de eventos (`NotificacionesEventsListener`) con datos de ejemplo, porque en local no hay broker. Las peticiones de las capturas son reales, hechas desde Swagger UI (`/swagger-ui/index.html`) con un JWT de demostración firmado con la clave de pruebas. Los datos son de ejemplo (usuarios 101, 102 y 103).

| Captura | Criterio de aceptación |
| :--- | :--- |
| `GJ-60_01_contador_de_no_leidas.png` | Dadas notificaciones sin leer, el contador devuelve cuántas hay |
| `GJ-60_02_marcar_una_como_leida.png` | Una notificación no leída queda leída |
| `GJ-60_03_el_contador_baja_en_uno.png` | Después de marcarla, el contador baja en 1 |
| `GJ-60_04_marcar_todas_como_leidas.png` | «Marcar todas» deja el contador en 0 |
| `GJ-60_05_403_notificacion_de_otro_usuario.png` | Marcar la notificación de otro usuario responde 403 |
