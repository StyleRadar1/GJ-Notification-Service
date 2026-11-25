[← Volver al índice](../../README.md) · [Todos los README](./)


## <img src="https://img.shields.io/badge/11-5600EF?style=for-the-badge" height="20" align="absmiddle" /> 11. SEGURIDAD, HABEAS DATA Y ANÁLISIS OWASP TOP 10

Esta sección describe los controles de seguridad que **están implementados** en `ms-Notifications`, los datos personales que trata y lo que **todavía falta**. Es una revisión del equipo basada en el código y las pruebas del repositorio: **no es una certificación OWASP ni un dictamen jurídico**, y no garantiza ausencia de vulnerabilidades.

### 11.1 Controles implementados

| Área | Control | Dónde se ve |
| :--- | :--- | :--- |
| **Autenticación** | `ms-Notifications` no emite tokens: valida el JWT de `ms-Auth` (firma `HS256` con clave compartida en `JWT_SECRET`, vencimiento `exp`). El servicio no arranca sin esa clave. Sin token, vencido, con otra firma o manipulado responde `401`. | [Contrato JWT](../CONTRATO_JWT_AUTENTICACION.md), `SeguridadJwtHttpTest`, `JwtSecretConfigurationTest` |
| **Autorización por usuario** | El `{id}` de la ruta debe coincidir con el usuario del token; si no coincide responde `403`, salvo el rol administrador de StyleRadar. El alias `me` no se acepta (`403`). Una notificación o alerta de otro usuario no se puede ver, marcar ni eliminar. | `SeguridadJwtHttpTest`, `AlertaControllerAndInfrastructureTest` |
| **Ticket SSE de corta duración** | El navegador no puede enviar cabeceras con `EventSource`, así que el stream se abre con un ticket de **60 segundos** que solo sirve para ese stream. Un JWT de sesión en `?ticket=` o `?access_token=` se rechaza: el JWT nunca viaja en la URL. | `TicketStreamHttpTest`, [sección 3](03-endpoints-y-swagger.md) |
| **Límite de solicitudes (429)** | Ventana de un minuto: **120 solicitudes por usuario** (`RATE_LIMIT_PER_MINUTE`); lo que llega sin autenticar se cuenta por IP. Al pasarse responde `429`. | `LimiteDeSolicitudesHttpTest`, `RateLimitFilter` |
| **Swagger apagado en PROD** | `SWAGGER_ENABLED=false` por defecto en el perfil de producción: no se exponen `/swagger-ui` ni `/v3/api-docs`. | `application-prod.yml` |
| **Métricas protegidas** | `/actuator/metrics/**` y `/actuator/prometheus` solo para administradores; `/actuator/health` es público para el chequeo de Azure. | `SecurityConfig`, `MetricasActuatorHttpTest` |
| **CORS y CSRF** | Con `CORS_ALLOWED_ORIGINS` vacío (QA y PROD) las cabeceras CORS las emite solo el API Gateway. La API es sin estado (JWT en cabecera, sin cookies ni sesión), por lo que se exime de CSRF únicamente `/api/**`. | `SecurityConfig`, `CorsHttpTest` |
| **Mensajería** | Conexión TLS (AMQPS, puerto 5671) al broker central; el certificado raíz de StyleRadar se importa en la imagen. Un payload inválido va directo a la cola de errores sin reintentos. | `Dockerfile`, [sección 4](04-eventos.md) |
| **Errores** | Las respuestas de error usan `ProblemDetail` con códigos de error estables. | `CodigosDeErrorHttpTest` |
| **Cadena de suministro** | Dependabot revisa cada semana Maven, GitHub Actions y la imagen base; CodeQL analiza el código en cada PR a `develop` y cada lunes. Esto detecta problemas, **no garantiza cero vulnerabilidades**. | `.github/dependabot.yml`, `.github/workflows/codeql.yml` |
| **Imagen** | Se ejecuta con un usuario sin privilegios (`appuser`) y con la misma versión de Java que el resto del ciclo (Java 21). | `Dockerfile` |

### 11.2 Habeas Data: datos personales que trata el servicio

El tratamiento de datos personales en Colombia se rige por la **Ley 1581 de 2012** (Habeas Data). El servicio no pide datos al usuario por su cuenta: recibe del resto de la plataforma lo mínimo para avisarle, y el consentimiento y la autorización del titular se gestionan en `ms-Users`.

