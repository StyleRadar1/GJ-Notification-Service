# GJ-112 · Diagramas (clases, BD, componentes) y README al 100 %

Criterio de aceptación y evidencia: los 6 diagramas de arquitectura en formato oficial Style-Radar (con sus editables en `.drawio` y sus exportaciones en `.png`) y el `README.md` actualizado al 100% con la cobertura de pruebas (93%), stack tecnológico (Java 21 y PostgreSQL 16), puertos (8300) y variables de entorno.

## Estado del Pull Request de Evidencia

* **Pull Request:** [PR #18 · docs: diagramas, README y explicacion de diagramas del sprint 3 hacia develop](https://github.com/StyleRadar1/GJ-Notification-Service/pull/18)
* **Estado:** **FUSIONADO (MERGED)** por Paula Díaz (`paulisdiar`) en `develop`.

## Los 6 Diagramas Entregados

| Diagrama | Archivo PNG | Archivo Editable Draw.io | Qué representa |
| :--- | :--- | :--- | :--- |
| **1. Clases** | `diagrama-clases.png` | `docs/uml/diagrama-clases.drawio` | Arquitectura hexagonal: puertos in/out, casos de uso, adaptadores y entidades de dominio. |
| **2. Base de Datos** | `diagrama-base-datos.png` | `docs/uml/diagrama-base-datos.drawio` | Modelo relacional PostgreSQL 16 (`notificaciones`, `alertas`, `preferencias_avisos`, `busquedas_guardadas_copia`). |
| **3. Componentes específico** | `diagrama-componentes-especifico.png` | `docs/uml/diagrama-componentes-especifico.drawio` | Componentes internos de Spring Boot: controladores REST, stream SSE, listeners AMQP, adaptadores y repositorios JPA. |
| **4. Contexto** | `diagrama-contexto.png` | `docs/uml/diagrama-contexto.drawio` | Diagrama C4 de contexto con los actores (comprador, almacén, fundación) y microservicios colaboradores. |
| **5. Despliegue** | `diagrama-despliegue.png` | `docs/uml/diagrama-despliegue.drawio` | Topología de Azure: App Service Linux (puerto 8300), PostgreSQL Flexible Server, VM RabbitMQ (puerto 5671 AMQPS) y Azure Container Registry. |
| **6. Secuencia** | `diagrama-secuencia.png` | `docs/uml/diagrama-secuencia.drawio` | Flujo reactivo e idempotente: Recepción de evento en RabbitMQ -> Evaluación de alertas -> Persistencia -> Emisión SSE en vivo y despacho SMTP. |

Los archivos fuente editables se encuentran en la carpeta [`docs/uml/`](../../uml/) del repositorio.
