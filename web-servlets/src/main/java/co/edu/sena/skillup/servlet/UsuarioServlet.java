package co.edu.sena.skillup.servlet;

import co.edu.sena.skillup.dto.ActualizacionUsuarioDTO;
import co.edu.sena.skillup.dto.RegistroUsuarioDTO;
import co.edu.sena.skillup.dto.UsuarioDTO;
import co.edu.sena.skillup.entity.NombreRol;
import co.edu.sena.skillup.entity.TipoDocumento;
import co.edu.sena.skillup.exception.DatosInvalidosException;
import co.edu.sena.skillup.exception.NegocioException;
import co.edu.sena.skillup.service.UsuarioService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Controlador web Jakarta Servlet para la gestión integral de cuentas de usuario (CRUD).
 *
 * <p>Atiende las peticiones HTTP en la ruta {@code /usuarios} mediante los métodos
 * {@link #doGet} (consulta con filtros por texto, rol y estado, carga de formulario de
 * registro y carga de formulario de edición) y {@link #doPost} (registro de cuenta RF-01,
 * actualización de perfil RF-03, cambio de estado y eliminación RF-09).</p>
 */
@WebServlet(name = "UsuarioServlet", urlPatterns = {"/usuarios"})
public class UsuarioServlet extends HttpServlet {

    public static final String VISTA_LISTADO = "/WEB-INF/vistas/usuarios.jsp";
    public static final String VISTA_FORMULARIO = "/WEB-INF/vistas/formulario-usuario.jsp";

    private static final long serialVersionUID = 1L;
    private static final long ID_ADMIN_POR_DEFECTO = 999_999L;

    private transient UsuarioService usuarioService;

    /**
     * Crea el servlet para su despliegue en el contenedor Apache Tomcat 10.
     */
    public UsuarioServlet() {
        // El servicio se obtiene en init() desde el ServletContext.
    }

