package gt.edu.umg.lecciones.leccion.carga;

import gt.edu.umg.lecciones.leccion.model.Objetivo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigTxtParserTest {

    @ParameterizedTest
    @EnumSource(Objetivo.class)
    void aceptaLosCuatroObjetivos(Objetivo objetivo) {
        assertThat(ConfigTxtParser.parsear("objetivo=" + objetivo.name())).isEqualTo(objetivo);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "﻿objetivo=EJECUTAR_EXITOSO",          // BOM que agrega el Bloc de notas
            "objetivo=EJECUTAR_EXITOSO\r\n",            // fin de línea de Windows
            "  objetivo = EJECUTAR_EXITOSO  ",          // espacios alrededor
            "\n\nobjetivo=EJECUTAR_EXITOSO\n\n\n",      // líneas en blanco extra
    })
    void toleraDetallesDeFormato(String contenido) {
        assertThat(ConfigTxtParser.parsear(contenido)).isEqualTo(Objetivo.EJECUTAR_EXITOSO);
    }

    @Test
    void rechazaArchivoVacio() {
        assertThatThrownBy(() -> ConfigTxtParser.parsear("  \n \r\n"))
                .isInstanceOf(LeccionInvalidaException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void rechazaArchivoInexistente() {
        assertThatThrownBy(() -> ConfigTxtParser.parsear(null))
                .isInstanceOf(LeccionInvalidaException.class);
    }

    @Test
    void rechazaMasDeUnaLinea() {
        assertThatThrownBy(() -> ConfigTxtParser.parsear("objetivo=COMPILAR_EXITOSO\nobjetivo=EJECUTAR_EXITOSO"))
                .isInstanceOf(LeccionInvalidaException.class)
                .hasMessageContaining("exactamente una línea");
    }

    @ParameterizedTest
    @ValueSource(strings = {"objetivo=OTRO", "objetivo=compilar_exitoso", "objetivo="})
    void rechazaValoresDesconocidos(String contenido) {
        assertThatThrownBy(() -> ConfigTxtParser.parsear(contenido))
                .isInstanceOf(LeccionInvalidaException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"meta=EJECUTAR_EXITOSO", "EJECUTAR_EXITOSO", "objetivo=EJECUTAR_EXITOSO extra", "objetivo:EJECUTAR_EXITOSO"})
    void rechazaLineasConOtraForma(String contenido) {
        assertThatThrownBy(() -> ConfigTxtParser.parsear(contenido))
                .isInstanceOf(LeccionInvalidaException.class);
    }

    @Test
    void elMensajeListaLosValoresValidos() {
        assertThatThrownBy(() -> ConfigTxtParser.parsear("objetivo=TERMINAR"))
                .hasMessageContaining("TERMINAR")
                .hasMessageContaining("COMPILAR_CON_ERROR")
                .hasMessageContaining("EJECUTAR_EXITOSO");
    }
}
