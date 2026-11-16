# GJ-118 · Paginación de la bandeja de notificaciones

## Descripción Funcional
Implementación de paginación basada en Spring Data (`Pageable`, `page`, `size`) para el endpoint de consulta de notificaciones del usuario autenticado:
`GET /api/v1/usuarios/me/notificaciones?page=0&size=20&soloNoLeidas=false`

## Especificación del Contrato
* **Query Params:**
  * `page` (int, default `0`, min `0`).
  * `size` (int, default `20`, min `1`, max `100`).
  * `soloNoLeidas` (boolean, default `false`).
* **Respuesta JSON:**
  Envuelve los elementos en una estructura paginada estándar con metadatos:
  ```json
  {
    "content": [
      {
        "id": 101,
        "usuarioId": 501,
        "tipo": "SOLICITUD_DONACION",
        "titulo": "Nueva solicitud",
        "mensaje": "Carlos quiere donar a tu fundación",
        "enlace": "/donaciones/40",
        "leida": false,
        "fechaCreacion": "2026-11-16T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3,
    "first": true,
    "last": false
  }
  ```

## Implementación y Pruebas
* **Branch:** `feature/backlog-notificaciones-gj117-118-119` (PR #26 hacia `develop`).
* **Pruebas Newman:** Colección Postman ejecutada con validaciones de límites de página y contenido.
