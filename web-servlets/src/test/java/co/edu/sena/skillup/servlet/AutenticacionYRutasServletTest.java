package co.edu.sena.skillup.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.sena.skillup.dto.RegistroUsuarioDTO;
import co.edu.sena.skillup.dto.RutaAprendizajeDTO;
import co.edu.sena.skillup.dto.UsuarioDTO;
import co.edu.sena.skillup.entity.TipoDocumento;
import co.edu.sena.skillup.exception.CredencialesInvalidasException;
import co.edu.sena.skillup.repository.MemoriaUsuarioRepository;
import co.edu.sena.skillup.security.CifradorContrasena;
import co.edu.sena.skillup.service.RutaAprendizajeService;
import co.edu.sena.skillup.service.UsuarioService;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de {@link AutenticacionServlet} y {@link RutaAprendizajeServlet}
 * validando el flujo de inicio de sesión (RF-02, HU-02), cierre de sesión y catálogo
 * de rutas de aprendizaje (RF-04, RF-05, RF-06) por métodos {@code GET} y {@code POST}.
 */
class AutenticacionYRutasServletTest {

    private static final int ITERACIONES_PRUEBA = 1_000;

    private AutenticacionServlet autenticacionServlet;
    private RutaAprendizajeServlet rutaServlet;

    @BeforeEach
    void prepararContexto() {
        UsuarioService usuarioService = new UsuarioService(
                new MemoriaUsuarioRepository(),
                new CifradorContrasena(ITERACIONES_PRUEBA),
                Clock.systemDefaultZone());
        usuarioService.registrarAdministradorInicial(new RegistroUsuarioDTO(
                TipoDocumento.CE,
                "3855467",
                "Juan Fernando",
                "Araujo Castellanos",
                "jfac2420@gmail.com",
                "SkillUp#2026"));
        autenticacionServlet = new AutenticacionServlet(usuarioService);
        rutaServlet = new RutaAprendizajeServlet(new RutaAprendizajeService());
    }

    @Test
    @DisplayName("POST /acceso con credenciales válidas crea sesión HTTP y redirige a /usuarios")
    void doPostLoginExitosoCreaSesionYRedirige() throws Exception {
        SimuladorHttpServlet http = new SimuladorHttpServlet()
                .conMetodo("POST")
                .conParametro("correoElectronico", "jfac2420@gmail.com")
                .conParametro("contrasena", "SkillUp#2026");

        autenticacionServlet.doPost(http.crearRequest(), http.crearResponse());

        assertNotNull(http.getUrlRedirigida());
        assertTrue(http.getUrlRedirigida().startsWith("/skillup-web/usuarios?mensaje="));
        UsuarioDTO enSesion = (UsuarioDTO) http.getAtributoSesion(
                AutenticacionServlet.ATRIBUTO_SESION_USUARIO);
        assertNotNull(enSesion);
        assertEquals("3855467", enSesion.numeroDocumento());
    }

    @Test
    @DisplayName("POST /acceso con contraseña errónea muestra mensaje genérico CA-02 en login.jsp")
    void doPostLoginFallidoReenviaALoginJspConError() throws Exception {
        SimuladorHttpServlet http = new SimuladorHttpServlet()
                .conMetodo("POST")
                .conParametro("correoElectronico", "jfac2420@gmail.com")
                .conParametro("contrasena", "ClaveIncorrecta#1");

        autenticacionServlet.doPost(http.crearRequest(), http.crearResponse());

        assertNull(http.getUrlRedirigida());
        assertEquals(AutenticacionServlet.VISTA_LOGIN, http.getVistaDespachada());
        assertEquals(CredencialesInvalidasException.MENSAJE,
                http.getAtributoRequest("mensajeError"));
    }

    @Test
    @DisplayName("GET /acceso?accion=salir invalida la sesión HTTP activa y muestra login.jsp")
    void doGetSalirInvalidaSesion() throws Exception {
        SimuladorHttpServlet http = new SimuladorHttpServlet()
                .conMetodo("GET")
                .conParametro("accion", "salir");

        autenticacionServlet.doGet(http.crearRequest(), http.crearResponse());

        assertTrue(http.isSesionInvalidada());
        assertEquals(AutenticacionServlet.VISTA_LOGIN, http.getVistaDespachada());
        assertNotNull(http.getAtributoRequest("mensajeExito"));
    }

    @Test
    @DisplayName("GET /rutas?area=Ciberseguridad filtra las rutas y despacha a rutas.jsp")
    void doGetRutasFiltraPorArea() throws Exception {
        SimuladorHttpServlet http = new SimuladorHttpServlet()
                .conMetodo("GET")
                .conParametro("area", "Ciberseguridad");

        rutaServlet.doGet(http.crearRequest(), http.crearResponse());

        assertEquals(RutaAprendizajeServlet.VISTA_RUTAS, http.getVistaDespachada());
        @SuppressWarnings("unchecked")
        List<RutaAprendizajeDTO> rutas =
                (List<RutaAprendizajeDTO>) http.getAtributoRequest("rutas");
        assertEquals(1, rutas.size());
        assertEquals("Ciberseguridad", rutas.getFirst().areaConocimiento());
    }

    @Test
    @DisplayName("POST /rutas inscribe al aprendiz o incrementa su avance en la ruta seleccionada")
    void doPostRutasInscribeYRedirige() throws Exception {
        SimuladorHttpServlet http = new SimuladorHttpServlet()
                .conMetodo("POST")
                .conParametro("idRuta", "3");

        rutaServlet.doPost(http.crearRequest(), http.crearResponse());

        assertNotNull(http.getUrlRedirigida());
        assertTrue(http.getUrlRedirigida().startsWith("/skillup-web/rutas?mensaje="));
    }
}
