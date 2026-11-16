# GJ-116 · Poner la URL real de QA en el README

Evidencia de la corrección y actualización de la documentación del microservicio para reflejar el host oficial de Azure QA y marcar adecuadamente el estado de Producción.

## Cumplimiento de Criterios

* **URL Real de QA:** El `README.md` (sección 4) y la configuración Swagger (`OpenApiConfig.java`) apuntan a `https://styleradar-notificaciones-qa.azurewebsites.net`.
* **Estado de PROD:** El entorno de Producción se marca formalmente como `Pendiente (GJ-92)`, retirando el host inexistente `styleradar-backend-prod`.
* **Evidencia exigida por Jira:** [Pull Request #19 · docs: URL real de QA en el README y sin el host de PROD inexistente (GJ-116)](https://github.com/StyleRadar1/GJ-Notification-Service/pull/19) **FUSIONADO (MERGED)** por Paula Díaz (`paulisdiar`) en la rama `develop`.

## Archivos de Evidencia

| Archivo | Qué muestra |
| :--- | :--- |
| `GJ-116_01_matriz_swagger_readme.png` | Captura limpia de la Matriz Oficial de Enlaces de Swagger en el `README.md` mostrando QA activo en Azure y PROD pendiente. |
