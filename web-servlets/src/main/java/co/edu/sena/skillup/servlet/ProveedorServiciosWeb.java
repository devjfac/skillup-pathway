package co.edu.sena.skillup.servlet;

import co.edu.sena.skillup.config.ConexionBaseDatos;
import co.edu.sena.skillup.dto.RegistroUsuarioDTO;
import co.edu.sena.skillup.entity.NombreRol;
import co.edu.sena.skillup.entity.TipoDocumento;
import co.edu.sena.skillup.repository.JdbcUsuarioRepository;
import co.edu.sena.skillup.repository.MemoriaUsuarioRepository;
import co.edu.sena.skillup.repository.UsuarioRepository;
import co.edu.sena.skillup.security.CifradorContrasena;
import co.edu.sena.skillup.service.RutaAprendizajeService;
import co.edu.sena.skillup.service.UsuarioService;
import jakarta.servlet.ServletContext;
import java.time.Clock;

/**
 * Proveedor centralizado de servicios de negocio para los Servlets del módulo web.
 *
 * <p>Almacena las instancias de {@link UsuarioService} y {@link RutaAprendizajeService}
 * como atributos del {@link ServletContext} para que todos los Servlets compartan el
 * mismo estado de negocio durante el ciclo de vida de la aplicación web.</p>
 */
public final class ProveedorServiciosWeb {

    public static final String ATRIBUTO_USUARIO_SERVICE = "skillup.usuarioService";
    public static final String ATRIBUTO_RUTA_SERVICE = "skillup.rutaAprendizajeService";
    public static final int ITERACIONES_RAPIDAS_WEB = 10_000;

    private ProveedorServiciosWeb() {
        // Clase utilitaria: no se instancia.
    }

    /**
     * Obtiene o inicializa el servicio de gestión de usuarios en el contexto del contenedor.
     *
     * @param contexto contexto de la aplicación web
     * @return instancia compartida de {@link UsuarioService}
     */
    public static synchronized UsuarioService obtenerUsuarioService(ServletContext contexto) {
        Object existente = contexto.getAttribute(ATRIBUTO_USUARIO_SERVICE);
        if (existente instanceof UsuarioService servicio) {
            return servicio;
        }
        UsuarioService nuevoServicio = crearUsuarioServicePorDefecto();
        contexto.setAttribute(ATRIBUTO_USUARIO_SERVICE, nuevoServicio);
        return nuevoServicio;
    }

    /**
     * Obtiene o inicializa el servicio de catálogo de rutas formativas en el contexto web.
     *
     * @param contexto contexto de la aplicación web
     * @return instancia compartida de {@link RutaAprendizajeService}
     */
    public static synchronized RutaAprendizajeService obtenerRutaService(ServletContext contexto) {
        Object existente = contexto.getAttribute(ATRIBUTO_RUTA_SERVICE);
        if (existente instanceof RutaAprendizajeService servicio) {
            return servicio;
        }
        RutaAprendizajeService nuevoServicio = new RutaAprendizajeService();
        contexto.setAttribute(ATRIBUTO_RUTA_SERVICE, nuevoServicio);
        return nuevoServicio;
    }

    /**
     * Construye una instancia de {@link UsuarioService} conectada a MySQL 8 por JDBC si las
     * variables de entorno están definidas, o respaldada por {@link MemoriaUsuarioRepository}
     * con datos semilla del proyecto SkillUp Pathway.
     *
     * @return servicio de usuarios inicializado con cuentas semilla
     */
    public static UsuarioService crearUsuarioServicePorDefecto() {
        CifradorContrasena cifrador = new CifradorContrasena(ITERACIONES_RAPIDAS_WEB);
        UsuarioRepository repositorio = seleccionarRepositorio();
        UsuarioService servicio = new UsuarioService(
                repositorio, cifrador, Clock.systemDefaultZone());
        sembrarUsuariosIniciales(servicio);
        return servicio;
    }

    private static UsuarioRepository seleccionarRepositorio() {
        String contrasenaDb = System.getenv(ConexionBaseDatos.VARIABLE_CONTRASENA);
        if (contrasenaDb != null && !contrasenaDb.isBlank()) {
            try {
                ConexionBaseDatos conexion = ConexionBaseDatos.desdeConfiguracion();
                conexion.probarConexion();
                return new JdbcUsuarioRepository(conexion);
            } catch (RuntimeException ignorada) {
                // Si MySQL no está activo localmente, se utiliza el repositorio en memoria.
            }
        }
        return new MemoriaUsuarioRepository();
    }

    private static void sembrarUsuariosIniciales(UsuarioService servicio) {
        if (!servicio.listarUsuarios().isEmpty()) {
            return;
        }
        servicio.registrarAdministradorInicial(new RegistroUsuarioDTO(
                TipoDocumento.CE,
                "3855467",
                "Juan Fernando",
                "Araujo Castellanos",
                "jfac2420@gmail.com",
                "SkillUp#2026"));
        servicio.crearUsuario(new RegistroUsuarioDTO(
                TipoDocumento.CC,
                "1052418900",
                "Katherine Isabel",
                "Arzuaga Doria",
                "karzuaga@sena.edu.co",
                "Instructora#2026"),
                NombreRol.INSTRUCTOR);
        servicio.registrarEstudiante(new RegistroUsuarioDTO(
                TipoDocumento.CC,
                "1020765432",
                "Cristian Mauricio",
                "Cuevas Lozano",
                "cmcuevas@soy.sena.edu.co",
                "Aprendiz#2026"));
    }
}
