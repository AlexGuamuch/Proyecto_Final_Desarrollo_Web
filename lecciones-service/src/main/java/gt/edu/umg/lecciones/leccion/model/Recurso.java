package gt.edu.umg.lecciones.leccion.model;

import java.util.Objects;

/**
 * Imagen que acompaña a una lección y se muestra dentro de su markdown.
 *
 * @param nombre    nombre del archivo dentro de la carpeta de la lección (ej. {@code ciclo.png})
 * @param tipo      formato de la imagen
 * @param contenido bytes del archivo
 */
public record Recurso(String nombre, TipoImagen tipo, byte[] contenido) {

    public Recurso {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(contenido, "contenido");
    }
}
