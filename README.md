# SkillUp Pathway

Plataforma web y móvil de rutas de aprendizaje personalizadas.
Proyecto formativo del programa Análisis y Desarrollo de Software (SENA), ficha 3336215.

## Estructura del monorepositorio

| Carpeta     | Contenido                                        |
|-------------|--------------------------------------------------|
| `backend/`  | API REST en Java 21 + Spring Boot 3 (Maven)      |
| `frontend/` | Sitio web en React 19 + Vite                     |
| `mobile/`   | Aplicación Android en Kotlin                     |
| `database/` | Scripts SQL de MySQL 8                           |
| `docs/`     | Plan de trabajo, estándares de codificación      |

## Ramas

- `main`: versión estable entregada al final de cada sprint.
- `develop`: integración del sprint en curso.
- `feature/RF-XX-descripcion`: una rama por historia de usuario.

## Convención de commits

Conventional Commits: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`.
Ejemplo: `feat(auth): agrega registro de usuario (RF-01)`.
