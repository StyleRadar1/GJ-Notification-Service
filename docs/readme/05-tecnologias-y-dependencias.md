[← Volver al índice](../../README.md) · [Todos los README](./)


## <img src="https://img.shields.io/badge/05-5600EF?style=for-the-badge" height="20" align="absmiddle" /> 5. TECNOLOGÍAS Y DEPENDENCIAS

El stack del microservicio cumple con el estándar unificado de arquitectura de StyleRadar:

| Capa / Componente | Tecnología | Versión | Justificación Técnica |
| :--- | :--- | :--- | :--- |
| **Lenguaje Core** | Java LTS | `21` | Records inmutables, Pattern Matching y alto rendimiento en procesamiento de eventos. |
| **Construcción** | Apache Maven | `3.9+` | Compila, prueba y empaqueta (`mvn verify`). El repositorio no incluye Maven Wrapper (`mvnw`): se usa el `mvn` instalado, el mismo que usan CI, CD y el Dockerfile (imagen `maven:3.9-eclipse-temurin-21`). |
| **Framework Empresarial** | Spring Boot | `3.5.16` | Gestión declarativa de configuración, ecosistema nativo Spring Web, Security, Data JPA y AMQP. |
| **Cliente HTTP Declarativo** | Spring Cloud OpenFeign | `2025.0.3` (Spring Cloud) | Validación síncrona de prenda agotada contra `ms-ItemCatalog` (NOT-10). |
| **Base de Datos** | PostgreSQL | `16` | Integridad transaccional ACID y restricción única `(alerta_id, item_id, ciclo_reposicion)`. |
| **Migraciones de BD** | Flyway (`flyway-core` y `flyway-database-postgresql`) | `11.x` (gestionada por Spring Boot) | Control de versiones reproducible de scripts SQL (`V1` esquema, `V2` índices, `V3` `evento_id` de idempotencia, `V4` `preferencias_avisos`, `V5` `correos_pendientes`, `V6` `item_id` como `VARCHAR(100)`). |
| **Broker de Mensajería** | RabbitMQ | `3.13` | Desacoplamiento temporal de microservicios mediante Topic Exchanges y Dead Letter Queues. |
| **Caché en Memoria** | Redis | `7.x` | Está en el `pom.xml` y en `docker-compose`, pero el servicio no lo usa: QA y PROD lo desactivan, el SSE vive en memoria y los correos pendientes van a PostgreSQL. |
| **Correo** | Spring Boot Starter Mail (`JavaMailSender`) | `3.5.16` | Avisos por correo cuando el usuario lo elige. Se activa con `SPRING_MAIL_HOST`; sin él no se envía correo. |
| **Seguridad** | Spring Security + JJWT | `0.11.5` | JWT HS256 sin estado; la clave se comparte con `ms-Auth`. Límite de solicitudes por usuario (`429`). |
| **Documentación de API** | springdoc-openapi | `2.8.17` | OpenAPI 3 y Swagger UI en inglés. |
| **Observabilidad** | Spring Boot Actuator + Micrometer (Prometheus) | `3.5.16` | `/actuator/health` público y métricas propias protegidas (campanas conectadas, correos pendientes, respuestas `429`); ver [docs/observabilidad](../observabilidad/README.md). |
| **Mapeo de DTOs** | MapStruct + Lombok | `1.6.3` · `1.18.48` | Generación de mappers (DTO en `application`, entidades JPA en `infrastructure`) en tiempo de compilación sin sobrecarga de reflexión. |
| **Testing Core & Web** | JUnit 5 + Mockito + MockMvc | `5.12.x` | Pruebas unitarias de dominio/casos de uso y pruebas de integración REST/AMQP/JPA con H2 en modo PostgreSQL. |
| **Pruebas de integración** | Testcontainers · WireMock · GreenMail | `1.21.4` · `3.13.2` · `2.1.15` | RabbitMQ y PostgreSQL reales, Catálogo simulado y servidor SMTP de prueba. |
| **Métricas de Cobertura** | JaCoCo Plugin | `0.8.16` | Verificación estricta de cobertura de líneas mínima (`>= 85%`) como regla obligatoria en `mvn verify`. |

---

> [!NOTE]
> **Documento Oficial de Análisis y Justificación Tecnológica:** Para consultar el análisis comparativo formal de alternativas evaluadas y descartadas (PostgreSQL vs NoSQL, RabbitMQ vs Kafka, Java/Spring vs Node/Python, SSE vs WebSockets, JavaMailSender vs SDKs propietarios), visita:
> **[docs/ANALISIS_DECISIONES_TECNOLOGICAS.md](../ANALISIS_DECISIONES_TECNOLOGICAS.md)**.
