package co.edu.sena.skillup.repository;

import co.edu.sena.skillup.entity.NombreRol;
import co.edu.sena.skillup.entity.TipoDocumento;
import co.edu.sena.skillup.entity.Usuario;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementación en memoria de {@link UsuarioRepository} para ejecución del contenedor
 * de Servlets y pruebas automatizadas cuando no se dispone de una instancia activa de MySQL.
 */
public class MemoriaUsuarioRepository implements UsuarioRepository {

    private final Map<Long, Usuario> almacenamiento = new ConcurrentHashMap<>();
    private final AtomicLong secuenciaId = new AtomicLong(1L);

    /**
     * Crea el repositorio en memoria vacío.
     */
    public MemoriaUsuarioRepository() {
        // Inicialización sin registros previos.
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        if (usuario.getRoles().isEmpty()) {
            throw new IllegalArgumentException("El usuario debe tener al menos un rol");
        }
        long id = secuenciaId.getAndIncrement();
        LocalDateTime ahora = LocalDateTime.now();
        Usuario copia = copiarUsuario(usuario);
        copia.setIdUsuario(id);
        copia.setFechaCreacion(ahora);
        copia.setFechaActualizacion(ahora);
        almacenamiento.put(id, copia);
        return copiarUsuario(copia);
    }

    @Override
    public Optional<Usuario> buscarPorId(long idUsuario) {
        Usuario encontrado = almacenamiento.get(idUsuario);
        return encontrado == null ? Optional.empty() : Optional.of(copiarUsuario(encontrado));
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correoElectronico) {
        if (correoElectronico == null) {
            return Optional.empty();
        }
        String buscado = correoElectronico.trim().toLowerCase(Locale.ROOT);
        return almacenamiento.values().stream()
                .filter(u -> buscado.equalsIgnoreCase(u.getCorreoElectronico()))
                .findFirst()
                .map(MemoriaUsuarioRepository::copiarUsuario);
    }

    @Override
    public List<Usuario> listarTodos() {
        return almacenamiento.values().stream()
                .sorted(Comparator.comparing(Usuario::getIdUsuario))
                .map(MemoriaUsuarioRepository::copiarUsuario)
                .toList();
    }

    @Override
    public List<Usuario> buscarPorTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return listarTodos();
        }
        String filtro = texto.trim().toLowerCase(Locale.ROOT);
        List<Usuario> resultado = new ArrayList<>();
        for (Usuario u : listarTodos()) {
            if (coincideTexto(u, filtro)) {
                resultado.add(u);
            }
        }
        return resultado;
    }

    @Override
    public boolean existeCorreo(String correoElectronico, Long idUsuarioExcluido) {
        if (correoElectronico == null) {
            return false;
        }
        String buscado = correoElectronico.trim().toLowerCase(Locale.ROOT);
        return almacenamiento.values().stream()
                .anyMatch(u -> !u.getIdUsuario().equals(idUsuarioExcluido)
                        && buscado.equalsIgnoreCase(u.getCorreoElectronico()));
    }

    @Override
    public boolean existeDocumento(
            TipoDocumento tipoDocumento, String numeroDocumento, Long idUsuarioExcluido) {
        if (tipoDocumento == null || numeroDocumento == null) {
            return false;
        }
        return almacenamiento.values().stream()
                .anyMatch(u -> !u.getIdUsuario().equals(idUsuarioExcluido)
                        && tipoDocumento == u.getTipoDocumento()
                        && numeroDocumento.equalsIgnoreCase(u.getNumeroDocumento()));
    }

    @Override
    public long contarActivosPorRol(NombreRol rol) {
        return almacenamiento.values().stream()
                .filter(u -> u.isActivo() && u.tieneRol(rol))
                .count();
    }

    @Override
    public boolean actualizarDatos(Usuario usuario) {
        Usuario actual = almacenamiento.get(usuario.getIdUsuario());
        if (actual == null) {
            return false;
        }
        actual.setTipoDocumento(usuario.getTipoDocumento());
        actual.setNumeroDocumento(usuario.getNumeroDocumento());
        actual.setNombres(usuario.getNombres());
        actual.setApellidos(usuario.getApellidos());
        actual.setCorreoElectronico(usuario.getCorreoElectronico());
        actual.setFechaActualizacion(LocalDateTime.now());
        return true;
    }

    @Override
    public boolean actualizarContrasena(long idUsuario, String contrasenaHash) {
        Usuario actual = almacenamiento.get(idUsuario);
        if (actual == null) {
            return false;
        }
        actual.setContrasenaHash(contrasenaHash);
        actual.setFechaActualizacion(LocalDateTime.now());
        return true;
    }

    @Override
    public boolean actualizarEstado(long idUsuario, boolean activo) {
        Usuario actual = almacenamiento.get(idUsuario);
        if (actual == null) {
            return false;
        }
        actual.setActivo(activo);
        actual.setFechaActualizacion(LocalDateTime.now());
        return true;
    }

    @Override
    public boolean actualizarControlAcceso(
            long idUsuario, int intentosFallidos, LocalDateTime bloqueadoHasta) {
        Usuario actual = almacenamiento.get(idUsuario);
        if (actual == null) {
            return false;
        }
        actual.setIntentosFallidos(intentosFallidos);
        actual.setBloqueadoHasta(bloqueadoHasta);
        actual.setFechaActualizacion(LocalDateTime.now());
        return true;
    }

    @Override
    public boolean asignarRol(long idUsuario, NombreRol rol) {
        Usuario actual = almacenamiento.get(idUsuario);
        if (actual == null || actual.tieneRol(rol)) {
            return false;
        }
        actual.agregarRol(rol);
        return true;
    }

    @Override
    public boolean quitarRol(long idUsuario, NombreRol rol) {
        Usuario actual = almacenamiento.get(idUsuario);
        if (actual == null || !actual.tieneRol(rol)) {
            return false;
        }
        actual.quitarRol(rol);
        return true;
    }

    @Override
    public boolean eliminar(long idUsuario) {
        return almacenamiento.remove(idUsuario) != null;
    }

    private static boolean coincideTexto(Usuario u, String filtro) {
        return u.getNombres().toLowerCase(Locale.ROOT).contains(filtro)
                || u.getApellidos().toLowerCase(Locale.ROOT).contains(filtro)
                || u.getCorreoElectronico().toLowerCase(Locale.ROOT).contains(filtro)
                || u.getNumeroDocumento().toLowerCase(Locale.ROOT).contains(filtro);
    }

    private static Usuario copiarUsuario(Usuario origen) {
        Usuario copia = new Usuario();
        copia.setIdUsuario(origen.getIdUsuario());
        copia.setTipoDocumento(origen.getTipoDocumento());
        copia.setNumeroDocumento(origen.getNumeroDocumento());
        copia.setNombres(origen.getNombres());
        copia.setApellidos(origen.getApellidos());
        copia.setCorreoElectronico(origen.getCorreoElectronico());
        copia.setContrasenaHash(origen.getContrasenaHash());
        copia.setActivo(origen.isActivo());
        copia.setIntentosFallidos(origen.getIntentosFallidos());
        copia.setBloqueadoHasta(origen.getBloqueadoHasta());
        copia.setFechaCreacion(origen.getFechaCreacion());
        copia.setFechaActualizacion(origen.getFechaActualizacion());
        for (NombreRol rol : origen.getRoles()) {
            copia.agregarRol(rol);
        }
        return copia;
    }
}
