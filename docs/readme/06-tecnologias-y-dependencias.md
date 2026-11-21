[← Volver al índice](../../README.md) · [Todos los README](./)


## <img src="https://img.shields.io/badge/06-5600EF?style=for-the-badge" height="20" align="absmiddle" /> 6. TECNOLOGÍAS Y DEPENDENCIAS

El stack del microservicio cumple con el estándar unificado de arquitectura de StyleRadar:

| Capa / Componente | Tecnología | Versión | Justificación Técnica |
| :--- | :--- | :--- | :--- |
| **Lenguaje Core** | Java LTS | `21` | Records inmutables, Pattern Matching y alto rendimiento en procesamiento de eventos. |
| **Framework Empresarial** | Spring Boot | `3.2.5` | Gestión declarativa de configuración, ecosistema nativo Spring Web, Security, Data JPA y AMQP. |
| **Cliente HTTP Declarativo** | Spring Cloud OpenFeign | `2023.0.1` | Validación síncrona de prenda agotada contra `ms-item-catalogo` (NOT-10). |
| **Base de Datos** | PostgreSQL | `16` | Integridad transaccional ACID y restricción única `(alerta_id, item_id, ciclo_reposicion)`. |
| **Migraciones de BD** | Flyway | `9.22.x` | Control de versiones reproducible de scripts SQL (`V1` esquema, `V2` índices, `V3` `evento_id` de idempotencia, `V4` `preferencias_avisos`, `V5` `correos_pendientes`). |
| **Broker de Mensajería** | RabbitMQ | `3.13` | Desacoplamiento temporal de microservicios mediante Topic Exchanges y Dead Letter Queues. |
| **Caché en Memoria** | Redis | `7.x` | Declarado en `docker-compose`; el servicio todavía no lo usa. |
| **Correo** | Spring Boot Starter Mail (`JavaMailSender`) | `3.2.5` | Avisos por correo cuando el usuario lo elige. Se activa con `SPRING_MAIL_HOST`; sin él no se envía correo. |
| **Seguridad** | Spring Security + JJWT | `0.11.5` | JWT HS256 sin estado; la clave se comparte con `ms-auth`. |
| **Documentación de API** | springdoc-openapi | `2.5.0` | OpenAPI 3 y Swagger UI. |
| **Mapeo de DTOs** | MapStruct | `1.5.5.Final` | Generación de mappers (DTO en `application`, entidades JPA en `infrastructure`) en tiempo de compilación sin sobrecarga de reflexión. |
| **Testing Core & Web** | JUnit 5 + Mockito + MockMvc | `5.10.x` | Pruebas unitarias de dominio/casos de uso y pruebas de integración REST/AMQP/JPA con H2 PostgreSQL mode. |
| **Pruebas de integración** | Testcontainers · WireMock · GreenMail | `1.21.4` · `3.5.4` · `2.0.1` | RabbitMQ real, Catálogo simulado y servidor SMTP de prueba. |
| **Métricas de Cobertura** | JaCoCo Plugin | `0.8.11` | Verificación estricta de cobertura mínima (`>= 85%`) como regla obligatoria en `mvn verify`. |

---

> [!NOTE]
> **Documento Oficial de Análisis y Justificación Tecnológica:** Para consultar el análisis comparativo formal de alternativas evaluadas y descartadas (PostgreSQL vs NoSQL, RabbitMQ vs Kafka, Java/Spring vs Node/Python, SSE vs WebSockets, JavaMailSender vs SDKs propietarios), visita:
> **[docs/ANALISIS_DECISIONES_TECNOLOGICAS.md](../ANALISIS_DECISIONES_TECNOLOGICAS.md)**.
