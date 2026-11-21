[← Volver al índice](../../README.md) · [Todos los README](./)


## <img src="https://img.shields.io/badge/12-5600EF?style=for-the-badge" height="20" align="absmiddle" /> 12. ENLACE A LA ÉPICA EN JIRA

El seguimiento de requerimientos, evidencias y artefactos de entrega se centraliza en los siguientes enlaces oficiales:

### Enlaces de Gestión, Repositorios y Evidencias

* **Épica Oficial del Microservicio (Jira):** [GJ-2 · ms-notificaciones](https://mail-team-nyjtgqcj.atlassian.net/browse/GJ-2)
* **Proyecto en Jira (GJ):** [https://mail-team-nyjtgqcj.atlassian.net/browse/GJ](https://mail-team-nyjtgqcj.atlassian.net/browse/GJ)
* **API Swagger en Producción (PROD):** apagado por seguridad (`SWAGGER_ENABLED=false`); la especificación se consulta en QA o en local. Se enciende con `SWAGGER_ENABLED=true` en el App Service.
* **API Swagger en Calidad (QA):** [https://styleradar-notificaciones-qa.azurewebsites.net/swagger-ui/index.html](https://styleradar-notificaciones-qa.azurewebsites.net/swagger-ui/index.html)
* **Swagger UI Microservicio Notificaciones (:8300):** [http://localhost:8300/swagger-ui/index.html](http://localhost:8300/swagger-ui/index.html)
* **Especificación OpenAPI 3.0 JSON Schema:** [http://localhost:8300/v3/api-docs](http://localhost:8300/v3/api-docs)
* **Documento de Análisis y Justificación de Decisiones Tecnológicas:** [docs/ANALISIS_DECISIONES_TECNOLOGICAS.md](../ANALISIS_DECISIONES_TECNOLOGICAS.md)
* **Contratos y Especificación de Eventos Asíncronos (NOT-21):** [docs/CONTRATOS_EVENTOS_NOT21.md](../CONTRATOS_EVENTOS_NOT21.md)
* **Contrato de autenticación JWT (GJ-35):** [docs/CONTRATO_JWT_AUTENTICACION.md](../CONTRATO_JWT_AUTENTICACION.md)
* **Colección de Pruebas Postman:** [postman/styleradar-notificaciones.postman_collection.json](../../postman/styleradar-notificaciones.postman_collection.json)
* **Contratos de Eventos Asíncronos (NOT-21):** [docs/CONTRATOS_EVENTOS_NOT21.md](../CONTRATOS_EVENTOS_NOT21.md)
* **Diagramas UML Oficiales en Draw.io:** [docs/uml/](../uml/)
* **Documentación Arquitectónica y Justificaciones (NOT-20):** [docs/DIAGRAMAS_NOT20.md](../DIAGRAMAS_NOT20.md)


<div align="center">
  <table width="100%" style="margin: 18px 0; border: 2px dashed #5600EF; border-radius: 8px; background-color: #FBFBFE;">
    <tr>
      <td align="center" style="padding: 22px;">
        <p>
          <img src="https://img.shields.io/badge/ESPACIO_DE_CAPTURA-EVIDENCIA_05-5600EF?style=for-the-badge" alt="Evidencia 05" /><br />
          <strong><font color="#5600EF" size="3">TABLERO SCRUM Y SEGUIMIENTO DE LA ÉPICA EN JIRA</font></strong>
        </p>
        <p>
          <sub>Pega aquí la captura del tablero de Jira mostrando la Épica, User Stories y tareas asignadas al microservicio para el sprint.</sub>
        </p>
        <p>
          <img src="../evidencias/jira-tablero-epica.png" width="90%" alt="Tablero Jira" /><br />
        </p>
      </td>
    </tr>
  </table>
</div>

### Requerimientos Funcionales y No Funcionales Asignados (Numeración Unificada)

| Código RF / RNF | Nombre del Requerimiento | Actor Principal | Criterio de Aceptación Clave |
| :---: | :--- | :--- | :--- |
| `SR-RF-29` | Configurar alerta de disponibilidad | Comprador | Crea alertas sobre prenda agotada (`ITEM_AGOTADO`, `itemId` UUID/texto) o búsqueda guardada (`BUSQUEDA_GUARDADA`) con canal `IN_APP`, `EMAIL`, `PUSH` o `AMBOS`; valida que la prenda esté sin stock en `ms-item-catalogo` (NOT-10), comprueba estado inicial con `GET /alerts/check`, devuelve `nombreBusqueda` y publica `AlertaCreada`. |
| `SR-RF-30` | Detectar prendas que coinciden con una alerta | Sistema / Eventos | Evalúa criterios en memoria al recibir `InventarioActualizado` (tolerante a `stockAnterior` opcional) y `PrendaPublicada`, y desactiva alertas ante `PrendaRetirada` o búsqueda eliminada (NOT-06, NOT-07, NOT-08). |
| `SR-RF-31` | Notificar coincidencia o reposición de una prenda | Comprador / Sistema | Genera una sola notificación por `(alerta_id, item_id, ciclo_reposicion)`, la empuja en tiempo real por SSE y envía correo si la preferencia del tipo es `CORREO_Y_PLATAFORMA` o si el canal de la alerta es `EMAIL` o `AMBOS`, con reintentos persistentes en `correos_pendientes` (NOT-11, GJ-119). |
| `SR-RF-32` | Consultar, desactivar y reactivar alertas | Comprador | Permite listar sus alertas (con `nombreBusqueda`), desactivarlas (`DELETE` o `PATCH /deactivate`) sin borrar la búsqueda guardada y reactivarlas (`PATCH /activate`) validando disponibilidad y ausencia de duplicados (NOT-05). |
| `SR-RF-104` | Consultar la bandeja de notificaciones | Usuario Autenticado | Permite listar con paginación (`page`, `size`) y filtro único `read` (`true` leídas, `false` no leídas), contar no leídas, marcar como leída (individual/todas) y abrir el stream SSE mediante ticket de 60 s (`POST /stream-ticket`) con reconexión `Last-Event-ID` (NOT-12, NOT-13, NOT-15, GJ-62, GJ-118). |
| `SR-RF-105` | Elegir el canal de cada tipo de aviso | Usuario Autenticado | Permite consultar y elegir por cada uno de los 9 valores de `TipoNotificacion` entre `SOLO_PLATAFORMA` (por defecto) y `CORREO_Y_PLATAFORMA` (GJ-64). |
| `SR-RF-106` | Avisar la actividad de otros módulos | Comprador / Donante / Tienda | Genera notificaciones idempotentes por `(evento_id, usuario_id)` al recibir eventos de `ms-chat`, `ms-segunda-vida-donaciones`, `ms-item-catalogo`, `ms-pagos` y `ms-tiendas` (fundación desactivada), excluyendo cuentas en estado `INACTIVO`, `BLOQUEADO` o `SUSPENDIDO` (NOT-09, NOT-14, GJ-61, GJ-65, GJ-117). |
| `SR-RNF-15` | Resiliencia e idempotencia en consumidores de eventos | Sistema / Broker | 3 entregas totales con backoff ante fallos transitorios, rechazo inmediato sin reencolar hacia `styleradar.notificaciones.dlq` ante payloads inválidos, restricciones únicas en PostgreSQL 16 y errores HTTP con campo `code` estable (`CodigoError`). |
