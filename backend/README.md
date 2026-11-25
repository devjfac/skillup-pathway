# backend

Backend de SkillUp Pathway en Java 21. Paquete raíz `co.edu.sena.skillup`.

## Sprint 1: módulo de gestión de usuarios con JDBC

Aplicación de escritorio en modo consola que se conecta a MySQL 8 por medio de
JDBC (Java Database Connectivity) y ofrece las operaciones de inserción, consulta,
actualización y eliminación (CRUD) sobre las tablas `usuario`, `rol` y `usuario_rol`.
Las capas de servicio y repositorio se reutilizan en el Sprint 1 con Servlets y JSP
y en el Sprint 2 con Spring Boot.

### Requisitos cubiertos

| Requisito | Historia | Funcionalidad | Operación |
|-----------|----------|---------------|-----------|
| RF-01 Registro | HU-01 | Registro de estudiantes con validación de correo (CA-01) y política de contraseñas (CA-02) | Inserción |
| RF-02 Inicio de sesión | HU-02 | Validación de credenciales, mensaje genérico ante error (CA-02) y bloqueo de 15 minutos después de 5 intentos fallidos (CA-03) | Consulta y actualización |
| RF-03 Perfil | — | Ver y actualizar los datos propios, cambiar la contraseña | Consulta y actualización |
| RF-09 Gestión de usuarios | HU-07 | Panel de administrador: listar, buscar, crear con rol, actualizar, activar o desactivar, asignar y quitar roles, eliminar (CA-01) | CRUD completo |

Quedan para sprints posteriores: correo de confirmación (HU-01 CA-03), token JWT
(HU-02 CA-01) y registro de auditoría (HU-07 CA-02).

### Operaciones CRUD y sentencias JDBC

Todas las sentencias usan `PreparedStatement` con parámetros (sin concatenar datos
del usuario) y se encuentran en `repository/JdbcUsuarioRepository.java`.

| Operación | Método del repositorio | Sentencia SQL |
|-----------|------------------------|---------------|
| Inserción | `guardar` (transacción con `commit` y `rollback`) | `INSERT INTO usuario ...` y `INSERT INTO usuario_rol ... SELECT` |
| Inserción | `asignarRol` | `INSERT INTO usuario_rol ... SELECT` |
| Consulta | `buscarPorId`, `buscarPorCorreo`, `listarTodos`, `buscarPorTexto` | `SELECT ... LEFT JOIN usuario_rol ... GROUP BY` |
| Actualización | `actualizarDatos`, `actualizarContrasena`, `actualizarEstado`, `actualizarControlAcceso` | `UPDATE usuario SET ... WHERE id_usuario = ?` |
| Eliminación | `eliminar` | `DELETE FROM usuario WHERE id_usuario = ?` (roles en cascada) |
| Eliminación | `quitarRol` | `DELETE ur FROM usuario_rol AS ur INNER JOIN rol ...` |

### Estructura de paquetes

```
backend/
├── pom.xml                      Dependencias (MySQL Connector/J, JUnit 5) y plugins
├── mvnw, mvnw.cmd, .mvn/        Maven Wrapper: no requiere instalar Maven
├── config/checkstyle/           Reglas del estándar de codificación
└── src/
    ├── main/java/co/edu/sena/skillup/
    │   ├── SkillUpApplication.java   Punto de arranque
    │   ├── config/                   ConexionBaseDatos (DriverManager)
    │   ├── consola/                  Menús, formularios y entrada/salida de texto
    │   ├── dto/                      RegistroUsuarioDTO, ActualizacionUsuarioDTO, UsuarioDTO
    │   ├── entity/                   Usuario, NombreRol, TipoDocumento
    │   ├── exception/                Excepciones de negocio y de persistencia
    │   ├── repository/               UsuarioRepository y JdbcUsuarioRepository
    │   ├── security/                 CifradorContrasena (PBKDF2-HMAC-SHA256)
    │   └── service/                  UsuarioService y ValidadorUsuario
    ├── main/resources/               db.properties y logging.properties
    └── test/java/co/edu/sena/skillup/
        ├── repository/               Prueba de integración JDBC y repositorio en memoria
        ├── security/                 Pruebas del cifrado de contraseñas
        └── service/                  Pruebas de reglas de negocio y validaciones
```

