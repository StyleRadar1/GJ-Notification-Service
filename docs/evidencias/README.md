# Evidencias del Sprint 3

Capturas y resultados que respaldan los criterios de aceptación de las historias GJ-60 a GJ-65 de ms-Notifications. Cada carpeta tiene su propio `README.md` que explica qué muestra cada archivo y cómo se generó.

| Carpeta | Tarea |
| :--- | :--- |
| `GJ-60` | Marcar como leídas y contador |
| `GJ-61` | Avisos por cada tipo de evento |
| `GJ-62` | Campana en tiempo real con SSE |
| `GJ-63` | Publicar AlertaCreada |
| `GJ-64` | Preferencias de canal y envío por correo |
| `GJ-65` | Aviso de pago confirmado |
| `GJ-92` | Publicar imagen y desplegar en QA |
| `GJ-111` | Ficha técnica para la matriz Backend–Frontend |
| `GJ-112` | Diagramas (clases, BD, componentes) y README al 100 % |
| `GJ-113` | Acceso seguro al broker para otros servicios (usuarios, CA y truststore) |
| `GJ-114` | Evidencias de GJ-60, GJ-61, GJ-63 y GJ-65 |
| `GJ-115` | Confirmar con otros equipos los eventos de donación, venta y pago |
| `GJ-116` | Poner la URL real de QA en el README |
| `GJ-117` | Avisar cuando una fundación se desactiva |
| `GJ-118` | Paginación de la bandeja de notificaciones |
| `GJ-119` | Reintentos de correo que sobreviven a un reinicio |

## Evidencias transversales

Las imágenes de esta carpeta que respaldan el README principal son capturas o reportes reales, con la fecha en que se obtuvieron:

| Archivo | Qué muestra | Origen |
| :--- | :--- | :--- |
| `swagger-ui.png` | Los 14 endpoints agrupados en «Notification Preferences» y «Alerts & Notifications» | Swagger UI de `ms-Notifications` |
| `cobertura-jacoco.png` | Reporte HTML de JaCoCo con 94 % de instrucciones y 94,0 % de líneas | `target/site/jacoco/index.html` tras `mvn clean verify` (20/11/2026) |
| `pipeline-ci-cd.png` | Ejecución en verde del pipeline CI / QA sobre `develop` (`build-and-verify` y `docker-verify-qa`) | Página pública de GitHub Actions (20/11/2026) |
| `codeql-analysis.png` | Ejecución en verde del análisis CodeQL sobre `develop` | Página pública de GitHub Actions (20/11/2026) |
| `health-qa.png` | Respuesta de `/actuator/health` del servicio desplegado en QA | `https://styleradar-notificaciones-qa.azurewebsites.net/actuator/health` (20/11/2026) |
| `jira-tablero-epica.png` | Las 46 historias y tareas de la épica GJ-2 en estado «Listo» | Consulta `project = GJ AND parent = GJ-2` en Jira (20/11/2026) |
