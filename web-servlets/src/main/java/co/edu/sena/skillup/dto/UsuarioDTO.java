package co.edu.sena.skillup.dto;

import co.edu.sena.skillup.entity.NombreRol;
import co.edu.sena.skillup.entity.TipoDocumento;
import co.edu.sena.skillup.entity.Usuario;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Vista de solo lectura de un usuario que se entrega a la capa de presentación.
 *
 * <p>No incluye el hash de la contraseña ni los datos de control de acceso.</p>
 *
 * @param idUsuario identificador del usuario
 * @param tipoDocumento tipo de documento de identidad
 * @param numeroDocumento número del documento de identidad
 * @param nombres nombres del usuario
 * @param apellidos apellidos del usuario
 * @param correoElectronico correo de inicio de sesión
 * @param activo indica si la cuenta está habilitada
 * @param roles roles asignados al usuario
 * @param fechaCreacion fecha y hora de registro
 */
public record UsuarioDTO(
        Long idUsuario,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String correoElectronico,
        boolean activo,
        Set<NombreRol> roles,
        LocalDateTime fechaCreacion) {

    /**
     * Copia los roles en un conjunto inmutable ordenado según la enumeración.
     */
    public UsuarioDTO {
        roles = roles == null || roles.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(roles));
    }

    /**
     * Convierte una entidad en su DTO de salida.
     *
     * @param usuario entidad persistida
     * @return DTO sin credenciales
     */
    public static UsuarioDTO desdeEntidad(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getIdUsuario(),
                usuario.getTipoDocumento(),
                usuario.getNumeroDocumento(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreoElectronico(),
                usuario.isActivo(),
                usuario.getRoles(),
                usuario.getFechaCreacion());
    }

    /**
     * Devuelve los nombres y apellidos separados por un espacio.
     *
     * @return nombre completo del usuario
     */
    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }

    /**
     * Indica si el usuario tiene el rol de administrador.
     *
     * @return {@code true} si puede gestionar cuentas de usuario
     */
    public boolean esAdministrador() {
        return roles.contains(NombreRol.ADMINISTRADOR);
    }
}
