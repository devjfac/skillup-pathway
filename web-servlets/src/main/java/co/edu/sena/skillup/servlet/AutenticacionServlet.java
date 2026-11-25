package co.edu.sena.skillup.servlet;

import co.edu.sena.skillup.dto.UsuarioDTO;
import co.edu.sena.skillup.exception.NegocioException;
import co.edu.sena.skillup.service.UsuarioService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controlador web Jakarta Servlet para el inicio y cierre de sesión (RF-02, HU-02).
 *
 * <p>Gestiona la petición {@code GET /acceso} para desplegar la vista {@code login.jsp}
 * o cerrar la sesión activa ({@code accion=salir}), y la petición {@code POST /acceso}
 * para autenticar credenciales y almacenar el usuario en {@link HttpSession}.</p>
 */
@WebServlet(name = "AutenticacionServlet", urlPatterns = {"/acceso"})
public class AutenticacionServlet extends HttpServlet {

    public static final String VISTA_LOGIN = "/WEB-INF/vistas/login.jsp";
    public static final String ATRIBUTO_SESION_USUARIO = "usuarioAutenticado";

    private static final long serialVersionUID = 1L;

    private transient UsuarioService usuarioService;

    /**
     * Crea el servlet de autenticación para el contenedor web.
     */
    public AutenticacionServlet() {
        // El servicio se resuelve desde el ServletContext en init().
    }

    /**
     * Crea el servlet con el servicio inyectado explícitamente para pruebas unitarias.
     *
     * @param usuarioService servicio de negocio de usuarios
     */
    public AutenticacionServlet(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void init() throws ServletException {
        if (usuarioService == null && getServletContext() != null) {
            usuarioService = ProveedorServiciosWeb.obtenerUsuarioService(getServletContext());
        }
    }

    /**
     * Atiende la petición HTTP {@code GET} mostrando el formulario JSP de inicio de sesión
     * o cerrando la sesión cuando se recibe el parámetro {@code accion=salir}.
     *
     * @param request petición HTTP entrante
     * @param response respuesta HTTP saliente
     * @throws ServletException si falla el despacho a la vista JSP
     * @throws IOException si falla la escritura de respuesta
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html; charset=UTF-8");

        String accion = request.getParameter("accion");
        if ("salir".equalsIgnoreCase(accion)) {
            HttpSession sesion = request.getSession(false);
            if (sesion != null) {
                sesion.invalidate();
            }
            request.setAttribute("mensajeExito", "La sesión se cerró correctamente.");
        }
        request.getRequestDispatcher(VISTA_LOGIN).forward(request, response);
    }

    /**
     * Atiende la petición HTTP {@code POST} con los parámetros {@code correoElectronico}
     * y {@code contrasena} enviados desde el formulario HTML o JSP.
     *
     * @param request petición HTTP con credenciales en el cuerpo
     * @param response respuesta HTTP saliente
     * @throws ServletException si ocurre un error al reenviar a la vista JSP
     * @throws IOException si ocurre un error en la redirección HTTP
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html; charset=UTF-8");

        String correo = request.getParameter("correoElectronico");
        String contrasena = request.getParameter("contrasena");

        try {
            UsuarioDTO autenticado = obtenerServicio(request).iniciarSesion(correo, contrasena);
            HttpSession sesion = request.getSession(true);
            sesion.setAttribute(ATRIBUTO_SESION_USUARIO, autenticado);

            String mensaje = URLEncoder.encode(
                    "Bienvenido a SkillUp Pathway, " + autenticado.nombreCompleto() + ".",
                    StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/usuarios?mensaje=" + mensaje);
        } catch (NegocioException e) {
            request.setAttribute("mensajeError", e.getMessage());
            request.setAttribute("correoIngresado", correo == null ? "" : correo.trim());
            request.getRequestDispatcher(VISTA_LOGIN).forward(request, response);
        }
    }

    private UsuarioService obtenerServicio(HttpServletRequest request) {
        if (usuarioService != null) {
            return usuarioService;
        }
        return ProveedorServiciosWeb.obtenerUsuarioService(request.getServletContext());
    }
}
