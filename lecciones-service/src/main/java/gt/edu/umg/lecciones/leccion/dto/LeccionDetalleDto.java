package gt.edu.umg.lecciones.leccion.dto;

import gt.edu.umg.lecciones.leccion.model.Objetivo;

/**
 * Lección lista para mostrarse.
 *
 * @param html markdown ya renderizado y seguro: el HTML crudo viene escapado y las imágenes
 *             relativas apuntan a {@code /api/lecciones/{id}/recursos/{archivo}}
 */
public record LeccionDetalleDto(String id, String titulo, Objetivo objetivo, String html) {
}
