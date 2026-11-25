package co.edu.sena.skillup.servlet;

import co.edu.sena.skillup.dto.RutaAprendizajeDTO;
import co.edu.sena.skillup.exception.NegocioException;
import co.edu.sena.skillup.service.RutaAprendizajeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controlador web Jakarta Servlet para el catálogo e inscripción en rutas formativas
 * (RF-04, RF-05 y RF-06).
 *
 * <p>Procesa peticiones {@code GET /rutas} con filtro por área de conocimiento y peticiones
 * {@code POST /rutas} para inscribir al aprendiz o registrar su avance por módulo.</p>
 */
@WebServlet(name = "RutaAprendizajeServlet", urlPatterns = {"/rutas"})
public class RutaAprendizajeServlet extends HttpServlet {

    public static final String VISTA_RUTAS = "/WEB-INF/vistas/rutas.jsp";

    private static final long serialVersionUID = 1L;

    private transient RutaAprendizajeService rutaService;

    /**
     * Crea el servlet para despliegue en contenedor web.
     */
    public RutaAprendizajeServlet() {
        // El servicio se resuelve desde el ServletContext en init().
    }

    /**
     * Crea el servlet con el servicio inyectado explícitamente para pruebas unitarias.
     *
     * @param rutaService servicio de catálogo de rutas formativas
     */
    public RutaAprendizajeServlet(RutaAprendizajeService rutaService) {
        this.rutaService = rutaService;
    }

    @Override
    public void init() throws ServletException {
        if (rutaService == null && getServletContext() != null) {
            rutaService = ProveedorServiciosWeb.obtenerRutaService(getServletContext());
        }
    }

    /**
     * Procesa peticiones HTTP {@code GET} filtrando el catálogo por el parámetro {@code area}.
     *
     * @param request petición HTTP entrante
     * @param response respuesta HTTP saliente
     * @throws ServletException si ocurre un error al despachar la vista JSP
     * @throws IOException si ocurre un error de entrada/salida
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html; charset=UTF-8");

        String area = request.getParameter("area");
        String areaLimpia = area == null ? "" : area.trim();
        RutaAprendizajeService servicio = obtenerServicio(request);
        List<RutaAprendizajeDTO> rutas = servicio.listarRutas(areaLimpia);

        request.setAttribute("rutas", rutas);
        request.setAttribute("areaSeleccionada", areaLimpia);
        request.setAttribute("totalInscripciones", servicio.contarInscripcionesActivas());
        request.getRequestDispatcher(VISTA_RUTAS).forward(request, response);
    }

    /**
     * Procesa peticiones HTTP {@code POST} para inscribir al estudiante en una ruta o
     * actualizar su porcentaje de avance (RF-05, RF-06).
     *
     * @param request petición HTTP con {@code idRuta} en el cuerpo
     * @param response respuesta HTTP saliente
     * @throws ServletException si falla el despacho a la vista JSP
     * @throws IOException si falla la redirección HTTP
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html; charset=UTF-8");

        long idRuta = leerIdRuta(request.getParameter("idRuta"));
        try {
            RutaAprendizajeDTO actualizada = obtenerServicio(request).inscribirOAvanzar(idRuta);
            String mensaje = URLEncoder.encode(
                    "Inscripción y avance actualizados en la ruta: " + actualizada.titulo()
                    + " (" + actualizada.porcentajeAvance() + "% completado).",
                    StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/rutas?mensaje=" + mensaje);
        } catch (NegocioException e) {
            request.setAttribute("mensajeExito", e.getMessage());
            doGet(request, response);
        }
    }

    private RutaAprendizajeService obtenerServicio(HttpServletRequest request) {
        if (rutaService != null) {
            return rutaService;
        }
        return ProveedorServiciosWeb.obtenerRutaService(request.getServletContext());
    }

    private static long leerIdRuta(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
