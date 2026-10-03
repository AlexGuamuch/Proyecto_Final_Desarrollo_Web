package gt.edu.umg.lecciones.leccion.carga;

import gt.edu.umg.lecciones.leccion.model.Objetivo;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Interpreta config.txt. El archivo debe tener exactamente una línea {@code objetivo=VALOR}.
 * Se toleran detalles que suelen agregar los editores de texto: BOM de UTF-8, finales de línea
 * de Windows, espacios alrededor y líneas en blanco.
 */
public final class ConfigTxtParser {

    private static final Pattern LINEA = Pattern.compile("objetivo\\s*=\\s*(\\S+)");
    private static final String VALORES = Arrays.stream(Objetivo.values())
            .map(Enum::name)
            .collect(Collectors.joining(", "));

    private ConfigTxtParser() {
    }

    public static Objetivo parsear(String contenido) {
        if (contenido == null) {
            throw new LeccionInvalidaException("config.txt no existe");
        }
        String texto = contenido.startsWith("﻿") ? contenido.substring(1) : contenido;
        List<String> lineas = texto.lines().map(String::strip).filter(l -> !l.isEmpty()).toList();

        if (lineas.isEmpty()) {
            throw new LeccionInvalidaException("config.txt está vacío; debe contener objetivo=VALOR");
        }
        if (lineas.size() > 1) {
            throw new LeccionInvalidaException(
                    "config.txt debe tener exactamente una línea objetivo=VALOR, pero tiene " + lineas.size());
        }

        Matcher m = LINEA.matcher(lineas.getFirst());
        if (!m.matches()) {
            throw new LeccionInvalidaException(
                    "config.txt debe tener la forma objetivo=VALOR, pero dice: " + lineas.getFirst());
        }
        String valor = m.group(1);
        return Objetivo.desdeNombre(valor).orElseThrow(() -> new LeccionInvalidaException(
                "objetivo desconocido en config.txt: " + valor + " (valores válidos: " + VALORES + ")"));
    }
}
