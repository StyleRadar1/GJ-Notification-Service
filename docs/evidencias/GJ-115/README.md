# GJ-115 · Confirmar con otros equipos los eventos de donación, venta y pago

Acuerdos de integración técnica y definición de routing keys consumidas por `ms-notificaciones` para los eventos emitidos por los microservicios de Donaciones, Segunda Vida y Pagos, **contrastados y validados al 100% contra las Fichas Técnicas Oficiales de Integración compartidas en Google Drive** (`15wFfjZFzp1_Px9CEuXbxsgM8BpNCF8XP`).

## Contratos y Routing Keys Acordadas y Verificadas

1. **Equipo de Donaciones (`ms-segunda-vida-donaciones` · Portal 8080 - Juan David Munar):**
   * **Ficha Técnica Oficial:** `ms-segunda-vida-donaciones.pdf` (Pág. 7 y 20).
   * **Routing Key Oficial:** `styleradar.donaciones.solicitud.creada` y `styleradar.donaciones.estado.cambiado`.
   * **Exchange:** `styleradar.exchange` (topic, durable).
   * **Payload Oficial:** `eventoId`, `donacionId`, `fundacionUsuarioId`, `nombreDonante`, `resumen`, `fechaEvento`.
   * **Comportamiento:** Genera notificación tipo `SOLICITUD_DONACION` con enlace `/donaciones/{donacionId}`.
2. **Equipo de Segunda Vida (`ms-segunda-vida-donaciones` / C2C):**
   * **Ficha Técnica Oficial:** `ms-segunda-vida-donaciones.pdf` (Pág. 7) y `ms-chat.pdf` (Pág. 4).
   * **Routing Key Oficial:** `styleradar.segundavida.prenda.vendida` (con alias `segunda-mano.prenda.vendida` en topic).
   * **Exchange:** `styleradar.exchange` (y `styleradar.events`).
   * **Payload Oficial:** `eventoId`, `publicacionId`, `nombrePrenda`, `compradorId`, `interesadosIds`, `fechaEvento`.
   * **Comportamiento:** Genera notificación tipo `PRENDA_VENDIDA` con enlace `/segunda-mano/{publicacionId}` a los interesados, excluyendo al comprador.
3. **Equipo de Pagos (`ms-pagos-suscripciones`):**
   * **Ficha Técnica Oficial:** `ms-pagos.docx` y `ms-pagos.xlsx`.
   * **Routing Key Oficial:** `styleradar.pagos.pago.confirmado` (con binding a `pago.confirmado`).
   * **Exchange:** `styleradar.payments.exchange` y `styleradar.exchange`.
   * **Payload Oficial:** `eventoId`, `pagoId`, `usuarioId`, `plan`, `fechaConfirmacion`.
   * **Comportamiento:** Genera notificación tipo `PAGO_CONFIRMADO` con enlace `/planes/mi-plan`.

## Idempotencia y Resiliencia

* Cada emisor incluye un `eventoId` único y persistente que se mantiene constante ante reintentos.
* Ante eventos duplicados, la restricción de base de datos descarta la repetición sin emitir duplicados a la campana.
* Mensajes con datos obligatorios faltantes o usuarios no registrados se derivan a la cola de errores `styleradar.notificaciones.dlq`.

## Archivos de Evidencia

| Archivo | Qué demuestra |
| :--- | :--- |
| `GJ-115_01_acuerdos_routing_keys_equipos.png` | Resumen de contratos, claves de enrutamiento y payloads oficiales verificados para cada equipo emisor. |
| `GJ-115_02_recepcion_eventos_campana_e2e.png` | Recepción de extremo a extremo de los 3 eventos en el stream SSE de Azure QA (`/notificaciones/stream`) actualizando la campana en tiempo real. |
| `mensajes_equipos_eventos.md` | Mensajes y contratos de integración técnica validados con las fichas de Google Drive. |

