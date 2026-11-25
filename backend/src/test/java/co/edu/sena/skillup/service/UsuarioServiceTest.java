package co.edu.sena.skillup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.sena.skillup.dto.ActualizacionUsuarioDTO;
import co.edu.sena.skillup.dto.RegistroUsuarioDTO;
import co.edu.sena.skillup.dto.UsuarioDTO;
import co.edu.sena.skillup.entity.NombreRol;
import co.edu.sena.skillup.entity.TipoDocumento;
import co.edu.sena.skillup.entity.Usuario;
import co.edu.sena.skillup.exception.CredencialesInvalidasException;
import co.edu.sena.skillup.exception.CuentaBloqueadaException;
import co.edu.sena.skillup.exception.DatosInvalidosException;
import co.edu.sena.skillup.exception.OperacionNoPermitidaException;
import co.edu.sena.skillup.exception.RecursoNoEncontradoException;
import co.edu.sena.skillup.exception.RegistroDuplicadoException;
import co.edu.sena.skillup.repository.UsuarioRepositoryEnMemoria;
import co.edu.sena.skillup.security.CifradorContrasena;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UsuarioService: reglas de negocio del módulo de usuarios")
class UsuarioServiceTest {

    private static final String CONTRASENA_VALIDA = "Skillup#2026";
    private static final int ITERACIONES_PRUEBA = 1_000;

    private UsuarioRepositoryEnMemoria usuarioRepository;
    private RelojAjustable reloj;
    private UsuarioService usuarioService;

    @BeforeEach
    void configurar() {
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        reloj = new RelojAjustable(Instant.parse("2026-11-24T15:00:00Z"));
        usuarioService = new UsuarioService(
                usuarioRepository, new CifradorContrasena(ITERACIONES_PRUEBA), reloj);
    }

    @Test
    @DisplayName("RF-01: registra un estudiante con la contraseña cifrada")
    void registraEstudianteConContrasenaCifrada() {
        UsuarioDTO creado = usuarioService.registrarEstudiante(registro("ana@skillup.co", "10101"));

        assertEquals(Set.of(NombreRol.ESTUDIANTE), creado.roles());
        assertTrue(creado.activo());
        Usuario almacenado = usuarioRepository.buscarPorId(creado.idUsuario()).orElseThrow();
        assertNotEquals(CONTRASENA_VALIDA, almacenado.getContrasenaHash());
        assertTrue(almacenado.getContrasenaHash().startsWith("pbkdf2_sha256$"));
    }

    @Test
    @DisplayName("RF-01: normaliza el correo y los espacios de los nombres")
    void normalizaCorreoYNombres() {
        RegistroUsuarioDTO datos = new RegistroUsuarioDTO(TipoDocumento.CC, " 20202 ",
                "  Luisa   Fernanda ", "Gómez", "  Luisa.Gomez@SkillUp.CO ", CONTRASENA_VALIDA);

        UsuarioDTO creado = usuarioService.registrarEstudiante(datos);

        assertEquals("luisa.gomez@skillup.co", creado.correoElectronico());
        assertEquals("Luisa Fernanda", creado.nombres());
        assertEquals("20202", creado.numeroDocumento());
    }

    @Test
    @DisplayName("HU-01 CA-01: rechaza un correo ya registrado")
    void rechazaCorreoDuplicado() {
        usuarioService.registrarEstudiante(registro("ana@skillup.co", "10101"));

        assertThrows(RegistroDuplicadoException.class,
                () -> usuarioService.registrarEstudiante(registro("ANA@skillup.co", "30303")));
    }

    @Test
    @DisplayName("Rechaza un documento ya registrado")
    void rechazaDocumentoDuplicado() {
        usuarioService.registrarEstudiante(registro("ana@skillup.co", "10101"));

        assertThrows(RegistroDuplicadoException.class,
                () -> usuarioService.registrarEstudiante(registro("otra@skillup.co", "10101")));
    }

    @Test
    @DisplayName("HU-01 CA-02: rechaza contraseñas que no cumplen la política")
    void rechazaContrasenaDebil() {
        RegistroUsuarioDTO datos = new RegistroUsuarioDTO(TipoDocumento.CC, "10101",
                "Ana", "Pérez", "ana@skillup.co", "debil123");

        DatosInvalidosException error = assertThrows(DatosInvalidosException.class,
                () -> usuarioService.registrarEstudiante(datos));

        assertEquals(2, error.getErrores().size());
        assertTrue(usuarioRepository.listarTodos().isEmpty());
    }

    @Test
    @DisplayName("RF-02: inicia sesión con credenciales correctas")
    void iniciaSesionConCredencialesCorrectas() {
        usuarioService.registrarEstudiante(registro("ana@skillup.co", "10101"));

        UsuarioDTO autenticado =
                usuarioService.iniciarSesion(" Ana@SkillUp.co ", CONTRASENA_VALIDA);

        assertEquals("ana@skillup.co", autenticado.correoElectronico());
    }

