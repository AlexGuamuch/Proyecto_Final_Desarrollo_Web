package gt.edu.umg.lecciones.leccion.repository;

import gt.edu.umg.lecciones.leccion.model.Leccion;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Repositorio en memoria (fase 1). Los datos se pierden al reiniciar el servicio y no se
 * comparten entre instancias; ambas limitaciones desaparecen con la base de datos de la fase 2.
 */
@Repository
public class InMemoryLeccionRepository implements LeccionRepository {

    private final ConcurrentMap<String, Leccion> lecciones = new ConcurrentHashMap<>();

    @Override
    public List<Leccion> findAll() {
        return lecciones.values().stream()
                .sorted(Comparator.comparing(Leccion::id))
                .toList();
    }

    @Override
    public Optional<Leccion> findById(String id) {
        return Optional.ofNullable(lecciones.get(id));
    }

    @Override
    public Leccion save(Leccion leccion) {
        lecciones.put(leccion.id(), leccion);
        return leccion;
    }

    @Override
    public boolean deleteById(String id) {
        return lecciones.remove(id) != null;
    }

    @Override
    public boolean existsById(String id) {
        return lecciones.containsKey(id);
    }
}
