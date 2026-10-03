package gt.edu.umg.lecciones.leccion;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Datos de prueba compartidos. */
public final class Fixtures {

    /** Bytes que empiezan con la firma PNG (suficiente para la validación de formato). */
    public static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};

    private Fixtures() {
    }

    public static byte[] utf8(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8);
    }

    /** Archivos de una lección mínima válida, en un mapa modificable. */
    public static Map<String, byte[]> leccionMinima(String objetivo) {
        Map<String, byte[]> archivos = new HashMap<>();
        archivos.put("leccion.md", utf8("# Título\n\nTexto con tilde: canción.\n"));
        archivos.put("config.txt", utf8("objetivo=" + objetivo + "\n"));
        return archivos;
    }
}
