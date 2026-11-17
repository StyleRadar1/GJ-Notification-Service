# GJ-64 · Preferencias de canal y envío por correo

Cómo se generó: el servicio se levantó en local con la configuración de pruebas (`src/test/resources/application-test.yml`: base H2 en modo PostgreSQL y sin RabbitMQ) y las notificaciones se generaron llamando a los consumidores de eventos (`NotificacionesEventsListener`) con datos de ejemplo, porque en local no hay broker. Las peticiones de las capturas son reales, hechas desde Swagger UI (`/swagger-ui/index.html`) con un JWT de demostración firmado con la clave de pruebas. Los datos son de ejemplo (usuarios 101, 102 y 103).

El correo se envió a un servidor SMTP de prueba local (GreenMail, puerto 3025). **No es un buzón real**: la captura del correo recibido en un buzón real queda pendiente de un servidor SMTP configurado.

| Captura | Criterio de aceptación |
| :--- | :--- |
| `GJ-64_01_preferencias_por_defecto_solo_plataforma.png` | Sin preferencias guardadas se aplica «solo plataforma» en los 9 tipos |
| `GJ-64_02_elegir_correo_y_plataforma.png` | El usuario elige «correo y plataforma» para un tipo de aviso |
| `GJ-64_03_correo_recibido.png` | Se genera un aviso de ese tipo y llega el correo (remitente, destinatario, asunto y cuerpo) además del aviso en la plataforma |
| `GJ-64_04_elegir_solo_plataforma.png` | El usuario cambia a «solo plataforma» |
| `GJ-64_05_solo_plataforma_no_envia_correo.png` | Con «solo plataforma» el aviso se crea en la campana y no llega ningún correo nuevo |

El caso de fallo del servidor de correo (la notificación se crea igual y el correo se reintenta) lo cubren las pruebas `CorreoServidorCaidoTest` y `CorreoSmtpAdapterTest`.
