package gt.edu.umg.lecciones.leccion.dto;

import gt.edu.umg.lecciones.leccion.model.Objetivo;

/** Elemento de la lista de lecciones disponibles. */
public record LeccionResumenDto(String id, String titulo, Objetivo objetivo) {
}
