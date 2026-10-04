package gt.edu.umg.lecciones.leccion.service;

import gt.edu.umg.lecciones.comun.error.RecursoNoEncontradoException;
import gt.edu.umg.lecciones.leccion.carga.LeccionInvalidaException;
import gt.edu.umg.lecciones.leccion.carga.ValidadorLeccion;
import gt.edu.umg.lecciones.leccion.model.Leccion;
import gt.edu.umg.lecciones.leccion.model.Nombres;
import gt.edu.umg.lecciones.leccion.model.Objetivo;
import gt.edu.umg.lecciones.leccion.model.Recurso;
import gt.edu.umg.lecciones.leccion.repository.LeccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Casos de uso que modifican el catálogo de lecciones (importar, eliminar, exportar).
 * Otros módulos, como la administración, los usan en lugar de acceder al repositorio.
 *
 * <p>Las lecciones se intercambian en el mismo formato que una carpeta de lección:
 * nombre de carpeta → (nombre de archivo → bytes). Así importar y exportar son simétricos.
 *
 * <p>Como el catálogo vive en memoria y en la fase 1 estas operaciones no requieren
 * autenticación, hay un tope de lecciones y de bytes para que nadie pueda agotar la memoria.
 */
@Service
public class GestionLeccionesService {

    public static final int MAX_LECCIONES = 100;
    public static final long MAX_BYTES_EN_MEMORIA = 64L * 1024 * 1024;

    private final LeccionRepository repositorio;
    private final ValidadorLeccion validador;
    private final int maxLecciones;
    private final long maxBytes;

    @Autowired
    public GestionLeccionesService(LeccionRepository repositorio, ValidadorLeccion validador) {
        this(repositorio, validador, MAX_LECCIONES, MAX_BYTES_EN_MEMORIA);
    }

    GestionLeccionesService(LeccionRepository repositorio, ValidadorLeccion validador, int maxLecciones, long maxBytes) {
        this.repositorio = repositorio;
        this.validador = validador;
        this.maxLecciones = maxLecciones;
        this.maxBytes = maxBytes;
    }

    /** Resultado de registrar una lección. {@code reemplazada} indica que ya existía una con el mismo id. */
    public record Registro(String id, Objetivo objetivo, boolean reemplazada) {
    }

    /**
     * Valida la carpeta y guarda la lección, reemplazando la anterior con el mismo id.
     *
     * @throws LeccionInvalidaException si no cumple el formato o no cabe en memoria
     */
    public synchronized Registro registrar(String carpeta, Map<String, byte[]> archivos) {
        Leccion leccion = validador.validar(carpeta, archivos);
        Optional<Leccion> anterior = repositorio.findById(leccion.id());

        int cantidad = repositorio.findAll().size() + (anterior.isPresent() ? 0 : 1);
        if (cantidad > maxLecciones) {
            throw new LeccionInvalidaException("se alcanzó el máximo de " + maxLecciones + " lecciones en memoria");
        }
        long bytes = bytesEnMemoria() - anterior.map(GestionLeccionesService::tamano).orElse(0L) + tamano(leccion);
        if (bytes > maxBytes) {
            throw new LeccionInvalidaException("no hay espacio: el máximo en memoria es "
                    + maxBytes / (1024 * 1024) + " MB para todas las lecciones");
        }

        repositorio.save(leccion);
        return new Registro(leccion.id(), leccion.objetivo(), anterior.isPresent());
    }

    public synchronized void eliminar(String id) {
        if (!Nombres.esIdValido(id) || !repositorio.deleteById(id)) {
            throw new RecursoNoEncontradoException("No existe la lección " + id);
        }
    }

    /** Todas las lecciones en formato de carpeta: leccion.md, config.txt e imágenes. */
    public Map<String, Map<String, byte[]>> exportar() {
        Map<String, Map<String, byte[]>> carpetas = new LinkedHashMap<>();
        for (Leccion leccion : repositorio.findAll()) {
            Map<String, byte[]> archivos = new LinkedHashMap<>();
            archivos.put(ValidadorLeccion.ARCHIVO_MARKDOWN, leccion.markdown().getBytes(StandardCharsets.UTF_8));
            archivos.put(ValidadorLeccion.ARCHIVO_CONFIG,
                    ("objetivo=" + leccion.objetivo().name() + "\n").getBytes(StandardCharsets.UTF_8));
            for (Recurso recurso : leccion.recursos().values()) {
                archivos.put(recurso.nombre(), recurso.contenido());
            }
            carpetas.put(leccion.id(), archivos);
        }
        return carpetas;
    }

    private long bytesEnMemoria() {
        return repositorio.findAll().stream().mapToLong(GestionLeccionesService::tamano).sum();
    }

    private static long tamano(Leccion leccion) {
        long imagenes = leccion.recursos().values().stream().mapToLong(r -> r.contenido().length).sum();
        // Aproximación: un String Java ocupa ~2 bytes por carácter.
        return imagenes + 2L * leccion.markdown().length();
    }
}
