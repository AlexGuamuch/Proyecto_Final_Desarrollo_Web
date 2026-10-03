package gt.edu.umg.lecciones.leccion.repository;

import gt.edu.umg.lecciones.leccion.model.Leccion;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a las lecciones. En la fase 1 la implementación vive en memoria; en la fase 2 se
 * reemplaza por una basada en JPA/MariaDB sin tocar controllers ni services.
 */
public interface LeccionRepository {

    /** Todas las lecciones, ordenadas por id. */
    List<Leccion> findAll();

    Optional<Leccion> findById(String id);

    /** Guarda la lección; si ya existe una con el mismo id, la reemplaza. */
    Leccion save(Leccion leccion);

    /** Elimina la lección. Devuelve {@code false} si no existía. */
    boolean deleteById(String id);

    boolean existsById(String id);
}
