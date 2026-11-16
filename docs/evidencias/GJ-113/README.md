# GJ-113 · Acceso seguro al broker para otros servicios (usuarios, CA y truststore)

Evidencias de la configuración de seguridad y entrega de certificados TLS (puerto 5671 AMQPS) para el broker RabbitMQ en Azure (`vm-styleradar-rabbitmq`), asegurando el principio de menor privilegio para cada microservicio colaborador.

## Criterios de Aceptación Cumplidos

1. **Usuarios con permisos mínimos y sin roles de administración:**
   * `ms_auth`: Tags `[]` (sin permisos de admin). Permisos acotados a configure/write/read en sus topics `styleradar.auth.*`, `styleradar.exchange` y `styleradar.dlx`.
   * `ms_tiendas`: Tags `[]` (sin permisos de admin). Permisos de escritura limitados exclusivamente al exchange `styleradar.exchange` (para publicar `styleradar.tiendas.fundacion.desactivada`).
2. **Entrega de certificados públicos:**
   * Certificado público raíz: `rabbitmq-ca.pem` (válido hasta 2036, sin clave privada).
   * Almacén de confianza PKCS12 para Java: `rabbitmq-truststore.jks`.
   * Huella SHA-256 pública verificable: `76:D7:98:83:70:39:A5:69:71:A6:22:63:DF:6C:97:B4:C7:12:A5:C6:88:37:9C:A1:5F:32:0C:28:D6:0D:DD:D7`.
3. **No exposición de secretos:**
   * Las contraseñas no se comparten por canales públicos ni commits; se gestionan de forma segregada mediante el documento interno de credenciales.

## Archivos de Evidencia en esta Carpeta

| Archivo | Qué demuestra |
| :--- | :--- |
| `GJ-113_01_usuarios_y_permisos_rabbitmq.png` | Salida real de `rabbitmqctl list_users` y `rabbitmqctl list_permissions -p /` en el contenedor `styleradar-event-broker`, mostrando `ms_auth` y `ms_tiendas` sin tags y con permisos restringidos. |
| `GJ-113_02_mensaje_curso_archivos_adjuntos.png` | Mensaje formal enviado al curso adjuntando los certificados públicos, guía de configuración YAML y huella digital SHA-256 sin filtrar secretos. |
| `rabbitmq-ca.pem` | Certificado público raíz oficial de la CA. |
| `rabbitmq-truststore.jks` | Truststore PKCS12 para proyectos Spring Boot. |
