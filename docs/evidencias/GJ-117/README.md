# GJ-117 · Avisar cuando una fundación se desactiva

## Contexto y Origen Técnico
* **Microservicio Emisor:** `ms-tiendas` (`ms-stores`, Puerto `8140`).
* **Ficha Técnica Oficial Verificada:** `ms-stores.pdf` (Pág. 6 y 18 en Google Drive `15wFfjZFzp1_Px9CEuXbxsgM8BpNCF8XP`).
* **Operación Emisora:** `PATCH /api/v1/admin/fundaciones/{id}/desactivar` (requiere `ADMIN_STYLERADAR`).
* **Exchange:** `styleradar.fundaciones.exchange` (y `styleradar.exchange`).
* **Routing Key:** `styleradar.tiendas.fundacion.desactivada` (con alias `fundacion.desactivada`).
* **Cola Consumidora en `ms-notificaciones`:** `styleradar.notificaciones.fundacion-desactivada.q`.

## Contrato de Datos Oficial (Sin Vacíos)
El evento se dispara cuando un administrador desactiva una fundación aliada (`activa: false` en `FundacionResponseDTO`). El payload oficial transmitido incluye:

```json
{
  "eventoId": "evt-fund-9901",
  "fundacionId": 3,
  "nombreFundacion": "Fundación Abrigo Solidario",
  "donantesAfectadosIds": [501, 502, 620],
  "motivo": "Desactivación administrativa por cambio de sede",
  "fechaEvento": "2026-11-16T10:00:00"
}
```

## Comportamiento en `ms-notificaciones`
1. **Aviso a Donantes:** Cada usuario en `donantesAfectadosIds` recibe una notificación directa tipo `CAMBIO_DONACION` con el mensaje: *"La fundación Fundación Abrigo Solidario fue desactivada. Revisa tus solicitudes de donación para redirigirlas a otra entidad."* y enlace `/donaciones/mis-donaciones`.
2. **Idempotencia:** Ante un reintento con el mismo `eventoId`, la base de datos ignora la repetición mediante la clave única `(evento_id, usuario_id)`.
3. **Resiliencia:** Si el payload es nulo, no trae `fundacionId` o la lista de donantes está vacía, el mensaje es derivado a la cola de errores `styleradar.notificaciones.dlq`.

## Implementación y Pruebas
* **Código Implementado:** Branch `feature/backlog-notificaciones-gj117-118-119` (PR #26 hacia `develop`).
* **Consumidor:** `NotificacionesEventsListener.java` (`onFundacionDesactivada`).
* **DTO:** `FundacionDesactivadaEventPayload.java`.
* **Pruebas de Integración:** `ConsumidoresEventosExternosTest.java` (pruebas de recepción, deduplicación y derivación a DLQ).
