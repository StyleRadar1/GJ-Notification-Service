# GJ-92 · Publicar imagen y desplegar en QA

Cómo se generó: el microservicio `ms-notificaciones` se desplegó en Azure App Service (`styleradar-notificaciones-qa`) enlazado a la base de datos PostgreSQL y al contenedor RabbitMQ 3.13.7 en la máquina virtual `vm-styleradar-rabbitmq` (puerto 5671 AMQPS con TLS). El pipeline de integración continua corre en GitHub Actions con compilación Java 21, 124 pruebas unitarias/integración, verificación de Docker Compose y análisis estático con CodeQL.

| Archivo | Qué muestra / Criterio de aceptación |
| :--- | :--- |
| `GJ-92_01_actuator_health_qa.png` | Respuesta real HTTP 200 OK de `/actuator/health` en QA con base de datos (`db: UP`) y broker (`rabbit: UP`, versión 3.13.7) |
| `GJ-92_02_pipeline_github_actions_codeql_en_verde.png` | Pipeline de GitHub Actions (`ci-qa.yml`) y análisis estático de seguridad (`codeql.yml`) ambos con ejecución exitosa en verde |
| `GJ-92_03_swagger_ui_qa.png` | Interfaz interactiva de Swagger UI desplegada y operativa en Azure QA sobre el host `styleradar-notificaciones-qa.azurewebsites.net` |
| `resultado-health.json` | Payload JSON crudo del estado de salud de todos los componentes del microservicio en QA |

## URLs Oficiales del Entorno de QA

* **Base URL:** `https://styleradar-notificaciones-qa.azurewebsites.net`
* **Swagger UI:** `https://styleradar-notificaciones-qa.azurewebsites.net/swagger-ui/index.html`
* **Health Check:** `https://styleradar-notificaciones-qa.azurewebsites.net/actuator/health`
* **Imagen en Registro OCI:** `acrstyleradarqa.azurecr.io/styleradar/ms-notificaciones:qa`