    @Test
    @DisplayName("HU-02 CA-02: usa el mismo mensaje si falla el correo o la contraseña")
    void usaMensajeGenericoAnteCredencialesInvalidas() {
        usuarioService.registrarEstudiante(registro("ana@skillup.co", "10101"));

        CredencialesInvalidasException contrasenaErrada = assertThrows(
                CredencialesInvalidasException.class,
                () -> usuarioService.iniciarSesion("ana@skillup.co", "Otra#Clave1"));
        CredencialesInvalidasException correoInexistente = assertThrows(
                CredencialesInvalidasException.class,
                () -> usuarioService.iniciarSesion("nadie@skillup.co", CONTRASENA_VALIDA));

        assertEquals(CredencialesInvalidasException.MENSAJE, contrasenaErrada.getMessage());
        assertEquals(contrasenaErrada.getMessage(), correoInexistente.getMessage());
    }

    @Test
    @DisplayName("HU-02 CA-03: bloquea la cuenta 15 minutos después de 5 intentos fallidos")
    void bloqueaCuentaDespuesDeCincoIntentosFallidos() {
        usuarioService.registrarEstudiante(registro("ana@skillup.co", "10101"));
        for (int intento = 1; intento < UsuarioService.MAXIMO_INTENTOS_FALLIDOS; intento++) {
            assertThrows(CredencialesInvalidasException.class,
                    () -> usuarioService.iniciarSesion("ana@skillup.co", "Errada#123"));
        }

        assertThrows(CuentaBloqueadaException.class,
                () -> usuarioService.iniciarSesion("ana@skillup.co", "Errada#123"));
        assertThrows(CuentaBloqueadaException.class,
                () -> usuarioService.iniciarSesion("ana@skillup.co", CONTRASENA_VALIDA));

        reloj.avanzar(Duration.ofMinutes(UsuarioService.MINUTOS_BLOQUEO + 1));
        UsuarioDTO autenticado = usuarioService.iniciarSesion("ana@skillup.co", CONTRASENA_VALIDA);
        assertEquals("ana@skillup.co", autenticado.correoElectronico());
    }

    @Test
    @DisplayName("Un ingreso exitoso reinicia el contador de intentos fallidos")
    void reiniciaIntentosDespuesDeIngresoExitoso() {
        UsuarioDTO creado = usuarioService.registrarEstudiante(registro("ana@skillup.co", "10101"));
        assertThrows(CredencialesInvalidasException.class,
                () -> usuarioService.iniciarSesion("ana@skillup.co", "Errada#123"));

        usuarioService.iniciarSesion("ana@skillup.co", CONTRASENA_VALIDA);

        Usuario almacenado = usuarioRepository.buscarPorId(creado.idUsuario()).orElseThrow();
        assertEquals(0, almacenado.getIntentosFallidos());
    }

    @Test
    @DisplayName("No permite iniciar sesión con una cuenta desactivada")
    void rechazaCuentaDesactivada() {
        UsuarioDTO administrador = crearAdministrador();
        UsuarioDTO estudiante = usuarioService.registrarEstudiante(
                registro("ana@skillup.co", "10101"));
        usuarioService.cambiarEstado(estudiante.idUsuario(), false, administrador.idUsuario());

        assertThrows(OperacionNoPermitidaException.class,
                () -> usuarioService.iniciarSesion("ana@skillup.co", CONTRASENA_VALIDA));
    }

    @Test
    @DisplayName("RF-03: actualiza los datos del perfil")
    void actualizaDatosDelPerfil() {
        UsuarioDTO creado = usuarioService.registrarEstudiante(registro("ana@skillup.co", "10101"));
        ActualizacionUsuarioDTO cambios = new ActualizacionUsuarioDTO(
                TipoDocumento.CE, "E55555", "Ana María", "Pérez Ruiz", "ana.maria@skillup.co");

        UsuarioDTO actualizado = usuarioService.actualizarDatos(creado.idUsuario(), cambios);

        assertEquals("Ana María", actualizado.nombres());
        assertEquals(TipoDocumento.CE, actualizado.tipoDocumento());
        assertEquals("ana.maria@skillup.co", actualizado.correoElectronico());
    }

    @Test
    @DisplayName("No permite cambiar el correo por uno de otro usuario")
    void rechazaActualizarConCorreoDeOtroUsuario() {
        usuarioService.registrarEstudiante(registro("ana@skillup.co", "10101"));
        UsuarioDTO segundo = usuarioService.registrarEstudiante(
                registro("beto@skillup.co", "20202"));
        ActualizacionUsuarioDTO cambios = new ActualizacionUsuarioDTO(
                TipoDocumento.CC, "20202", "Beto", "López", "ana@skillup.co");

        assertThrows(RegistroDuplicadoException.class,
                () -> usuarioService.actualizarDatos(segundo.idUsuario(), cambios));
    }

