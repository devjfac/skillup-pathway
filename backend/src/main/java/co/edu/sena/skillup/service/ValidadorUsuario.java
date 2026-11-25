package co.edu.sena.skillup.service;

import co.edu.sena.skillup.dto.ActualizacionUsuarioDTO;
import co.edu.sena.skillup.dto.RegistroUsuarioDTO;
import co.edu.sena.skillup.entity.TipoDocumento;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reglas de normalización y validación de los datos de usuario.
 *
 * <p>Implementa los criterios de aceptación CA-01 (formato de correo) y CA-02 (política
 * de contraseñas) de la historia de usuario HU-01.</p>
 */
public final class ValidadorUsuario {

    public static final int LONGITUD_MINIMA_CONTRASENA = 8;
    public static final int LONGITUD_MAXIMA_CONTRASENA = 64;
    public static final String POLITICA_CONTRASENA = "Mínimo " + LONGITUD_MINIMA_CONTRASENA
            + " caracteres, con al menos una mayúscula, una minúscula, un número y un carácter"
            + " especial.";

    private static final int LONGITUD_MAXIMA_NOMBRE = 100;
    private static final int LONGITUD_MAXIMA_CORREO = 150;
    private static final Pattern PATRON_CORREO = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern PATRON_NOMBRE = Pattern.compile("^\\p{L}+([ '\\-]\\p{L}+)*$");
    private static final Pattern PATRON_DOCUMENTO = Pattern.compile("^[A-Z0-9]{5,20}$");
    private static final Pattern PATRON_ESPACIOS = Pattern.compile("\\s+");

    private ValidadorUsuario() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Normaliza los datos de registro: recorta espacios y pasa el correo a minúsculas.
     *
     * @param datos datos digitados por el usuario
     * @return copia normalizada de los datos
     */
    public static RegistroUsuarioDTO normalizar(RegistroUsuarioDTO datos) {
        return new RegistroUsuarioDTO(
                datos.tipoDocumento(),
                normalizarDocumento(datos.numeroDocumento()),
                normalizarNombre(datos.nombres()),
                normalizarNombre(datos.apellidos()),
                normalizarCorreo(datos.correoElectronico()),
                datos.contrasena());
    }

    /**
     * Normaliza los datos de actualización de perfil.
     *
     * @param datos datos digitados por el usuario
     * @return copia normalizada de los datos
     */
    public static ActualizacionUsuarioDTO normalizar(ActualizacionUsuarioDTO datos) {
        return new ActualizacionUsuarioDTO(
                datos.tipoDocumento(),
                normalizarDocumento(datos.numeroDocumento()),
                normalizarNombre(datos.nombres()),
                normalizarNombre(datos.apellidos()),
                normalizarCorreo(datos.correoElectronico()));
    }

    /**
     * Recorta espacios y convierte el correo electrónico a minúsculas.
     *
     * @param correoElectronico correo digitado
     * @return correo normalizado o {@code null} si se recibió {@code null}
     */
    public static String normalizarCorreo(String correoElectronico) {
        return correoElectronico == null ? null : correoElectronico.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Valida los datos de registro de un usuario nuevo.
     *
     * @param datos datos ya normalizados
     * @return lista de errores; vacía si los datos son válidos
     */
    public static List<String> validarRegistro(RegistroUsuarioDTO datos) {
        List<String> errores = new ArrayList<>();
        validarIdentificacion(datos.tipoDocumento(), datos.numeroDocumento(), errores);
        validarNombre("nombres", datos.nombres(), errores);
        validarNombre("apellidos", datos.apellidos(), errores);
        validarCorreo(datos.correoElectronico(), errores);
        errores.addAll(validarContrasena(datos.contrasena()));
        return errores;
    }

    /**
     * Valida los datos de actualización de perfil.
     *
     * @param datos datos ya normalizados
     * @return lista de errores; vacía si los datos son válidos
     */
    public static List<String> validarActualizacion(ActualizacionUsuarioDTO datos) {
        List<String> errores = new ArrayList<>();
        validarIdentificacion(datos.tipoDocumento(), datos.numeroDocumento(), errores);
        validarNombre("nombres", datos.nombres(), errores);
        validarNombre("apellidos", datos.apellidos(), errores);
        validarCorreo(datos.correoElectronico(), errores);
        return errores;
    }

    /**
     * Valida la política de contraseñas de HU-01, CA-02.
     *
     * @param contrasena contraseña en texto plano
     * @return lista de errores; vacía si la contraseña cumple la política
     */
    public static List<String> validarContrasena(String contrasena) {
        List<String> errores = new ArrayList<>();
        if (contrasena == null || contrasena.length() < LONGITUD_MINIMA_CONTRASENA) {
            errores.add("La contraseña debe tener mínimo " + LONGITUD_MINIMA_CONTRASENA
                    + " caracteres.");
            return errores;
        }
        if (contrasena.length() > LONGITUD_MAXIMA_CONTRASENA) {
            errores.add("La contraseña no puede superar " + LONGITUD_MAXIMA_CONTRASENA
                    + " caracteres.");
        }
        if (contrasena.chars().noneMatch(Character::isUpperCase)) {
            errores.add("La contraseña debe incluir al menos una letra mayúscula.");
        }
        if (contrasena.chars().noneMatch(Character::isLowerCase)) {
            errores.add("La contraseña debe incluir al menos una letra minúscula.");
        }
        if (contrasena.chars().noneMatch(Character::isDigit)) {
            errores.add("La contraseña debe incluir al menos un número.");
        }
        if (contrasena.chars().allMatch(Character::isLetterOrDigit)) {
            errores.add("La contraseña debe incluir al menos un carácter especial.");
        }
        return errores;
    }

    private static void validarIdentificacion(
            TipoDocumento tipoDocumento, String numeroDocumento, List<String> errores) {
        if (tipoDocumento == null) {
            errores.add("El tipo de documento es obligatorio.");
        }
        if (numeroDocumento == null || !PATRON_DOCUMENTO.matcher(numeroDocumento).matches()) {
            errores.add("El número de documento debe tener entre 5 y 20 letras o números,"
                    + " sin espacios ni puntos.");
        }
    }

    private static void validarNombre(String campo, String valor, List<String> errores) {
        if (valor == null || valor.isEmpty()) {
            errores.add("El campo " + campo + " es obligatorio.");
        } else if (valor.length() > LONGITUD_MAXIMA_NOMBRE) {
            errores.add("El campo " + campo + " no puede superar " + LONGITUD_MAXIMA_NOMBRE
                    + " caracteres.");
        } else if (!PATRON_NOMBRE.matcher(valor).matches()) {
            errores.add("El campo " + campo + " solo admite letras, espacios, apóstrofos y"
                    + " guiones.");
        }
    }

    private static void validarCorreo(String correoElectronico, List<String> errores) {
        if (correoElectronico == null || correoElectronico.isEmpty()) {
            errores.add("El correo electrónico es obligatorio.");
        } else if (correoElectronico.length() > LONGITUD_MAXIMA_CORREO
                || !PATRON_CORREO.matcher(correoElectronico).matches()) {
            errores.add("El correo electrónico no tiene un formato válido.");
        }
    }

    private static String normalizarNombre(String valor) {
        return valor == null ? null : PATRON_ESPACIOS.matcher(valor.trim()).replaceAll(" ");
    }

    private static String normalizarDocumento(String valor) {
        return valor == null ? null : valor.trim().toUpperCase(Locale.ROOT);
    }
}
