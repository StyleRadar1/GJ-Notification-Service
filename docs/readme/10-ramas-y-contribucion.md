[← Volver al índice](../../README.md) · [Todos los README](./)


## <img src="https://img.shields.io/badge/10-5600EF?style=for-the-badge" height="20" align="absmiddle" /> 10. RAMAS Y CÓMO CONTRIBUIR

El equipo sigue el modelo de trabajo **GitFlow adaptado para microservicios**. Las ramas `main` y `develop` están protegidas: no admiten *force-push* y solo reciben cambios mediante Pull Requests con al menos una aprobación y el CI en verde.

### Estructura de Ramas Oficial

```text
main (default / protegida)    <-- Código validado en producción; su fusión dispara el CD a PROD
└── develop                   <-- Rama de integración; su fusión despliega en QA
    ├── feature/<tema>        <-- Funcionalidades nuevas
    ├── fix/<tema>            <-- Correcciones
    ├── docs/<tema>           <-- Documentación y diagramas
    └── ci/<tema>             <-- Pipelines y automatización
```

* Cada rama de trabajo nace de la versión más reciente de `develop` y vuelve a ella mediante un Pull Request. Después de fusionarse, la rama se elimina.
* Las versiones se llevan a producción con un Pull Request de `develop` hacia `main`, que también necesita aprobación.
* `Scaffolding` es la rama compartida del arranque del servicio; ya está integrada en `develop` y no se usa para trabajo nuevo.
* Dependabot propone cada lunes las actualizaciones de Maven, GitHub Actions y Docker hacia `develop`.

### Convención de Commits (*Conventional Commits*)

Todo mensaje de commit debe seguir el estándar en minúsculas y modo imperativo:

```text
<tipo>(<alcance>): <descripción imperativa en español>

Ejemplos válidos:
feat(rest): agregar GET /alerts/check para comprobar si el usuario ya tiene una alerta
fix(ci): calentar QA hasta que responda rápido y reintentar Newman tres veces
test(notificaciones): agregar pruebas de integración y mantener la cobertura sobre 85%
docs(readme): dividir el README en un archivo por sección
```

### Checklist Obligatorio para Pull Requests

- [ ] La rama parte de la versión más reciente de `develop`.
- [ ] El código compila limpiamente con `mvn clean compile`.
- [ ] Todas las pruebas unitarias e integración pasan (`mvn verify`).
- [ ] La cobertura de líneas de JaCoCo es mayor o igual al 85%.
- [ ] No se introducen credenciales, contraseñas ni tokens en el código fuente.
- [ ] Si cambia un endpoint, se actualizan el Swagger y la tabla de endpoints (el test `ContratoSwaggerAlineadoTest` las compara).
- [ ] El PR tiene asignado al menos 1 revisor del equipo antes del merge.
