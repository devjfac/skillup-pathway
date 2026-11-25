package co.edu.sena.skillup.service;

import co.edu.sena.skillup.dto.RutaAprendizajeDTO;
import co.edu.sena.skillup.exception.RecursoNoEncontradoException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de negocio para la consulta del catálogo de rutas de aprendizaje e inscripción
 * desde las vistas JSP y Servlets (RF-04, RF-05 y RF-06).
 */
public class RutaAprendizajeService {

    private static final int INCREMENTO_AVANCE = 25;
    private static final int AVANCE_MAXIMO = 100;

    private final Map<Long, RutaAprendizajeDTO> catalogo = new ConcurrentHashMap<>();

    /**
     * Crea el servicio e inicializa las rutas formativas del proyecto SkillUp Pathway.
     */
    public RutaAprendizajeService() {
        cargarRutasIniciales();
    }

    /**
     * Lista todas las rutas o aquellas que pertenecen al área de conocimiento indicada (RF-04).
     *
     * @param areaConocimiento nombre del área a filtrar; si es vacío devuelve todo el catálogo
     * @return lista ordenada de rutas formativas
     */
    public List<RutaAprendizajeDTO> listarRutas(String areaConocimiento) {
        return catalogo.values().stream()
                .filter(r -> areaConocimiento == null
                        || areaConocimiento.isBlank()
                        || r.areaConocimiento().equalsIgnoreCase(areaConocimiento.trim()))
                .sorted(Comparator.comparing(RutaAprendizajeDTO::idRuta))
                .toList();
    }

    /**
     * Inscribe al aprendiz en la ruta seleccionada o incrementa su porcentaje de avance
     * si ya estaba inscrito (RF-05, RF-06).
     *
     * @param idRuta identificador de la ruta de aprendizaje
     * @return ruta actualizada con el nuevo estado de inscripción y avance
     * @throws RecursoNoEncontradoException si el identificador de ruta no existe
     */
    public RutaAprendizajeDTO inscribirOAvanzar(long idRuta) {
        RutaAprendizajeDTO actual = catalogo.get(idRuta);
        if (actual == null) {
            throw new RecursoNoEncontradoException("No existe la ruta formativa #" + idRuta + ".");
        }
        int nuevoAvance = actual.inscrito()
                ? Math.min(AVANCE_MAXIMO, actual.porcentajeAvance() + INCREMENTO_AVANCE)
                : 0;
        RutaAprendizajeDTO actualizada = new RutaAprendizajeDTO(
                actual.idRuta(),
                actual.titulo(),
                actual.areaConocimiento(),
                actual.descripcion(),
                actual.duracionHoras(),
                true,
                nuevoAvance);
        catalogo.put(idRuta, actualizada);
        return actualizada;
    }

    /**
     * Cuenta cuántas rutas tienen inscripción activa.
     *
     * @return cantidad de rutas inscritas
     */
    public long contarInscripcionesActivas() {
        return catalogo.values().stream().filter(RutaAprendizajeDTO::inscrito).count();
    }

    private void cargarRutasIniciales() {
        registrar(new RutaAprendizajeDTO(
                1L,
                "Desarrollo Web Empresarial con Java, Servlets, JSP y Spring Boot",
                "Programación",
                "Conectividad JDBC, controladores HttpServlet (GET/POST), JSP, JSTL y API REST.",
                96,
                true,
                50));
        registrar(new RutaAprendizajeDTO(
                2L,
                "Maquetación Web Semántica, Accesibilidad WCAG 2.2 y React 19",
                "Desarrollo Web",
                "Diseño de interfaces responsivas, formularios HTML5 y heurísticas de Nielsen.",
                64,
                true,
                75));
        registrar(new RutaAprendizajeDTO(
                3L,
                "Modelado Relacional y Optimización SQL en MySQL 8",
                "Bases de Datos",
                "Normalización, integridad referencial, transacciones InnoDB y consultas SQL.",
                48,
                false,
                0));
        registrar(new RutaAprendizajeDTO(
                4L,
                "Seguridad en Aplicaciones Web, Cifrado PBKDF2 y Autenticación JWT",
                "Ciberseguridad",
                "Protección contra inyección SQL, XSS, bloqueo por intentos fallidos y tokens JWT.",
                56,
                false,
                0));
    }

    private void registrar(RutaAprendizajeDTO ruta) {
        catalogo.put(ruta.idRuta(), ruta);
    }
}
