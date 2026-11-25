package co.edu.sena.skillup.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.sena.skillup.config.ConexionBaseDatos;
import co.edu.sena.skillup.entity.NombreRol;
import co.edu.sena.skillup.entity.TipoDocumento;
import co.edu.sena.skillup.entity.Usuario;
import co.edu.sena.skillup.exception.RegistroDuplicadoException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Prueba de integración del CRUD JDBC contra una base de datos MySQL real.
 *
 * <p>Solo se ejecuta cuando existe la variable de entorno SKILLUP_DB_CONTRASENA, por
 * ejemplo con el contenedor de database/docker-compose.yml o en la integración continua.</p>
 */
@Tag("integracion")
@EnabledIfEnvironmentVariable(named = ConexionBaseDatos.VARIABLE_CONTRASENA, matches = ".+")
@DisplayName("JdbcUsuarioRepository: CRUD sobre MySQL")
class JdbcUsuarioRepositoryTest {

    private JdbcUsuarioRepository repositorio;
    private final List<Long> idsCreados = new ArrayList<>();

    @BeforeEach
    void configurar() {
        repositorio = new JdbcUsuarioRepository(ConexionBaseDatos.desdeConfiguracion());
    }

    @AfterEach
    void limpiar() {
        idsCreados.forEach(repositorio::eliminar);
    }

    @Test
    @DisplayName("Inserta, consulta, actualiza y elimina un usuario con sus roles")
    void ejecutaCrudCompleto() {
        Usuario nuevo = usuarioDePrueba();

        Usuario guardado = repositorio.guardar(nuevo);
        idsCreados.add(guardado.getIdUsuario());
        assertNotNull(guardado.getIdUsuario());
        assertNotNull(guardado.getFechaCreacion());
        assertEquals(Set.of(NombreRol.ESTUDIANTE), guardado.getRoles());

        Usuario consultado =
                repositorio.buscarPorCorreo(nuevo.getCorreoElectronico()).orElseThrow();
        assertEquals(guardado.getIdUsuario(), consultado.getIdUsuario());
        assertTrue(repositorio.listarTodos().stream()
                .anyMatch(usuario -> usuario.getIdUsuario().equals(guardado.getIdUsuario())));
        assertEquals(1, repositorio.buscarPorTexto(nuevo.getNumeroDocumento()).size());

        consultado.setNombres("Nombre Actualizado");
        assertTrue(repositorio.actualizarDatos(consultado));
        assertTrue(repositorio.actualizarEstado(guardado.getIdUsuario(), false));
        assertTrue(repositorio.asignarRol(guardado.getIdUsuario(), NombreRol.INSTRUCTOR));
        Usuario actualizado = repositorio.buscarPorId(guardado.getIdUsuario()).orElseThrow();
        assertEquals("Nombre Actualizado", actualizado.getNombres());
        assertFalse(actualizado.isActivo());
        assertEquals(Set.of(NombreRol.ESTUDIANTE, NombreRol.INSTRUCTOR), actualizado.getRoles());

        assertTrue(repositorio.quitarRol(guardado.getIdUsuario(), NombreRol.INSTRUCTOR));
        assertTrue(repositorio.eliminar(guardado.getIdUsuario()));
        assertTrue(repositorio.buscarPorId(guardado.getIdUsuario()).isEmpty());
        assertFalse(repositorio.eliminar(guardado.getIdUsuario()));
    }

    @Test
    @DisplayName("Rechaza un correo duplicado mediante la restricción uq_usuario_correo")
    void rechazaCorreoDuplicado() {
        Usuario primero = repositorio.guardar(usuarioDePrueba());
        idsCreados.add(primero.getIdUsuario());
        Usuario duplicado = usuarioDePrueba();
        duplicado.setCorreoElectronico(primero.getCorreoElectronico());

        assertThrows(RegistroDuplicadoException.class, () -> repositorio.guardar(duplicado));
        assertTrue(repositorio.existeCorreo(primero.getCorreoElectronico(), null));
        assertFalse(repositorio.existeCorreo(
                primero.getCorreoElectronico(), primero.getIdUsuario()));
    }

    @Test
    @DisplayName("Guarda y limpia los datos de control de acceso")
    void actualizaControlDeAcceso() {
        Usuario guardado = repositorio.guardar(usuarioDePrueba());
        idsCreados.add(guardado.getIdUsuario());
        LocalDateTime bloqueadoHasta = LocalDateTime.now().plusMinutes(15)
                .truncatedTo(ChronoUnit.SECONDS);

        repositorio.actualizarControlAcceso(guardado.getIdUsuario(), 0, bloqueadoHasta);
        assertEquals(bloqueadoHasta,
                repositorio.buscarPorId(guardado.getIdUsuario()).orElseThrow().getBloqueadoHasta());

        repositorio.actualizarControlAcceso(guardado.getIdUsuario(), 2, null);
        Usuario consultado = repositorio.buscarPorId(guardado.getIdUsuario()).orElseThrow();
        assertEquals(2, consultado.getIntentosFallidos());
        assertNull(consultado.getBloqueadoHasta());
    }

    private static Usuario usuarioDePrueba() {
        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Usuario usuario = new Usuario();
        usuario.setTipoDocumento(TipoDocumento.CC);
        usuario.setNumeroDocumento("T" + sufijo.toUpperCase());
        usuario.setNombres("Prueba");
        usuario.setApellidos("Integración");
        usuario.setCorreoElectronico("prueba." + sufijo + "@skillup.co");
        usuario.setContrasenaHash("pbkdf2_sha256$1$c2Fs$aGFzaA==");
        usuario.agregarRol(NombreRol.ESTUDIANTE);
        return usuario;
    }
}
