package gt.edu.umg.lecciones.comun.error;

import java.time.Instant;

/**
 * Cuerpo JSON de todas las respuestas de error de la API.
 *
 * @param timestamp momento del error (ISO-8601, UTC)
 * @param estado    código HTTP
 * @param error     nombre corto del código HTTP (ej. "Not Found")
 * @param mensaje   explicación para la persona que usa la API
 * @param ruta      ruta solicitada
 */
public record ErrorRespuesta(Instant timestamp, int estado, String error, String mensaje, String ruta) {
}
