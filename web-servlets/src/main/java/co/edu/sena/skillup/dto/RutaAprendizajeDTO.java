package co.edu.sena.skillup.dto;

/**
 * Objeto de transferencia de datos para una ruta de aprendizaje en el catálogo web (RF-04, RF-05).
 *
 * @param idRuta identificador único de la ruta formativa
 * @param titulo título oficial de la ruta de aprendizaje
 * @param areaConocimiento área de conocimiento a la que pertenece
 * @param descripcion resumen académico de los módulos de la ruta
 * @param duracionHoras duración total estimada en horas
 * @param inscrito indica si el aprendiz actual se encuentra inscrito
 * @param porcentajeAvance porcentaje de progreso acumulado (RF-06)
 */
public record RutaAprendizajeDTO(
        Long idRuta,
        String titulo,
        String areaConocimiento,
        String descripcion,
        int duracionHoras,
        boolean inscrito,
        int porcentajeAvance) {
}
