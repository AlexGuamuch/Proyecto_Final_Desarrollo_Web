package gt.edu.umg.lecciones.leccion.carga;

import gt.edu.umg.lecciones.leccion.model.Leccion;
import gt.edu.umg.lecciones.leccion.repository.LeccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * Al arrancar, lee las lecciones de {@code app.lecciones.ubicacion} (por defecto la carpeta
 * {@code lecciones/} del classpath). Cada subcarpeta es una lección; las inválidas se registran
 * como advertencia y se omiten sin detener la aplicación.
 */
@Component
public class CargadorLecciones implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargadorLecciones.class);

    private final LeccionRepository repositorio;
    private final ValidadorLeccion validador;
    private final ResourcePatternResolver resolver;
    private final String ubicacion;

    public CargadorLecciones(LeccionRepository repositorio,
                             ValidadorLeccion validador,
                             @Value("${app.lecciones.ubicacion}") String ubicacion) {
        this.repositorio = repositorio;
        this.validador = validador;
        this.resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
        this.ubicacion = ubicacion;
    }

    @Override
    public void run(ApplicationArguments args) {
        cargar();
    }

    /** Carga todas las lecciones válidas y devuelve cuántas quedaron en el repositorio. */
    public int cargar() {
        Map<String, Map<String, byte[]>> carpetas = leerCarpetas();
        int cargadas = 0;
        for (Map.Entry<String, Map<String, byte[]>> carpeta : carpetas.entrySet()) {
            try {
                Leccion leccion = validador.validar(carpeta.getKey(), carpeta.getValue());
                repositorio.save(leccion);
                cargadas++;
            } catch (LeccionInvalidaException e) {
                log.warn("Lección omitida '{}': {}", carpeta.getKey(), e.getMessage());
            }
        }
        log.info("Lecciones cargadas: {} de {} carpetas encontradas en {}", cargadas, carpetas.size(), ubicacion);
        return cargadas;
    }

    /** Agrupa los archivos de primer nivel de cada carpeta de lección: carpeta → (archivo → bytes). */
    private Map<String, Map<String, byte[]>> leerCarpetas() {
        Map<String, Map<String, byte[]>> carpetas = new TreeMap<>();
        try {
            for (Resource recurso : resolver.getResources(ubicacion + "/*/*")) {
                String url = recurso.getURL().toString();
                // Las subcarpetas no forman parte del formato de lección.
                if (url.endsWith("/") || !recurso.isReadable()) {
                    continue;
                }
                String[] partes = url.split("/");
                String archivo = UriUtils.decode(partes[partes.length - 1], StandardCharsets.UTF_8);
                String carpeta = UriUtils.decode(partes[partes.length - 2], StandardCharsets.UTF_8);
                carpetas.computeIfAbsent(carpeta, c -> new TreeMap<>()).put(archivo, leer(recurso));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudieron leer las lecciones de " + ubicacion, e);
        }
        return carpetas;
    }

    private static byte[] leer(Resource recurso) throws IOException {
        try (InputStream entrada = recurso.getInputStream()) {
            // Se lee un byte más del máximo para que el validador detecte archivos demasiado grandes.
            return entrada.readNBytes(ValidadorLeccion.MAX_BYTES_IMAGEN + 1);
        }
    }
}
