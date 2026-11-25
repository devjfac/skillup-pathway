# database

Scripts SQL de MySQL 8 para el esquema `skillup`.

| Script | Contenido |
|--------|-----------|
| `scripts/01_esquema_modulo_usuarios.sql` | Tablas `rol`, `usuario` y `usuario_rol` con llaves primarias, foráneas, restricciones únicas y de validación |
| `scripts/02_datos_iniciales.sql` | Catálogo de roles: Estudiante, Instructor, Mentor, Empresa y Administrador |

Las tablas provienen del modelo lógico de la evidencia GA4-220501095-AA1-EV01 y aplican
las convenciones de GA7-220501096-AA1-EV02: nombres en snake_case y en singular,
llave primaria `id_<tabla>`, prefijos `pk_`, `fk_`, `uq_`, `idx_` y `chk_`, y columnas
de auditoría `fecha_creacion` y `fecha_actualizacion`. Frente al modelo lógico se
ajustaron los nombres de columna al estándar (`email` pasa a `correo_electronico`,
`password` a `contrasena_hash` y `fk_usuario` a `id_usuario`), se separó el nombre en
`nombres` y `apellidos`, y se agregaron `intentos_fallidos` y `bloqueado_hasta` para el
bloqueo temporal de HU-02.

## Opción 1: Docker

```bash
export SKILLUP_DB_CONTRASENA='una-contrasena-local'
docker compose -f database/docker-compose.yml up -d
```

El contenedor `skillup-mysql` crea la base `skillup`, el usuario `skillup_app` con la
contraseña de la variable y ejecuta los scripts la primera vez. Para borrar los datos
y empezar de cero: `docker compose -f database/docker-compose.yml down -v`.

## Opción 2: MySQL instalado (MySQL Workbench o consola)

Ejecutar los scripts en orden con un usuario administrador y crear el usuario de la
aplicación con permisos solo sobre el esquema:

```sql
SOURCE database/scripts/01_esquema_modulo_usuarios.sql;
SOURCE database/scripts/02_datos_iniciales.sql;

CREATE USER 'skillup_app'@'localhost' IDENTIFIED BY 'una-contrasena-local';
GRANT SELECT, INSERT, UPDATE, DELETE ON skillup.* TO 'skillup_app'@'localhost';
```
