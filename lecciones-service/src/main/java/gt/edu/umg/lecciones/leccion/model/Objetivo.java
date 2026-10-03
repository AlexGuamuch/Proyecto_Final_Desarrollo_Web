package gt.edu.umg.lecciones.leccion.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Condición que debe cumplirse para dar por completada una lección.
 * Se lee de la línea {@code objetivo=VALOR} del archivo config.txt.
 */
public enum Objetivo {
    /** El estudiante compila y el compilador reporta al menos un error. */
    COMPILAR_CON_ERROR,
    /** El estudiante compila y el compilador reporta 0 errores. */
    COMPILAR_EXITOSO,
    /** Compila sin errores y al ejecutar el programa se produce un error. */
    EJECUTAR_CON_ERROR,
    /** Compila sin errores y el programa termina sin errores. */
    EJECUTAR_EXITOSO;

    /** Busca el objetivo por su nombre exacto (sensible a mayúsculas). */
    public static Optional<Objetivo> desdeNombre(String nombre) {
        return Arrays.stream(values()).filter(o -> o.name().equals(nombre)).findFirst();
    }
}
