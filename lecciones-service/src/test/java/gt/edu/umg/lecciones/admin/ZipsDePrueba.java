package gt.edu.umg.lecciones.admin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static gt.edu.umg.lecciones.leccion.Fixtures.utf8;

/** Construye zips en memoria para las pruebas, con los nombres de entrada tal cual se indican. */
public final class ZipsDePrueba {

    private ZipsDePrueba() {
    }

    public static byte[] zip(Map<String, byte[]> entradas) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            for (Map.Entry<String, byte[]> entrada : entradas.entrySet()) {
                zip.putNextEntry(new ZipEntry(entrada.getKey()));
                zip.write(entrada.getValue());
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return salida.toByteArray();
    }

    /** Entradas de una lección válida dentro del prefijo indicado (por ejemplo "lecciones/" o ""). */
    public static Map<String, byte[]> leccion(String prefijo, String carpeta, String objetivo) {
        Map<String, byte[]> entradas = new LinkedHashMap<>();
        entradas.put(prefijo + carpeta + "/leccion.md", utf8("# " + carpeta + "\n\nTexto."));
        entradas.put(prefijo + carpeta + "/config.txt", utf8("objetivo=" + objetivo + "\n"));
        return entradas;
    }
}
