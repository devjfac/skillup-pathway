package co.edu.sena.skillup.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CifradorContrasena: hash PBKDF2 con sal")
class CifradorContrasenaTest {

    private final CifradorContrasena cifrador = new CifradorContrasena(1_000);

    @Test
    @DisplayName("Verifica la contraseña correcta y rechaza una distinta")
    void verificaContrasenaCorrecta() {
        String hash = cifrador.cifrar("Skillup#2026");

        assertTrue(cifrador.verificar("Skillup#2026", hash));
        assertFalse(cifrador.verificar("skillup#2026", hash));
    }

    @Test
    @DisplayName("Genera un hash distinto cada vez gracias a la sal aleatoria")
    void generaHashDistintoConCadaSal() {
        assertNotEquals(cifrador.cifrar("Skillup#2026"), cifrador.cifrar("Skillup#2026"));
    }

    @Test
    @DisplayName("Rechaza valores almacenados con formato dañado")
    void rechazaHashDaniado() {
        assertFalse(cifrador.verificar("Skillup#2026", "texto-plano"));
        assertFalse(cifrador.verificar("Skillup#2026", "pbkdf2_sha256$x$y$z"));
        assertFalse(cifrador.verificar("Skillup#2026", null));
    }
}
