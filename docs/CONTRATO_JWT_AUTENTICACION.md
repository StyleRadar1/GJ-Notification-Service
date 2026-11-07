# Contrato de autenticación JWT — ms-notificaciones (GJ-35)

`ms-notificaciones` no emite tokens: valida el JWT que emite el servicio de autenticación y toma el usuario de él. Este documento fija lo que debe cumplir ese token. Lo que dice aquí es lo que implementa `JwtTokenService` y `JwtAuthenticationFilter`.

## Lo que debe cumplir el token

| Aspecto | Valor |
| :--- | :--- |
| Algoritmo de firma | `HS256` (HMAC con clave compartida) |
| Clave | Texto plano, leído como UTF-8, en la variable de entorno `JWT_SECRET`. No es Base64. Debe tener al menos 32 bytes; si es más corta, el servicio la rellena con ceros, así que el emisor debe usar una clave de 32 bytes o más. |
| Envío | Cabecera `Authorization: Bearer <token>` |
| Expiración | Claim `exp`. Un token vencido se rechaza con 401. |

## Claims que se leen

| Claim | Uso | Si falta |
| :--- | :--- | :--- |
| `userId` | Id del usuario (número, o texto numérico). Es el dueño de las alertas y notificaciones. | Se usa `sub` |
| `sub` | Id del usuario como texto (alternativa a `userId`) | El token no sirve |
| `email` | Correo del usuario | Se usa `sub` |
| `role` | Rol (`COMPRADOR` por defecto). Se aceptan los valores con o sin prefijo `ROLE_`. | `COMPRADOR` |

## Respuestas del servicio

| Situación | Respuesta |
| :--- | :--- |
| Sin token | 401 |
| Token vencido, con otra firma o manipulado | 401 |
| Token válido que intenta usar una alerta o notificación de otro usuario | 403 |

Solo `/actuator/health` y la documentación de Swagger (`/swagger-ui`, `/v3/api-docs`) son públicos.

## Pruebas que lo respaldan

`SeguridadJwtHttpTest` cubre los 401 (sin token, vencido, otra firma, manipulado) y los 403 (alerta y notificación ajenas). `JwtSecretConfigurationTest` comprueba que el servicio no arranca sin `JWT_SECRET`.

## Pendiente de confirmar con el servicio de autenticación

- Que firme con `HS256` y con una clave de 32 bytes o más compartida por variable de entorno.
- Que incluya `userId` (o `sub` con el id numérico) en el token.
