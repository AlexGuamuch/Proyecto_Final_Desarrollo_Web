package gt.edu.umg.lecciones.leccion.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Lección tal como se guarda en el repositorio: el markdown original sin renderizar y sus
 * imágenes. El HTML se genera al consultarla, para que en la fase 2 la persistencia (JPA)
 * solo tenga que guardar estos datos.
 *
 * @param id       nombre de la carpeta; identifica la lección en la API
 * @param titulo   título que se muestra (por ahora, igual al nombre de la carpeta)
 * @param objetivo condición para completar la lección
 * @param markdown contenido de leccion.md
 * @param recursos imágenes de la lección, indexadas por nombre de archivo
 */
public record Leccion(String id, String titulo, Objetivo objetivo, String markdown, Map<String, Recurso> recursos) {

    public Leccion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(titulo, "titulo");
        Objects.requireNonNull(objetivo, "objetivo");
        Objects.requireNonNull(markdown, "markdown");
        recursos = Map.copyOf(recursos);
    }

    public Optional<Recurso> recurso(String nombre) {
        return Optional.ofNullable(recursos.get(nombre));
    }
}