### Estándar de codificación aplicado

Se aplica el estándar definido en la evidencia GA7-220501096-AA1-EV02 y se verifica
de forma automática con Checkstyle en la fase `validate` de Maven: la compilación
falla si alguna regla se incumple.

| Elemento | Convención | Ejemplos en el módulo |
|----------|------------|-----------------------|
| Paquetes | minúsculas, dominio invertido | `co.edu.sena.skillup.repository` |
| Clases e interfaces | PascalCase con sufijo de capa | `UsuarioService`, `JdbcUsuarioRepository`, `UsuarioDTO`, `RecursoNoEncontradoException` |
| Métodos | camelCase, verbo + sustantivo | `registrarEstudiante`, `iniciarSesion`, `actualizarDatos`, `eliminarUsuario` |
| Variables y parámetros | camelCase descriptivo | `correoElectronico`, `intentosFallidos`, `bloqueadoHasta` |
| Constantes | UPPER_SNAKE_CASE | `MAXIMO_INTENTOS_FALLIDOS`, `SQL_INSERTAR_USUARIO` |
| Enumeraciones | PascalCase; valores en mayúscula | `NombreRol.ADMINISTRADOR`, `TipoDocumento.CC` |
| Formato | 4 espacios, 100 columnas, llaves K&R, sin `import *` | Todo el código |
| Diseño | máximo 4 parámetros por método, inyección por constructor | `UsuarioService(UsuarioRepository, CifradorContrasena, Clock)` |
| Documentación | Javadoc en tipos y métodos públicos | Todo el código |
| SQL | palabras reservadas en mayúscula, sin `SELECT *`, snake_case | `SQL_BUSCAR_POR_ID` |

### Ejecución

Requisitos: Java 21 y MySQL 8 (o Docker). No es necesario instalar Maven.

1. Levantar la base de datos (ver `database/README.md`):

   ```bash
   export SKILLUP_DB_CONTRASENA='una-contrasena-local'
   docker compose -f database/docker-compose.yml up -d
   ```

2. Compilar, verificar el estándar y ejecutar las pruebas:

   ```bash
   cd backend
   ./mvnw verify          # En Windows: mvnw.cmd verify
   ```

3. Ejecutar la aplicación:

   ```bash
   java -jar target/skillup-usuarios.jar
   ```

La primera vez, la aplicación pide crear la cuenta de administrador. Después se
puede registrar estudiantes desde el menú principal e iniciar sesión como
administrador para usar el panel de gestión de usuarios.

### Configuración de la conexión

| Parámetro | Archivo `db.properties` | Variable de entorno |
|-----------|-------------------------|---------------------|
| URL JDBC | `skillup.db.url` (por defecto `jdbc:mysql://localhost:3306/skillup`) | `SKILLUP_DB_URL` |
| Usuario | `skillup.db.usuario` (por defecto `skillup_app`) | `SKILLUP_DB_USUARIO` |
| Contraseña | No se guarda en archivos | `SKILLUP_DB_CONTRASENA` (obligatoria) |

En Windows (PowerShell): `$env:SKILLUP_DB_CONTRASENA = 'una-contrasena-local'`.

### Pruebas

- `UsuarioServiceTest` (19 casos): registro, normalización, duplicados, política de
  contraseñas, inicio de sesión, bloqueo por intentos fallidos, perfil, roles y
  eliminación, con un repositorio en memoria y un reloj controlado.
- `ValidadorUsuarioTest` (12 casos) y `CifradorContrasenaTest` (3 casos).
- `JdbcUsuarioRepositoryTest` (3 casos): CRUD real contra MySQL. Solo se ejecuta si
  existe la variable `SKILLUP_DB_CONTRASENA`; en GitHub Actions corre con un
  contenedor MySQL 8.4.