    @Test
    @DisplayName("RF-03: cambia la contraseña solo si la actual es correcta")
    void cambiaContrasenaVerificandoLaActual() {
        UsuarioDTO creado = usuarioService.registrarEstudiante(registro("ana@skillup.co", "10101"));

        assertThrows(DatosInvalidosException.class, () -> usuarioService.cambiarContrasena(
                creado.idUsuario(), "Errada#123", "Nueva#Clave9"));
        usuarioService.cambiarContrasena(creado.idUsuario(), CONTRASENA_VALIDA, "Nueva#Clave9");

        assertEquals(creado.idUsuario(),
                usuarioService.iniciarSesion("ana@skillup.co", "Nueva#Clave9").idUsuario());
    }

    @Test
    @DisplayName("RF-09: el administrador crea usuarios con el rol elegido y asigna roles")
    void administradorCreaUsuarioYAsignaRol() {
        UsuarioDTO instructor = usuarioService.crearUsuario(
                registro("profe@skillup.co", "40404"), NombreRol.INSTRUCTOR);

        UsuarioDTO conMentor = usuarioService.asignarRol(instructor.idUsuario(), NombreRol.MENTOR);

        assertEquals(Set.of(NombreRol.INSTRUCTOR, NombreRol.MENTOR), conMentor.roles());
        assertThrows(OperacionNoPermitidaException.class,
                () -> usuarioService.asignarRol(instructor.idUsuario(), NombreRol.MENTOR));
    }

    @Test
    @DisplayName("RF-09: no permite dejar a un usuario sin roles")
    void rechazaQuitarElUnicoRol() {
        UsuarioDTO administrador = crearAdministrador();
        UsuarioDTO estudiante = usuarioService.registrarEstudiante(
                registro("ana@skillup.co", "10101"));

        assertThrows(OperacionNoPermitidaException.class, () -> usuarioService.quitarRol(
                estudiante.idUsuario(), NombreRol.ESTUDIANTE, administrador.idUsuario()));
    }

    @Test
    @DisplayName("RF-09: elimina un usuario existente")
    void eliminaUsuario() {
        UsuarioDTO administrador = crearAdministrador();
        UsuarioDTO estudiante = usuarioService.registrarEstudiante(
                registro("ana@skillup.co", "10101"));

        usuarioService.eliminarUsuario(estudiante.idUsuario(), administrador.idUsuario());

        assertThrows(RecursoNoEncontradoException.class,
                () -> usuarioService.consultarPorId(estudiante.idUsuario()));
    }

    @Test
    @DisplayName("RF-09: el administrador no puede eliminarse ni desactivarse a sí mismo")
    void administradorNoPuedeEliminarseNiDesactivarse() {
        UsuarioDTO administrador = crearAdministrador();
        long id = administrador.idUsuario();

        assertThrows(OperacionNoPermitidaException.class,
                () -> usuarioService.eliminarUsuario(id, id));
        assertThrows(OperacionNoPermitidaException.class,
                () -> usuarioService.cambiarEstado(id, false, id));
        assertThrows(OperacionNoPermitidaException.class,
                () -> usuarioService.quitarRol(id, NombreRol.ADMINISTRADOR, id));
    }

    @Test
    @DisplayName("Solo permite crear el administrador inicial una vez")
    void creaAdministradorInicialUnaSolaVez() {
        assertFalse(usuarioService.existeAdministradorActivo());
        crearAdministrador();

        assertTrue(usuarioService.existeAdministradorActivo());
        assertThrows(OperacionNoPermitidaException.class,
                () -> usuarioService.registrarAdministradorInicial(
                        registro("otro.admin@skillup.co", "90909")));
    }

    @Test
    @DisplayName("Busca usuarios por texto y lista todos si el texto está vacío")
    void buscaUsuariosPorTexto() {
        usuarioService.registrarEstudiante(registro("ana@skillup.co", "10101"));
        usuarioService.registrarEstudiante(registro("beto@skillup.co", "20202"));

        assertEquals(1, usuarioService.buscarUsuarios("beto").size());
        assertEquals(2, usuarioService.buscarUsuarios("  ").size());
    }

    private UsuarioDTO crearAdministrador() {
        return usuarioService.registrarAdministradorInicial(registro("admin@skillup.co", "99999"));
    }

    private static RegistroUsuarioDTO registro(String correoElectronico, String numeroDocumento) {
        return new RegistroUsuarioDTO(TipoDocumento.CC, numeroDocumento, "Ana", "Pérez",
                correoElectronico, CONTRASENA_VALIDA);
    }
}
