package co.edu.sena.skillup.dto;

import co.edu.sena.skillup.entity.TipoDocumento;

/**
 * Datos de entrada para actualizar el perfil de un usuario (RF-03 y RF-09).
 *
 * @param tipoDocumento tipo de documento de identidad
 * @param numeroDocumento número del documento de identidad
 * @param nombres nombres del usuario
 * @param apellidos apellidos del usuario
 * @param correoElectronico correo de inicio de sesión
 */
public record ActualizacionUsuarioDTO(
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String correoElectronico) {
}
