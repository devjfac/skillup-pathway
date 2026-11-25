# SkillUp Pathway — Módulo Web con Servlets y JSP (GA7-220501096-AA2-EV02)

Módulo web de gestión de cuentas de usuario, autenticación e inscripción a rutas de aprendizaje codificado y probado con **Java 21 (LTS)**, **Jakarta Servlets 6.0**, **Jakarta Server Pages (JSP 3.1)**, **JSTL 3.0**, **JDBC sobre MySQL 8.4** y **JUnit 5**.

- **Aprendiz:** Juan Fernando Araujo Castellanos (`C.E. 3855467`)
- **Programa:** Tecnólogo en Análisis y Desarrollo de Software (ADSO) — Ficha `3336215`
- **Instructora:** Katherine Isabel Arzuaga Doria
- **Evidencia:** `GA7-220501096-AA2-EV02` — Módulos de software codificados y probados
- **Repositorio GitHub:** [https://github.com/devjfac/skillup-pathway](https://github.com/devjfac/skillup-pathway)

## Estructura del Proyecto

```text
proyecto/
├── pom.xml                                       # Descriptor Maven (Java 21, Servlets 6.0, JSP 3.1, JUnit 5, Checkstyle)
├── config/checkstyle/checkstyle.xml              # Reglas automáticas del estándar de codificación (GA7-AA1-EV02)
├── database/scripts/
│   ├── 01_esquema_modulo_usuarios.sql            # DDL MySQL 8.4 (tablas rol, usuario, usuario_rol)
│   └── 02_datos_iniciales.sql                    # Catálogo inicial de roles de SkillUp Pathway
└── src/
    ├── main/
    │   ├── java/co/edu/sena/skillup/
    │   │   ├── config/ConexionBaseDatos.java     # Fábrica de conexiones JDBC
    │   │   ├── dto/                              # Records UsuarioDTO, RegistroUsuarioDTO, ActualizacionUsuarioDTO, RutaAprendizajeDTO
    │   │   ├── entity/                           # Entidades Usuario, NombreRol, TipoDocumento
    │   │   ├── exception/                        # Jerarquía de excepciones de dominio y validación
    │   │   ├── repository/                       # UsuarioRepository, JdbcUsuarioRepository y MemoriaUsuarioRepository
    │   │   ├── security/CifradorContrasena.java  # Hash PBKDF2-HMAC-SHA256 con sal aleatoria
    │   │   ├── service/                          # UsuarioService, ValidadorUsuario, RutaAprendizajeService
    │   │   └── servlet/                          # Controladores HTTP (doGet y doPost):
    │   │       ├── UsuarioServlet.java           # GET y POST /usuarios (CRUD, filtros y formularios)
    │   │       ├── AutenticacionServlet.java     # GET y POST /acceso (inicio y cierre de sesión)
    │   │       ├── RutaAprendizajeServlet.java   # GET y POST /rutas (catálogo e inscripción)
    │   │       └── ProveedorServiciosWeb.java    # Inicialización de servicios en ServletContext
    │   └── webapp/
    │       ├── index.html                        # Portal HTML5 con formularios GET y POST hacia los Servlets
    │       ├── css/styles.css                    # Hoja de estilos sobria accesible (WCAG 2.2 AA)
    │       └── WEB-INF/
    │           ├── web.xml                       # Descriptor Jakarta EE 10 (Web Application 6.0)
    │           └── vistas/
    │               ├── cabecera.jspf             # Fragmento JSP de encabezado y navegación
    │               ├── pie.jspf                  # Fragmento JSP de pie de página institucional
    │               ├── login.jsp                 # Vista JSP de autenticación (RF-02, HU-02)
    │               ├── usuarios.jsp              # Vista JSP de directorio y filtros GET / acciones POST (RF-09, HU-07)
    │               ├── formulario-usuario.jsp    # Vista JSP de registro y edición de perfil (RF-01, RF-03, HU-01)
    │               └── rutas.jsp                 # Vista JSP de catálogo e inscripción a rutas (RF-04, RF-05, RF-06)
    └── test/
        └── java/co/edu/sena/skillup/servlet/
            ├── SimuladorHttpServlet.java         # Simulador de HttpServletRequest, HttpServletResponse y HttpSession
            ├── UsuarioServletTest.java           # 8 pruebas unitarias de doGet y doPost en UsuarioServlet
            └── AutenticacionYRutasServletTest.java # 5 pruebas unitarias de AutenticacionServlet y RutaAprendizajeServlet
```

## Compilación, Verificación de Estándares y Pruebas

```bash
mvn clean test checkstyle:check package
```
