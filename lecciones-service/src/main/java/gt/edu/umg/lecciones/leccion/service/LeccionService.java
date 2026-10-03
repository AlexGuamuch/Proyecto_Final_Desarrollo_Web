package gt.edu.umg.lecciones.leccion.service;

import gt.edu.umg.lecciones.comun.error.RecursoNoEncontradoException;
import gt.edu.umg.lecciones.comun.error.SolicitudInvalidaException;
import gt.edu.umg.lecciones.leccion.dto.LeccionDetalleDto;
import gt.edu.umg.lecciones.leccion.dto.LeccionResumenDto;
import gt.edu.umg.lecciones.leccion.model.Leccion;
import gt.edu.umg.lecciones.leccion.model.Nombres;
import gt.edu.umg.lecciones.leccion.model.Recurso;
import gt.edu.umg.lecciones.leccion.model.TipoImagen;
import gt.edu.umg.lecciones.leccion.repository.LeccionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Casos de uso de consulta de lecciones para el estudiante. */
@Service
public class LeccionService {

    private final LeccionRepository repositorio;
    private final MarkdownRenderer renderer;

    public LeccionService(LeccionRepository repositorio, MarkdownRenderer renderer) {
        this.repositorio = repositorio;
        this.renderer = renderer;
    }

    public List<LeccionResumenDto> listar() {
        return repositorio.findAll().stream()
                .map(l -> new LeccionResumenDto(l.id(), l.titulo(), l.objetivo()))
                .toList();
    }

    public LeccionDetalleDto obtener(String id) {
        Leccion leccion = buscar(id);
        String html = renderer.renderizar(leccion.id(), leccion.markdown());
        return new LeccionDetalleDto(leccion.id(), leccion.titulo(), leccion.objetivo(), html);
    }

    /**
     * Devuelve una imagen de la lección. El nombre se valida antes de buscarlo y la búsqueda
     * se hace en el mapa de recursos en memoria, nunca en el sistema de archivos, así que una
     * ruta como {@code ../../config.txt} no puede salir de la lección.
     */
    public Recurso obtenerRecurso(String id, String archivo) {
        if (!Nombres.esArchivoValido(archivo)) {
            throw new SolicitudInvalidaException("Nombre de archivo no válido");
        }
        if (TipoImagen.desdeNombreArchivo(archivo).isEmpty()) {
            throw new SolicitudInvalidaException("Solo se sirven imágenes png, jpg, jpeg, gif o webp");
        }
        return buscar(id).recurso(archivo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La lección " + id + " no tiene el recurso " + archivo));
    }

    private Leccion buscar(String id) {
        if (!Nombres.esIdValido(id)) {
            throw new RecursoNoEncontradoException("No existe la lección solicitada");
        }
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la lección " + id));
    }
}
