package gt.edu.umg.lecciones.leccion.model;

import java.util.regex.Pattern;

/**
 * Reglas de nombres para ids de lección y archivos de recursos. Solo se aceptan letras ASCII,
 * dígitos, punto, guion y guion bajo, empezando con letra o dígito. Con eso un nombre nunca
 * puede contener separadores de ruta ni {@code ..}, que es la base contra el path traversal.
 */
public final class Nombres {

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,99}");
    private static final Pattern ARCHIVO = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");

    private Nombres() {
    }

    public static boolean esIdValido(String id) {
        return id != null && ID.matcher(id).matches() && !id.contains("..");
    }

    public static boolean esArchivoValido(String nombre) {
        return nombre != null && ARCHIVO.matcher(nombre).matches() && !nombre.contains("..");
    }
}
