package co.edu.sena.skillup.consola;

import co.edu.sena.skillup.dto.ActualizacionUsuarioDTO;
import co.edu.sena.skillup.dto.RegistroUsuarioDTO;
import co.edu.sena.skillup.dto.UsuarioDTO;
import co.edu.sena.skillup.entity.NombreRol;
import co.edu.sena.skillup.entity.TipoDocumento;
import co.edu.sena.skillup.exception.DatosInvalidosException;
import co.edu.sena.skillup.service.ValidadorUsuario;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Formularios y presentación de usuarios en la consola.
 */
public class VistaUsuario {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String FORMATO_FILA = "%-4s %-26s %-30s %-16s %-9s %s";
    private static final int ANCHO_MAXIMO_NOMBRE = 26;
    private static final int ANCHO_MAXIMO_CORREO = 30;

    private final Consola consola;

    /**
     * Crea la vista sobre la consola recibida.
     *
     * @param consola entrada y salida de texto
     */
    public VistaUsuario(Consola consola) {
        this.consola = consola;
    }

    /**
     * Pide los datos para registrar una cuenta nueva.
     *
     * @return datos digitados
     * @throws DatosInvalidosException si las dos contraseñas digitadas no coinciden
     */
    public RegistroUsuarioDTO leerDatosRegistro() {
        TipoDocumento tipoDocumento = leerTipoDocumento();
        String numeroDocumento = consola.leerTexto("Número de documento");
        String nombres = consola.leerTexto("Nombres");
        String apellidos = consola.leerTexto("Apellidos");
        String correoElectronico = consola.leerTexto("Correo electrónico");
        String contrasena = leerContrasenaNueva("Contraseña");
        return new RegistroUsuarioDTO(
                tipoDocumento, numeroDocumento, nombres, apellidos, correoElectronico, contrasena);
    }

    /**
     * Pide los datos nuevos de un usuario; con Enter se conserva el valor actual.
     *
     * @param actual datos vigentes del usuario
     * @return datos digitados
     */
    public ActualizacionUsuarioDTO leerDatosActualizacion(UsuarioDTO actual) {
        consola.mostrar("Presione Enter para conservar el valor que aparece entre corchetes.");
        TipoDocumento tipoDocumento = actual.tipoDocumento();
        if (consola.confirmar("¿Desea cambiar el tipo de documento (" + tipoDocumento + ")?")) {
            tipoDocumento = leerTipoDocumento();
        }
        return new ActualizacionUsuarioDTO(
                tipoDocumento,
                consola.leerTextoConValorActual("Número de documento", actual.numeroDocumento()),
                consola.leerTextoConValorActual("Nombres", actual.nombres()),
                consola.leerTextoConValorActual("Apellidos", actual.apellidos()),
                consola.leerTextoConValorActual(
                        "Correo electrónico", actual.correoElectronico()));
    }

    /**
     * Pide una contraseña nueva dos veces y verifica que coincidan.
     *
     * @param etiqueta texto que se muestra antes de leer
     * @return contraseña digitada
     * @throws DatosInvalidosException si las dos contraseñas no coinciden
     */
    public String leerContrasenaNueva(String etiqueta) {
        consola.mostrar("Política: " + ValidadorUsuario.POLITICA_CONTRASENA);
        String contrasena = consola.leerContrasena(etiqueta);
        String confirmacion = consola.leerContrasena("Confirme la contraseña");
        if (!contrasena.equals(confirmacion)) {
            throw new DatosInvalidosException(List.of("Las contraseñas no coinciden."));
        }
        return contrasena;
    }

    /**
     * Muestra la lista de tipos de documento y lee la opción elegida.
     *
     * @return tipo de documento elegido
     */
    public TipoDocumento leerTipoDocumento() {
        TipoDocumento[] tipos = TipoDocumento.values();
        consola.mostrar("Tipos de documento:");
        for (int indice = 0; indice < tipos.length; indice++) {
            consola.mostrar("  " + (indice + 1) + ". " + tipos[indice].getDescripcion()
                    + " (" + tipos[indice] + ")");
        }
        return tipos[consola.leerOpcion("Tipo de documento", 1, tipos.length) - 1];
    }

    /**
     * Muestra la lista de roles y lee la opción elegida.
     *
     * @param etiqueta texto que se muestra antes de leer
     * @return rol elegido
     */
    public NombreRol leerRol(String etiqueta) {
        NombreRol[] roles = NombreRol.values();
        consola.mostrar("Roles disponibles:");
        for (int indice = 0; indice < roles.length; indice++) {
            consola.mostrar("  " + (indice + 1) + ". " + roles[indice].getEtiqueta());
        }
        return roles[consola.leerOpcion(etiqueta, 1, roles.length) - 1];
    }

    /**
     * Muestra todos los datos de un usuario.
     *
     * @param usuario usuario que se muestra
     */
    public void mostrarDetalle(UsuarioDTO usuario) {
        consola.mostrar("  Identificador : " + usuario.idUsuario());
        consola.mostrar("  Documento     : " + usuario.tipoDocumento() + " "
                + usuario.numeroDocumento());
        consola.mostrar("  Nombre        : " + usuario.nombreCompleto());
        consola.mostrar("  Correo        : " + usuario.correoElectronico());
        consola.mostrar("  Roles         : " + describirRoles(usuario));
        consola.mostrar("  Estado        : " + describirEstado(usuario));
        if (usuario.fechaCreacion() != null) {
            consola.mostrar("  Registrado el : " + usuario.fechaCreacion().format(FORMATO_FECHA));
        }
    }

    /**
     * Muestra una tabla con los usuarios recibidos.
     *
     * @param usuarios usuarios que se muestran
     */
    public void mostrarListado(List<UsuarioDTO> usuarios) {
        if (usuarios.isEmpty()) {
            consola.mostrar("No se encontraron usuarios.");
            return;
        }
        consola.mostrar(String.format(
                FORMATO_FILA, "ID", "Nombre", "Correo", "Documento", "Estado", "Roles"));
        consola.mostrar("-".repeat(110));
        for (UsuarioDTO usuario : usuarios) {
            consola.mostrar(String.format(FORMATO_FILA,
                    usuario.idUsuario(),
                    recortar(usuario.nombreCompleto(), ANCHO_MAXIMO_NOMBRE),
                    recortar(usuario.correoElectronico(), ANCHO_MAXIMO_CORREO),
                    usuario.tipoDocumento() + " " + usuario.numeroDocumento(),
                    describirEstado(usuario),
                    describirRoles(usuario)));
        }
        consola.mostrar("Total de usuarios: " + usuarios.size());
    }

    private static String describirRoles(UsuarioDTO usuario) {
        return usuario.roles().stream()
                .map(NombreRol::getEtiqueta)
                .collect(Collectors.joining(", "));
    }

    private static String describirEstado(UsuarioDTO usuario) {
        return usuario.activo() ? "Activo" : "Inactivo";
    }

    private static String recortar(String texto, int anchoMaximo) {
        return texto.length() <= anchoMaximo ? texto : texto.substring(0, anchoMaximo - 1) + "…";
    }
}
