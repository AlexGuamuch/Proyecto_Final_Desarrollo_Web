package gt.edu.umg.lecciones.admin.service;

import gt.edu.umg.lecciones.comun.error.SolicitudInvalidaException;
import gt.edu.umg.lecciones.leccion.carga.ValidadorLeccion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Convierte un zip de lecciones en carpetas en memoria (y al revés).
 *
 * <p>Formato aceptado: {@code lecciones/<carpeta>/<archivo>} o {@code <carpeta>/<archivo>}.
 * Nada se escribe en disco. Aun así, el zip se rechaza completo si alguna entrada intenta salir
 * de su carpeta (zip slip: {@code ../}, rutas absolutas o con unidad), si tiene demasiadas
 * entradas o si descomprimido supera el límite (zip bomb). Se ignoran {@code __MACOSX/},
 * los archivos y carpetas ocultos y las subcarpetas.
 */
@Component
public class ZipLecciones {

    public static final int MAX_ENTRADAS = 500;
    public static final long MAX_BYTES_DESCOMPRIMIDOS = 50L * 1024 * 1024;
    public static final String CARPETA_RAIZ = "lecciones";

    private static final byte[] FIRMA_ZIP = {'P', 'K', 3, 4};
    private static final byte[] FIRMA_ZIP_VACIO = {'P', 'K', 5, 6};
    private static final Pattern UNIDAD_WINDOWS = Pattern.compile("^[A-Za-z]:.*");

    private final int maxEntradas;
    private final long maxBytesDescomprimidos;

    @Autowired
    public ZipLecciones() {
        this(MAX_ENTRADAS, MAX_BYTES_DESCOMPRIMIDOS);
    }

    ZipLecciones(int maxEntradas, long maxBytesDescomprimidos) {
        this.maxEntradas = maxEntradas;
        this.maxBytesDescomprimidos = maxBytesDescomprimidos;
    }

    /**
     * @param carpetas carpeta de lección → (nombre de archivo → bytes)
     * @param avisos   entradas ignoradas y por qué, para informar a quien importa
     */
    public record Contenido(Map<String, Map<String, byte[]>> carpetas, List<String> avisos) {
    }

    public Contenido leer(byte[] zip) {
        if (!empiezaCon(zip, FIRMA_ZIP) && !empiezaCon(zip, FIRMA_ZIP_VACIO)) {
            throw new SolicitudInvalidaException("El archivo no es un zip válido");
        }
        Map<String, Map<String, byte[]>> carpetas = new TreeMap<>();
        List<String> avisos = new ArrayList<>();
        int entradas = 0;
        long[] total = {0};

        try (ZipInputStream entrada = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry elemento;
            while ((elemento = entrada.getNextEntry()) != null) {
                if (++entradas > maxEntradas) {
                    throw new SolicitudInvalidaException("El zip tiene más de " + maxEntradas + " entradas");
                }
                String nombre = elemento.getName().replace('\\', '/');
                List<String> partes = segmentos(nombre);
                if (!partes.isEmpty() && partes.getFirst().equals(CARPETA_RAIZ) && partes.size() > 2) {
                    partes = partes.subList(1, partes.size());
                }
                boolean util = !elemento.isDirectory() && !ignorar(partes) && partes.size() == 2;
                if (!util && !elemento.isDirectory() && !ignorar(partes)) {
                    avisos.add(recortar(nombre) + ": se ignora, cada archivo debe estar dentro de la carpeta de su lección");
                }

                // Toda entrada se descomprime aquí, también las ignoradas, para que cuente contra el
                // límite: ZipInputStream descomprimiría el resto de la entrada al pasar a la siguiente.
                byte[] datos = leerAcotado(entrada, util, total, nombre);
                if (util) {
                    carpetas.computeIfAbsent(partes.get(0), c -> new TreeMap<>()).put(partes.get(1), datos);
                }
            }
        } catch (ZipException | IllegalArgumentException e) {
            throw new SolicitudInvalidaException("El zip está dañado o usa un formato no soportado");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (carpetas.isEmpty()) {
            throw new SolicitudInvalidaException(
                    "El zip no contiene lecciones: cada lección debe ser una carpeta con leccion.md y config.txt");
        }
        return new Contenido(carpetas, avisos);
    }

    /** Escribe las carpetas como {@code lecciones/<carpeta>/<archivo>}. */
    public byte[] escribir(Map<String, Map<String, byte[]>> carpetas) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            for (Map.Entry<String, Map<String, byte[]>> carpeta : carpetas.entrySet()) {
                for (Map.Entry<String, byte[]> archivo : carpeta.getValue().entrySet()) {
                    zip.putNextEntry(new ZipEntry(CARPETA_RAIZ + "/" + carpeta.getKey() + "/" + archivo.getKey()));
                    zip.write(archivo.getValue());
                    zip.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return salida.toByteArray();
    }

    /**
     * Descomprime la entrada actual sumando al total del zip. Si {@code conservar} es falso, los bytes
     * se descartan pero igual cuentan. Un archivo de más de 5 MB o un total de más del límite
     * detienen la lectura de inmediato.
     */
    private byte[] leerAcotado(ZipInputStream entrada, boolean conservar, long[] total, String nombre) throws IOException {
        ByteArrayOutputStream datos = new ByteArrayOutputStream();
        byte[] bloque = new byte[8192];
        int leidos;
        while ((leidos = entrada.read(bloque)) != -1) {
            total[0] += leidos;
            if (total[0] > maxBytesDescomprimidos) {
                throw new SolicitudInvalidaException("El contenido descomprimido supera "
                        + maxBytesDescomprimidos / (1024 * 1024) + " MB");
            }
            if (conservar) {
                if (datos.size() + leidos > ValidadorLeccion.MAX_BYTES_IMAGEN) {
                    throw new SolicitudInvalidaException("El archivo " + recortar(nombre) + " supera "
                            + ValidadorLeccion.MAX_BYTES_IMAGEN / (1024 * 1024) + " MB");
                }
                datos.write(bloque, 0, leidos);
            }
        }
        return datos.toByteArray();
    }

    /** Divide la ruta y rechaza el zip completo si intenta salir de su carpeta. */
    private static List<String> segmentos(String nombre) {
        if (nombre.startsWith("/") || UNIDAD_WINDOWS.matcher(nombre).matches()) {
            throw new SolicitudInvalidaException("El zip contiene rutas absolutas: " + recortar(nombre));
        }
        List<String> partes = Arrays.stream(nombre.split("/")).filter(p -> !p.isEmpty()).toList();
        if (partes.contains("..")) {
            throw new SolicitudInvalidaException("El zip contiene rutas que intentan salir de su carpeta: " + recortar(nombre));
        }
        return partes;
    }

    private static String recortar(String nombre) {
        return nombre.length() <= 120 ? nombre : nombre.substring(0, 117) + "...";
    }

    private static boolean ignorar(List<String> partes) {
        return partes.isEmpty()
                || partes.getFirst().equals("__MACOSX")
                || partes.stream().anyMatch(p -> p.startsWith("."));
    }

    private static boolean empiezaCon(byte[] datos, byte[] firma) {
        return datos.length >= firma.length && Arrays.equals(datos, 0, firma.length, firma, 0, firma.length);
    }
}