    /**
     * Crea el servlet inyectando explícitamente el servicio de negocio para pruebas unitarias.
     *
     * @param usuarioService servicio de gestión de usuarios
     */
    public UsuarioServlet(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void init() throws ServletException {
        if (usuarioService == null && getServletContext() != null) {
            usuarioService = ProveedorServiciosWeb.obtenerUsuarioService(getServletContext());
        }
    }

    /**
     * Procesa las peticiones HTTP {@code GET} para listar, filtrar o mostrar formularios.
     *
     * @param request petición HTTP entrante con parámetros en la cadena de consulta
     * @param response respuesta HTTP saliente
     * @throws ServletException si ocurre un error al despachar la vista JSP
     * @throws IOException si ocurre un error de entrada/salida
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        asegurarCodificacion(request, response);
        String accion = obtenerParametro(request, "accion", "listar");

        switch (accion) {
            case "nuevo" -> mostrarFormularioNuevo(request, response);
            case "editar" -> mostrarFormularioEdicion(request, response);
            default -> listarUsuariosConFiltros(request, response);
        }
    }

    /**
     * Procesa las peticiones HTTP {@code POST} enviadas desde los formularios HTML y JSP.
     *
     * @param request petición HTTP con los campos del formulario en el cuerpo
     * @param response respuesta HTTP saliente
     * @throws ServletException si ocurre un error al reenviar a la vista JSP
     * @throws IOException si ocurre un error al redirigir la respuesta
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        asegurarCodificacion(request, response);
        String accion = obtenerParametro(request, "accion", "registrar");

        switch (accion) {
            case "actualizar" -> procesarActualizacion(request, response);
            case "cambiarEstado" -> procesarCambioEstado(request, response);
            case "eliminar" -> procesarEliminacion(request, response);
            default -> procesarRegistro(request, response);
        }
    }

    private void listarUsuariosConFiltros(
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String busqueda = obtenerParametro(request, "q", "");
        String filtroRol = obtenerParametro(request, "rol", "");
        String filtroEstado = obtenerParametro(request, "estado", "");

        List<UsuarioDTO> base = busqueda.isBlank()
                ? obtenerServicio(request).listarUsuarios()
                : obtenerServicio(request).buscarUsuarios(busqueda);

        List<UsuarioDTO> filtrados = base.stream()
                .filter(u -> cumpleFiltroRol(u, filtroRol))
                .filter(u -> cumpleFiltroEstado(u, filtroEstado))
                .toList();

        request.setAttribute("usuarios", filtrados);
        request.setAttribute("totalRegistrados", obtenerServicio(request).listarUsuarios().size());
        request.setAttribute("filtroBusqueda", busqueda);
        request.setAttribute("filtroRol", filtroRol);
        request.setAttribute("filtroEstado", filtroEstado);
        request.setAttribute("rolesDisponibles", NombreRol.values());
        request.getRequestDispatcher(VISTA_LISTADO).forward(request, response);
    }

    private void mostrarFormularioNuevo(
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepararAtributosCatalogo(request, false, NombreRol.ESTUDIANTE.name());
        ActualizacionUsuarioDTO vacio = new ActualizacionUsuarioDTO(
                TipoDocumento.CE, "", "", "", "");
        request.setAttribute("usuarioFormulario", crearDtoBorrador(null, vacio));
        request.getRequestDispatcher(VISTA_FORMULARIO).forward(request, response);
    }

    private void mostrarFormularioEdicion(
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long idUsuario = leerIdentificador(request.getParameter("id"));
        try {
            UsuarioDTO encontrado = obtenerServicio(request).consultarPorId(idUsuario);
            String rolActual = encontrado.roles().stream()
                    .findFirst()
                    .map(Enum::name)
                    .orElse(NombreRol.ESTUDIANTE.name());
            prepararAtributosCatalogo(request, true, rolActual);
            request.setAttribute("usuarioFormulario", encontrado);
            request.getRequestDispatcher(VISTA_FORMULARIO).forward(request, response);
        } catch (NegocioException e) {
            request.setAttribute("mensajeError", e.getMessage());
            listarUsuariosConFiltros(request, response);
        }
    }

    private void procesarRegistro(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        TipoDocumento tipoDoc = parsearTipoDocumento(request.getParameter("tipoDocumento"));
        String numeroDoc = obtenerParametro(request, "numeroDocumento", "");
        String nombres = obtenerParametro(request, "nombres", "");
        String apellidos = obtenerParametro(request, "apellidos", "");
        String correo = obtenerParametro(request, "correoElectronico", "");
        String contrasena = obtenerParametro(request, "contrasena", "");
        NombreRol rol = parsearRol(request.getParameter("rol"));

        RegistroUsuarioDTO dto = new RegistroUsuarioDTO(
                tipoDoc, numeroDoc, nombres, apellidos, correo, contrasena);
        ActualizacionUsuarioDTO datosBorrador = new ActualizacionUsuarioDTO(
                tipoDoc, numeroDoc, nombres, apellidos, correo);
        try {
            UsuarioDTO creado = obtenerServicio(request).crearUsuario(dto, rol);
            redirigirConMensaje(request, response,
                    "Usuario registrado exitosamente: " + creado.nombreCompleto()
                    + " (" + creado.correoElectronico() + ").");
        } catch (DatosInvalidosException e) {
            request.setAttribute("erroresValidacion", e.getErrores());
            request.setAttribute("rolSeleccionado", rol.name());
            reenviarFormularioConBorrador(
                    request, response, false, crearDtoBorrador(null, datosBorrador));
        } catch (NegocioException e) {
            request.setAttribute("mensajeError", e.getMessage());
            request.setAttribute("rolSeleccionado", rol.name());
            reenviarFormularioConBorrador(
                    request, response, false, crearDtoBorrador(null, datosBorrador));
        }
    }

    private void procesarActualizacion(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long idUsuario = leerIdentificador(request.getParameter("idUsuario"));
        TipoDocumento tipoDoc = parsearTipoDocumento(request.getParameter("tipoDocumento"));
        String numeroDoc = obtenerParametro(request, "numeroDocumento", "");
        String nombres = obtenerParametro(request, "nombres", "");
        String apellidos = obtenerParametro(request, "apellidos", "");
        String correo = obtenerParametro(request, "correoElectronico", "");
        NombreRol rolNuevo = parsearRol(request.getParameter("rol"));

        ActualizacionUsuarioDTO dto = new ActualizacionUsuarioDTO(
                tipoDoc, numeroDoc, nombres, apellidos, correo);
        try {
            UsuarioDTO actualizado = obtenerServicio(request).actualizarDatos(idUsuario, dto);
            sincronizarRolPrincipal(obtenerServicio(request), actualizado, rolNuevo);
            redirigirConMensaje(request, response,
                    "Datos actualizados correctamente para el usuario #" + idUsuario + ".");
        } catch (DatosInvalidosException e) {
            request.setAttribute("erroresValidacion", e.getErrores());
            request.setAttribute("rolSeleccionado", rolNuevo.name());
            reenviarFormularioConBorrador(
                    request, response, true, crearDtoBorrador(idUsuario, dto));
        } catch (NegocioException e) {
            request.setAttribute("mensajeError", e.getMessage());
            request.setAttribute("rolSeleccionado", rolNuevo.name());
            reenviarFormularioConBorrador(
                    request, response, true, crearDtoBorrador(idUsuario, dto));
        }
    }

    private void procesarCambioEstado(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long idUsuario = leerIdentificador(request.getParameter("idUsuario"));
        boolean activo = Boolean.parseBoolean(request.getParameter("activo"));
        try {
            UsuarioDTO modificado = obtenerServicio(request).cambiarEstado(
                    idUsuario, activo, ID_ADMIN_POR_DEFECTO);
            String estadoTexto = modificado.activo() ? "activada" : "desactivada";
            redirigirConMensaje(request, response,
                    "Cuenta de " + modificado.nombreCompleto() + " " + estadoTexto + ".");
        } catch (NegocioException e) {
            request.setAttribute("mensajeError", e.getMessage());
            listarUsuariosConFiltros(request, response);
        }
    }

    private void procesarEliminacion(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long idUsuario = leerIdentificador(request.getParameter("idUsuario"));
        try {
            obtenerServicio(request).eliminarUsuario(idUsuario, ID_ADMIN_POR_DEFECTO);
            redirigirConMensaje(request, response,
                    "Usuario #" + idUsuario + " eliminado del directorio.");
        } catch (NegocioException e) {
            request.setAttribute("mensajeError", e.getMessage());
            listarUsuariosConFiltros(request, response);
        }
    }

    private void reenviarFormularioConBorrador(
            HttpServletRequest request,
            HttpServletResponse response,
            boolean modoEdicion,
            UsuarioDTO borrador) throws ServletException, IOException {
        String rol = obtenerParametro(request, "rol", NombreRol.ESTUDIANTE.name());
        prepararAtributosCatalogo(request, modoEdicion, rol);
        request.setAttribute("usuarioFormulario", borrador);
        request.getRequestDispatcher(VISTA_FORMULARIO).forward(request, response);
    }

    private static void sincronizarRolPrincipal(
            UsuarioService servicio, UsuarioDTO usuario, NombreRol rolNuevo) {
        if (!usuario.roles().contains(rolNuevo)) {
            servicio.asignarRol(usuario.idUsuario(), rolNuevo);
        }
    }

    private static void prepararAtributosCatalogo(
            HttpServletRequest request, boolean modoEdicion, String rolSeleccionado) {
        request.setAttribute("modoEdicion", modoEdicion);
        request.setAttribute("tiposDocumento", TipoDocumento.values());
        request.setAttribute("rolesDisponibles", NombreRol.values());
        request.setAttribute("rolSeleccionado", rolSeleccionado);
    }

    private static UsuarioDTO crearDtoBorrador(Long id, ActualizacionUsuarioDTO datos) {
        return new UsuarioDTO(
                id,
                datos.tipoDocumento(),
                datos.numeroDocumento(),
                datos.nombres(),
                datos.apellidos(),
                datos.correoElectronico(),
                true,
                Set.of(NombreRol.ESTUDIANTE),
                LocalDateTime.now());
    }

    private UsuarioService obtenerServicio(HttpServletRequest request) {
        if (usuarioService != null) {
            return usuarioService;
        }
        return ProveedorServiciosWeb.obtenerUsuarioService(request.getServletContext());
    }

    private static void redirigirConMensaje(
            HttpServletRequest request, HttpServletResponse response, String mensaje)
            throws IOException {
        String codificado = URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/usuarios?mensaje=" + codificado);
    }

    private static boolean cumpleFiltroRol(UsuarioDTO usuario, String filtroRol) {
        if (filtroRol == null || filtroRol.isBlank()) {
            return true;
        }
        return usuario.roles().stream()
                .anyMatch(r -> r.name().equalsIgnoreCase(filtroRol.trim()));
    }

    private static boolean cumpleFiltroEstado(UsuarioDTO usuario, String filtroEstado) {
        if (filtroEstado == null || filtroEstado.isBlank()) {
            return true;
        }
        if ("ACTIVO".equalsIgnoreCase(filtroEstado.trim())) {
            return usuario.activo();
        }
        if ("INACTIVO".equalsIgnoreCase(filtroEstado.trim())) {
            return !usuario.activo();
        }
        return true;
    }

    private static TipoDocumento parsearTipoDocumento(String valor) {
        if (valor == null || valor.isBlank()) {
            return TipoDocumento.CE;
        }
        try {
            return TipoDocumento.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return TipoDocumento.CE;
        }
    }

    private static NombreRol parsearRol(String valor) {
        if (valor == null || valor.isBlank()) {
            return NombreRol.ESTUDIANTE;
        }
        try {
            return NombreRol.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NombreRol.ESTUDIANTE;
        }
    }

    private static long leerIdentificador(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String obtenerParametro(
            HttpServletRequest request, String nombre, String porDefecto) {
        String valor = request.getParameter(nombre);
        return valor == null ? porDefecto : valor.trim();
    }

    private static void asegurarCodificacion(
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html; charset=UTF-8");
    }
}
