package co.edu.sena.skillup.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.sena.skillup.dto.RegistroUsuarioDTO;
import co.edu.sena.skillup.dto.UsuarioDTO;
import co.edu.sena.skillup.entity.NombreRol;
import co.edu.sena.skillup.entity.TipoDocumento;
import co.edu.sena.skillup.repository.MemoriaUsuarioRepository;
import co.edu.sena.skillup.security.CifradorContrasena;
import co.edu.sena.skillup.service.UsuarioService;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de {@link UsuarioServlet} que verifican el procesamiento de formularios
 * HTML y JSP mediante los métodos HTTP {@code GET} y {@code POST} (RF-01, RF-03, RF-09).
 */
class UsuarioServletTest {

    private static final int ITERACIONES_PRUEBA = 1_000;

    private UsuarioService usuarioService;
    private UsuarioServlet servlet;

    @BeforeEach
    void prepararContexto() {
        usuarioService = new UsuarioService(
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
        usuarioService.registrarEstudiante(new RegistroUsuarioDTO(
                TipoDocumento.CC,
                "1020765432",
                "Cristian Mauricio",
                "Cuevas Lozano",
                "cmcuevas@soy.sena.edu.co",
                "Aprendiz#2026"));
        servlet = new UsuarioServlet(usuarioService);
    }

    @Test
    @DisplayName("GET /usuarios lista el directorio completo y despacha a usuarios.jsp")
    void doGetListaDirectorioCompleto() throws Exception {
        SimuladorHttpServlet http = new SimuladorHttpServlet().conMetodo("GET");

        servlet.doGet(http.crearRequest(), http.crearResponse());

        assertEquals(UsuarioServlet.VISTA_LISTADO, http.getVistaDespachada());
        @SuppressWarnings("unchecked")
        List<UsuarioDTO> listados = (List<UsuarioDTO>) http.getAtributoRequest("usuarios");
        assertNotNull(listados);
        assertEquals(2, listados.size());
    }

    @Test
    @DisplayName("GET /usuarios con parámetros q, rol y estado filtra los resultados en JSP")
    void doGetFiltraPorParametrosQueryString() throws Exception {
        SimuladorHttpServlet http = new SimuladorHttpServlet()
                .conMetodo("GET")
                .conParametro("accion", "listar")
                .conParametro("q", "3855467")
                .conParametro("rol", "ADMINISTRADOR")
                .conParametro("estado", "ACTIVO");

        servlet.doGet(http.crearRequest(), http.crearResponse());

        assertEquals(UsuarioServlet.VISTA_LISTADO, http.getVistaDespachada());
        @SuppressWarnings("unchecked")
        List<UsuarioDTO> filtrados = (List<UsuarioDTO>) http.getAtributoRequest("usuarios");
        assertEquals(1, filtrados.size());
        assertEquals("jfac2420@gmail.com", filtrados.getFirst().correoElectronico());
    }

    @Test
    @DisplayName("GET /usuarios?accion=nuevo carga el formulario JSP de registro")
    void doGetAccionNuevoCargaFormularioVacio() throws Exception {
        SimuladorHttpServlet http = new SimuladorHttpServlet()
                .conMetodo("GET")
                .conParametro("accion", "nuevo");

        servlet.doGet(http.crearRequest(), http.crearResponse());

        assertEquals(UsuarioServlet.VISTA_FORMULARIO, http.getVistaDespachada());
        assertEquals(Boolean.FALSE, http.getAtributoRequest("modoEdicion"));
        assertNotNull(http.getAtributoRequest("usuarioFormulario"));
    }

    @Test
    @DisplayName("GET /usuarios?accion=editar&id=1 precarga los datos del usuario en el JSP")
    void doGetAccionEditarPrecargaUsuarioExistente() throws Exception {
        SimuladorHttpServlet http = new SimuladorHttpServlet()
                .conMetodo("GET")
                .conParametro("accion", "editar")
                .conParametro("id", "1");

        servlet.doGet(http.crearRequest(), http.crearResponse());

        assertEquals(UsuarioServlet.VISTA_FORMULARIO, http.getVistaDespachada());
        assertEquals(Boolean.TRUE, http.getAtributoRequest("modoEdicion"));
        UsuarioDTO precargado = (UsuarioDTO) http.getAtributoRequest("usuarioFormulario");
        assertEquals("3855467", precargado.numeroDocumento());
    }

    @Test
    @DisplayName("POST /usuarios con accion=registrar crea un nuevo usuario y redirige (PRG)")
    void doPostRegistraUsuarioValidoYRedirige() throws Exception {
        SimuladorHttpServlet http = new SimuladorHttpServlet()
                .conMetodo("POST")
                .conParametro("accion", "registrar")
                .conParametro("tipoDocumento", "CC")
                .conParametro("numeroDocumento", "1098654321")
                .conParametro("nombres", "Juan Felipe")
                .conParametro("apellidos", "Rojas Penagos")
                .conParametro("correoElectronico", "jfrojas@soy.sena.edu.co")
                .conParametro("contrasena", "ClaveSegura#2026")
                .conParametro("rol", NombreRol.ESTUDIANTE.name());

        servlet.doPost(http.crearRequest(), http.crearResponse());

        assertNotNull(http.getUrlRedirigida());
        assertTrue(http.getUrlRedirigida().startsWith("/skillup-web/usuarios?mensaje="));
        assertEquals(3, usuarioService.listarUsuarios().size());
    }

    @Test
    @DisplayName("POST /usuarios con contraseña débil devuelve errores CA-02 al formulario JSP")
    void doPostRechazaContrasenaDebilYReenviaAlFormulario() throws Exception {
        SimuladorHttpServlet http = new SimuladorHttpServlet()
                .conMetodo("POST")
                .conParametro("accion", "registrar")
                .conParametro("tipoDocumento", "CC")
                .conParametro("numeroDocumento", "1098654321")
                .conParametro("nombres", "Juan Felipe")
                .conParametro("apellidos", "Rojas Penagos")
                .conParametro("correoElectronico", "jfrojas@soy.sena.edu.co")
                .conParametro("contrasena", "debil")
                .conParametro("rol", NombreRol.ESTUDIANTE.name());

        servlet.doPost(http.crearRequest(), http.crearResponse());

        assertNull(http.getUrlRedirigida());
        assertEquals(UsuarioServlet.VISTA_FORMULARIO, http.getVistaDespachada());
        @SuppressWarnings("unchecked")
        List<String> errores = (List<String>) http.getAtributoRequest("erroresValidacion");
        assertNotNull(errores);
        assertFalse(errores.isEmpty());
    }

    @Test
    @DisplayName("POST /usuarios con accion=actualizar modifica el perfil y redirige")
    void doPostActualizaDatosExistentes() throws Exception {
        SimuladorHttpServlet http = new SimuladorHttpServlet()
                .conMetodo("POST")
                .conParametro("accion", "actualizar")
                .conParametro("idUsuario", "2")
                .conParametro("tipoDocumento", "CC")
                .conParametro("numeroDocumento", "1020765432")
                .conParametro("nombres", "Cristian Mauricio")
                .conParametro("apellidos", "Cuevas Actualizado")
                .conParametro("correoElectronico", "cmcuevas@soy.sena.edu.co")
                .conParametro("rol", NombreRol.MENTOR.name());

        servlet.doPost(http.crearRequest(), http.crearResponse());

        assertNotNull(http.getUrlRedirigida());
        UsuarioDTO actualizado = usuarioService.consultarPorId(2L);
        assertEquals("Cuevas Actualizado", actualizado.apellidos());
        assertTrue(actualizado.roles().contains(NombreRol.MENTOR));
    }

    @Test
    @DisplayName("POST /usuarios con accion=cambiarEstado y accion=eliminar gestiona la cuenta")
    void doPostCambiaEstadoYEliminaUsuario() throws Exception {
        SimuladorHttpServlet postEstado = new SimuladorHttpServlet()
                .conMetodo("POST")
                .conParametro("accion", "cambiarEstado")
                .conParametro("idUsuario", "2")
                .conParametro("activo", "false");

        servlet.doPost(postEstado.crearRequest(), postEstado.crearResponse());
        assertFalse(usuarioService.consultarPorId(2L).activo());

        SimuladorHttpServlet postEliminar = new SimuladorHttpServlet()
                .conMetodo("POST")
                .conParametro("accion", "eliminar")
                .conParametro("idUsuario", "2");

        servlet.doPost(postEliminar.crearRequest(), postEliminar.crearResponse());
        assertEquals(1, usuarioService.listarUsuarios().size());
    }
}
