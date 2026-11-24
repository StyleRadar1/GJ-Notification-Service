[← Volver al índice](../../README.md) · [Todos los README](./)


## <img src="https://img.shields.io/badge/09-5600EF?style=for-the-badge" height="20" align="absmiddle" /> 9. PRUEBAS Y COBERTURA (≥ 85%)

El proyecto exige una cobertura de líneas mínima del **85%** verificada automáticamente mediante el plugin de **JaCoCo** durante `mvn verify`.

### Comandos de Ejecución de Pruebas

```bash
# Ejecutar todas las pruebas unitarias y de integración con verificación JaCoCo >= 85%
mvn clean verify

# Generar únicamente el reporte HTML de JaCoCo
mvn jacoco:report
```

* **Ruta del Reporte HTML:** `target/site/jacoco/index.html`

<p align="center">
  <img src="../evidencias/cobertura-jacoco.png" width="95%" alt="Reporte HTML de JaCoCo" />
</p>

<p align="center">
  <sub>Reporte real de JaCoCo 0.8.16 (<code>target/site/jacoco/index.html</code>) generado con <code>mvn clean verify</code> el 20/11/2026: 94 % de instrucciones, 71 % de ramas y 1.905 líneas medidas, de las que 115 quedan sin cubrir (94,0 % de líneas).</sub>
</p>


### Pirámide de Pruebas Implementada

| Nivel de Prueba | Alcance | Tecnologías Utilizadas | Ubicación en el Código | Cobertura Meta |
| :--- | :--- | :--- | :--- | :---: |
| **Pruebas Unitarias de Dominio** | `AlertaValidator`, `EvaluadorCriteriosMatcher` y modelos puros | JUnit 5, AssertJ | `src/test/java/.../unit/domain/` | ≥ 90% |
| **Pruebas Unitarias de Aplicación** | Casos de uso aislados (`CrearAlerta`, `Listar`, `Desactivar`, `ProcesarEventos`, `PreferenciasAvisos`, `EnviarAvisoPorCorreo`) y adaptador de correo con reintentos | JUnit 5, Mockito, AssertJ | `src/test/java/.../unit/application/` | ≥ 90% |
| **Pruebas de Integración y Contrato** | Controladores REST, Seguridad JWT, Flyway, Repositorios JPA, Consumidores AMQP, SSE y correo | Spring Boot Test, MockMvc, H2 PostgreSQL Mode, GreenMail | `src/test/java/.../integration/` | ≥ 85% |
| **Pruebas con RabbitMQ real** | Publicación de `AlertaCreada` y consumo de eventos contra un broker real | Testcontainers (requieren Docker; en un equipo sin Docker se omiten y las corre el CI) | `src/test/java/.../integration/messaging/` | — |

Estado actual (24/11/2026): **293 pruebas ejecutadas, 0 fallos y 0 errores; cobertura de líneas 93,6 %** (el mínimo exigido es 85 %). Hay 14 pruebas que necesitan Docker (Testcontainers con RabbitMQ y PostgreSQL reales): en un equipo sin Docker se omiten y las ejecuta el CI.

> [!IMPORTANT]
> **Criterio de Fallo en CI:** El archivo `pom.xml` contiene la regla `<limit><minimum>0.85</minimum></limit>`. Si la cobertura de líneas consolidada es inferior al 85%, el comando `mvn verify` fallará con error y el pipeline bloqueará el Pull Request.
