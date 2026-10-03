package gt.edu.umg.lecciones.leccion.model;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Formatos de imagen que una lección puede incluir. SVG queda fuera a propósito:
 * puede contener scripts y se serviría desde nuestro propio dominio.
 */
public enum TipoImagen {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg", "jpeg"),
    GIF("image/gif", "gif"),
    WEBP("image/webp", "webp");

    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private final String tipoContenido;
    private final String[] extensiones;

    TipoImagen(String tipoContenido, String... extensiones) {
        this.tipoContenido = tipoContenido;
        this.extensiones = extensiones;
    }

    public String tipoContenido() {
        return tipoContenido;
    }

    /** Determina el tipo a partir de la extensión del nombre de archivo (sin distinguir mayúsculas). */
    public static Optional<TipoImagen> desdeNombreArchivo(String nombre) {
        int punto = nombre.lastIndexOf('.');
        if (punto < 0 || punto == nombre.length() - 1) {
            return Optional.empty();
        }
        String extension = nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(t -> Arrays.asList(t.extensiones).contains(extension))
                .findFirst();
    }

    /** Comprueba que los primeros bytes correspondan de verdad a este formato. */
    public boolean coincideFirma(byte[] contenido) {
        return switch (this) {
            case PNG -> empiezaCon(contenido, 0, FIRMA_PNG);
            case JPEG -> empiezaCon(contenido, 0, FIRMA_JPEG);
            case GIF -> empiezaCon(contenido, 0, ascii("GIF87a")) || empiezaCon(contenido, 0, ascii("GIF89a"));
            case WEBP -> empiezaCon(contenido, 0, ascii("RIFF")) && empiezaCon(contenido, 8, ascii("WEBP"));
        };
    }

    private static byte[] ascii(String texto) {
        return texto.getBytes(StandardCharsets.US_ASCII);
    }

    private static boolean empiezaCon(byte[] contenido, int desde, byte[] firma) {
        if (contenido.length < desde + firma.length) {
            return false;
        }
        return Arrays.equals(contenido, desde, desde + firma.length, firma, 0, firma.length);
    }
}
