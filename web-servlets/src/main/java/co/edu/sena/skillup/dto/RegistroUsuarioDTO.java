package co.edu.sena.skillup.dto;

import co.edu.sena.skillup.entity.TipoDocumento;

/**
 * Datos de entrada para registrar una cuenta de usuario (RF-01, HU-01).
 *
 * @param tipoDocumento tipo de documento de identidad
 * @param numeroDocumento número del documento de identidad
 * @param nombres nombres del usuario
 * @param apellidos apellidos del usuario
 * @param correoElectronico correo con el que el usuario iniciará sesión
 * @param contrasena contraseña en texto plano; se cifra antes de guardarse
 */
public record RegistroUsuarioDTO(
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String correoElectronico,
        String contrasena) {

    @Override
    public String toString() {
        return "RegistroUsuarioDTO[correoElectronico=" + correoElectronico + "]";
    }
}
