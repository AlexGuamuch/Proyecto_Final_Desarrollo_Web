package gt.edu.umg.lecciones.leccion.carga;

import gt.edu.umg.lecciones.leccion.model.Leccion;
import gt.edu.umg.lecciones.leccion.model.Nombres;
import gt.edu.umg.lecciones.leccion.model.Objetivo;
import gt.edu.umg.lecciones.leccion.model.Recurso;
import gt.edu.umg.lecciones.leccion.model.TipoImagen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Convierte el contenido de una carpeta de lección (nombre de archivo → bytes) en una
 * {@link Leccion} válida. Lo usan la carga desde el classpath y la importación de zip, para
 * que ambas apliquen exactamente las mismas reglas.
 */
@Component
public class ValidadorLeccion {

    public static final String ARCHIVO_MARKDOWN = "leccion.md";
    public static final String ARCHIVO_CONFIG = "config.txt";
    public static final int MAX_BYTES_MARKDOWN = 512 * 1024;
    public static final int MAX_BYTES_IMAGEN = 5 * 1024 * 1024;

    private static final Logger log = LoggerFactory.getLogger(ValidadorLeccion.class);

    public Leccion validar(String carpeta, Map<String, byte[]> archivos) {
        if (!Nombres.esIdValido(carpeta)) {
            throw new LeccionInvalidaException("el nombre de la carpeta solo puede tener letras sin tildes, "
                    + "dígitos, '.', '-' y '_', y debe empezar con letra o dígito");
        }

        byte[] markdownBytes = archivos.get(ARCHIVO_MARKDOWN);
        if (markdownBytes == null) {
            throw new LeccionInvalidaException("falta el archivo " + ARCHIVO_MARKDOWN);
        }
        if (markdownBytes.length > MAX_BYTES_MARKDOWN) {
            throw new LeccionInvalidaException(ARCHIVO_MARKDOWN + " supera " + MAX_BYTES_MARKDOWN / 1024 + " KB");
        }
        byte[] configBytes = archivos.get(ARCHIVO_CONFIG);
        if (configBytes == null) {
            throw new LeccionInvalidaException("falta el archivo " + ARCHIVO_CONFIG);
        }

        Objetivo objetivo = ConfigTxtParser.parsear(decodificarUtf8(configBytes, ARCHIVO_CONFIG));
        String markdown = decodificarUtf8(markdownBytes, ARCHIVO_MARKDOWN);
        if (markdown.startsWith("﻿")) {
            markdown = markdown.substring(1);
        }

        Map<String, Recurso> recursos = new LinkedHashMap<>();
        List<String> ignorados = new ArrayList<>();
        for (Map.Entry<String, byte[]> archivo : archivos.entrySet()) {
            String nombre = archivo.getKey();
            if (nombre.equals(ARCHIVO_MARKDOWN) || nombre.equals(ARCHIVO_CONFIG) || nombre.startsWith(".")) {
                continue;
            }
            Optional<TipoImagen> tipo = TipoImagen.desdeNombreArchivo(nombre);
            if (tipo.isEmpty() || !Nombres.esArchivoValido(nombre)) {
                ignorados.add(nombre);
                continue;
            }
            recursos.put(nombre, crearRecurso(nombre, tipo.get(), archivo.getValue()));
        }
        if (!ignorados.isEmpty()) {
            log.warn("Lección '{}': se ignoran archivos que no son imágenes permitidas "
                    + "(png, jpg, jpeg, gif, webp) o tienen un nombre no válido: {}", carpeta, ignorados);
        }

        return new Leccion(carpeta, carpeta, objetivo, markdown, recursos);
    }

    private static Recurso crearRecurso(String nombre, TipoImagen tipo, byte[] contenido) {
        if (contenido.length > MAX_BYTES_IMAGEN) {
            throw new LeccionInvalidaException("la imagen " + nombre + " supera " + MAX_BYTES_IMAGEN / (1024 * 1024) + " MB");
        }
        if (!tipo.coincideFirma(contenido)) {
            throw new LeccionInvalidaException("el archivo " + nombre + " no es una imagen " + tipo.name() + " válida");
        }
        return new Recurso(nombre, tipo, contenido);
    }

    private static String decodificarUtf8(byte[] bytes, String archivo) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new LeccionInvalidaException(archivo + " no está guardado en UTF-8");
        }
    }
}
