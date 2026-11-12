# GJ-62 · Campana en tiempo real con SSE

Cómo se generó: el servicio se levantó en local con la configuración de pruebas (`src/test/resources/application-test.yml`: base H2 en modo PostgreSQL y sin RabbitMQ) y las notificaciones se generaron llamando a los consumidores de eventos (`NotificacionesEventsListener`) con datos de ejemplo, porque en local no hay broker. Las peticiones de las capturas son reales, hechas desde Swagger UI (`/swagger-ui/index.html`) con un JWT de demostración firmado con la clave de pruebas. Los datos son de ejemplo (usuarios 101, 102 y 103).

Además se usó una página de demostración servida en `http://localhost:5173` (origen permitido por CORS) que lee el stream con `fetch` y la cabecera `Authorization`, porque el `EventSource` nativo del navegador no permite enviarla. La página no forma parte del repositorio.

| Archivo | Qué muestra |
| :--- | :--- |
| `GJ-62_campana_en_vivo_y_reconexion.gif` | Los 7 cuadros siguientes en secuencia |
| `GJ-62_f1_conectada.png` | Conexión abierta (evento `connected`) |
| `GJ-62_f2_llega_una_notificacion_en_vivo.png` y `GJ-62_f3_llega_otra.png` | Cada notificación nueva llega a la campana apenas se crea, con su id de evento |
| `GJ-62_f4_conexion_cortada.png` y `GJ-62_f5_desconectado_el_contador_no_cambia.png` | Se corta la conexión y llegan 2 notificaciones mientras el usuario está desconectado |
| `GJ-62_f6_reconecta_y_recibe_lo_perdido.png` | Al reconectar con `Last-Event-ID` (ver el registro) se reciben, en orden, las 2 notificaciones perdidas |
| `GJ-62_f7_de_nuevo_en_vivo.png` | Después de reconectar, las nuevas siguen llegando en vivo |

## Falta el video

Esta carpeta tiene un GIF, no un video. Para grabar el video final (con el servicio levantado y un JWT válido):

1. Abre el Swagger y autoriza con un JWT.
2. Conecta un cliente al stream: `curl -N -H "Authorization: Bearer $TOKEN" http://localhost:8300/api/v1/usuarios/me/notificaciones/stream`.
3. Genera un aviso y muestra que llega al instante.
4. Corta el cliente, genera dos avisos más y reconecta con la cabecera `Last-Event-ID` con el último id recibido: llegan los dos que se perdieron.