| Dato | Dónde se guarda | Para qué | Qué pasa al eliminar la cuenta |
| :--- | :--- | :--- | :--- |
| Identificador del usuario | Alertas, notificaciones, preferencias, búsquedas | Saber a quién avisar | Se borra todo lo asociado |
| Correo electrónico | `estados_cuenta_copia` (copia del estado de cuenta) y `correos_pendientes` (destinatario, asunto y cuerpo de los reintentos) | Enviar avisos por correo | Se borran los correos pendientes y el correo de la copia queda vacío |
| Contenido de las notificaciones | `notificaciones` (título, mensaje y enlace; puede incluir la vista previa de un mensaje de chat o el nombre de una prenda) | Mostrar la bandeja | Se borran |
| Alertas, búsquedas guardadas y preferencias de aviso | `alertas`, `busquedas_guardadas_copia`, `preferencias_avisos` | Detectar coincidencias y elegir el canal | Se borran |

Cómo se atienden los derechos del titular hoy:

- **Supresión:** cuando `ms-Users` elimina una cuenta publica `styleradar.usuarios.cuenta.eliminada`. El servicio borra los datos de arriba, cierra la conexión en tiempo real del usuario y deja su cuenta marcada como `ELIMINADA` sin correo, para que un evento tardío no le genere avisos (SR-RF-107).
- **Eliminación por el propio usuario:** puede eliminar una, varias o todas sus notificaciones leídas (SR-RF-108). Es un borrado lógico: la fila se conserva sin mostrarse.
- **Acceso:** el usuario consulta sus propias alertas, notificaciones y preferencias con la API.
- **Minimización:** el evento `AlertaCreada` que consume `ms-Statistics` lleva identificadores (alerta, usuario, prenda), no el correo ni el contenido de los mensajes.

### 11.3 Mapa OWASP Top 10 (2021)

| Categoría | Estado | Qué hay y qué falta |
| :--- | :---: | :--- |
| **A01** Control de acceso roto | Implementado | `{id}` contra el token, comprobación de propietario en cada alerta y notificación, métricas solo para administradores. |
| **A02** Fallos criptográficos | Parcial | Firma `HS256` con clave por variable de entorno y TLS al broker; HTTPS lo termina Azure App Service. Falta verificar cifrado en reposo de la base de datos (depende de Azure). |
| **A03** Inyección | Implementado | JPA con consultas parametrizadas y validación de entrada en los DTO (`@Valid`, `@Size`, `@Positive`). |
| **A04** Diseño inseguro | Parcial | Límite de solicitudes, ticket SSE de 60 s e idempotencia de eventos con cola de errores. Sin modelado de amenazas formal. |
| **A05** Configuración incorrecta | Parcial | Swagger apagado en PROD, CORS delegado al Gateway, secretos por variables de entorno. Pendiente rotar las credenciales del broker y restringir su acceso de administración. |
| **A06** Componentes vulnerables | Parcial | Dependabot y CodeQL activos. No hay escaneo de la imagen de contenedor. |
| **A07** Fallos de autenticación | Parcial | Se valida firma y vencimiento; la emisión, el cierre de sesión y la protección de credenciales son de `ms-Auth`. |
| **A08** Integridad de software y datos | Parcial | Payloads de eventos validados y CI con pruebas obligatorias. Sin firma de artefactos. |
| **A09** Registro y monitoreo | Parcial | Métricas Prometheus protegidas y registros con ids de usuario y evento (en el código revisado no se registran tokens). No hay auditoría centralizada ni alertas de seguridad. |
| **A10** SSRF | Implementado | La única llamada HTTP saliente es a `ms-ItemCatalog` con una URL fija de configuración (`CATALOG_SERVICE_URL`); no se acepta ninguna URL del usuario. |

### 11.4 Riesgos y pendientes

- **Retención de datos:** no hay purga automática. Las notificaciones eliminadas por el usuario se conservan con su fecha de eliminación y los datos del servicio no tienen un plazo máximo definido.
- **Derecho de acceso completo:** no existe una exportación de todos los datos de un usuario; solo se pueden consultar por la API.
- **Clave compartida `HS256`:** cualquier servicio que conozca `JWT_SECRET` podría firmar tokens. Pendiente definir la rotación de la clave y evaluar una firma asimétrica junto con `ms-Auth`.
- **Credenciales del broker:** rotar la contraseña administrativa de RabbitMQ y cerrar el acceso SSH de su red (tarea operativa, fuera de este repositorio).
- **Alertas de CodeQL:** revisar en la pestaña Security que la alerta de CSRF quede cerrada tras exceptuar solo `/api/**`.
- **Registros y base de datos:** falta una auditoría formal de logs y confirmar el cifrado en reposo y las copias de seguridad.
- **Pruebas de seguridad:** no se ha hecho una prueba de penetración independiente.
- **Redis:** `docker-compose.yml` levanta Redis, pero el servicio no lo usa (SSE en memoria). Mientras no se decida retirarlo, se documenta como dependencia local no usada.
