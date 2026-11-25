package co.edu.sena.skillup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.sena.skillup.dto.RegistroUsuarioDTO;
import co.edu.sena.skillup.entity.TipoDocumento;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ValidadorUsuario: criterios de aceptación de HU-01")
class ValidadorUsuarioTest {

    @Test
    @DisplayName("Acepta un registro completo y válido")
    void aceptaRegistroValido() {
        RegistroUsuarioDTO datos = new RegistroUsuarioDTO(TipoDocumento.CC, "1012345678",
                "María José", "D'Angelo-Rúa", "maria.jose@skillup.co", "Clave#Segura1");

        assertTrue(ValidadorUsuario.validarRegistro(datos).isEmpty());
    }

    @Test
    @DisplayName("Informa todos los campos obligatorios vacíos")
    void informaCamposObligatorios() {
        RegistroUsuarioDTO datos = new RegistroUsuarioDTO(null, "", "", "", "", "");

        List<String> errores = ValidadorUsuario.validarRegistro(datos);

        assertEquals(6, errores.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"sin-arroba.co", "ana@", "ana@dominio", "ana @skillup.co"})
    @DisplayName("HU-01 CA-01: rechaza correos con formato inválido")
    void rechazaCorreoInvalido(String correo) {
        RegistroUsuarioDTO datos = new RegistroUsuarioDTO(
                TipoDocumento.CC, "10101", "Ana", "Pérez", correo, "Clave#Segura1");

        assertFalse(ValidadorUsuario.validarRegistro(datos).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Corta#1", "sinmayuscula#1", "SINMINUSCULA#1", "SinNumero#", "SinEspecial1"})
    @DisplayName("HU-01 CA-02: rechaza contraseñas que incumplen la política")
    void rechazaContrasenaDebil(String contrasena) {
        assertFalse(ValidadorUsuario.validarContrasena(contrasena).isEmpty());
    }

    @Test
    @DisplayName("HU-01 CA-02: acepta una contraseña que cumple la política")
    void aceptaContrasenaSegura() {
        assertTrue(ValidadorUsuario.validarContrasena("Skillup#2026").isEmpty());
    }
}
