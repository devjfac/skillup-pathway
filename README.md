# SkillUp Pathway

Plataforma web y móvil de rutas de aprendizaje personalizadas.
Proyecto formativo del programa Análisis y Desarrollo de Software (SENA), ficha 3336215.

## Estructura del monorepositorio

| Carpeta     | Contenido                                        |
|-------------|--------------------------------------------------|
| `backend/`  | Java 21 (Maven): módulo JDBC y API REST con Spring Boot 3 |
| `frontend/` | Sitio web en React 19 + Vite                     |
| `mobile/`   | Aplicación Android en Kotlin                     |
| `database/` | Scripts SQL de MySQL 8                           |
| `docs/`     | Plan de trabajo, estándares de codificación      |

## Estado del proyecto

| Sprint | Entregable | Estado |
|--------|------------|--------|
| Sprint 0 | Estructura del monorepositorio, ramas y convenciones | Terminado (v0.1.0) |
| Sprint 1 | Módulo de gestión de usuarios con JDBC: CRUD sobre MySQL (RF-01, RF-02, RF-03, RF-09) | Terminado en `develop` |
| Sprint 1 | Formularios HTML con Servlets y JSP | Pendiente |

Instrucciones de ejecución del módulo de usuarios: [`backend/README.md`](backend/README.md).
Scripts de base de datos: [`database/README.md`](database/README.md).

## Ramas

- `main`: versión estable entregada al final de cada sprint.
- `develop`: integración del sprint en curso.
- `feature/RF-XX-descripcion`: una rama por historia de usuario.

## Convención de commits

Conventional Commits: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.
Ejemplo: `feat(auth): agrega registro de usuario (RF-01)`.
