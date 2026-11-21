[← Volver al índice](../../README.md) · [Todos los README](./)


## <img src="https://img.shields.io/badge/11-5600EF?style=for-the-badge" height="20" align="absmiddle" /> 11. RAMAS Y CÓMO CONTRIBUIR

El equipo sigue el modelo de trabajo **GitFlow adaptado para microservicios**. La rama `main` se encuentra protegida y solo recibe cambios mediante Pull Requests aprobados.

### Estructura de Ramas Oficial

```text
main (default / protegida)    <-- Código validado en producción
└── develop                   <-- Rama de integración para QA / Staging
    └── Scaffolding           <-- Rama de trabajo que concentra el desarrollo del microservicio
        (feature/scaffolding y features integrados con commits atómicos)
```

### Convención de Commits (*Conventional Commits*)

Todo mensaje de commit debe seguir el estándar en minúsculas y modo imperativo:

```text
<tipo>(<alcance>): <descripción imperativa en español>

Ejemplos válidos:
feat(domain): migrar modelos alerta, notificacion, enums y validadores de dominio
feat(rest): migrar endpoints de alertas, notificaciones y sse bajo /api/v1/usuarios/me
test(notificaciones): agregar pruebas unitarias y de integracion superando 85% en jacoco
docs(readme): actualizar estructura oficial del readme en develop
```

### Checklist Obligatorio para Pull Requests

- [ ] La rama parte de la versión más reciente de `develop`.
- [ ] El código compila limpiamente con `mvn clean compile`.
- [ ] Todas las pruebas unitarias e integración pasan (`mvn verify`).
- [ ] La cobertura JaCoCo es mayor o igual al 85%.
- [ ] No se introducen credenciales, contraseñas ni tokens en el código fuente.
- [ ] El PR tiene asignado al menos 1 revisor del squad antes del merge.
