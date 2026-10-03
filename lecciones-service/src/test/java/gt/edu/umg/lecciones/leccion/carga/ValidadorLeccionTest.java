package gt.edu.umg.lecciones.leccion.carga;

import gt.edu.umg.lecciones.leccion.model.Leccion;
import gt.edu.umg.lecciones.leccion.model.Objetivo;
import gt.edu.umg.lecciones.leccion.model.TipoImagen;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static gt.edu.umg.lecciones.leccion.Fixtures.PNG;
import static gt.edu.umg.lecciones.leccion.Fixtures.leccionMinima;
import static gt.edu.umg.lecciones.leccion.Fixtures.utf8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorLeccionTest {

    private final ValidadorLeccion validador = new ValidadorLeccion();

    @Test
    void creaLeccionConIdTituloObjetivoYMarkdown() {
        Leccion leccion = validador.validar("leccion-01-hola-mundo", leccionMinima("EJECUTAR_EXITOSO"));

        assertThat(leccion.id()).isEqualTo("leccion-01-hola-mundo");
        assertThat(leccion.titulo()).isEqualTo("leccion-01-hola-mundo");
        assertThat(leccion.objetivo()).isEqualTo(Objetivo.EJECUTAR_EXITOSO);
        assertThat(leccion.markdown()).contains("canción");
        assertThat(leccion.recursos()).isEmpty();
    }

    @Test
    void incluyeImagenesValidas() {
        Map<String, byte[]> archivos = leccionMinima("COMPILAR_EXITOSO");
        archivos.put("ciclo.png", PNG);

        Leccion leccion = validador.validar("leccion", archivos);

        assertThat(leccion.recurso("ciclo.png")).hasValueSatisfying(r -> {
            assertThat(r.tipo()).isEqualTo(TipoImagen.PNG);
            assertThat(r.contenido()).isEqualTo(PNG);
        });
    }

    @Test
    void quitaElBomDelMarkdown() {
        Map<String, byte[]> archivos = leccionMinima("COMPILAR_EXITOSO");
        archivos.put("leccion.md", utf8("﻿# Título"));

        assertThat(validador.validar("leccion", archivos).markdown()).isEqualTo("# Título");
    }

    @Test
    void rechazaSiFaltaElMarkdown() {
        Map<String, byte[]> archivos = leccionMinima("COMPILAR_EXITOSO");
        archivos.remove("leccion.md");

        assertThatThrownBy(() -> validador.validar("leccion", archivos))
                .isInstanceOf(LeccionInvalidaException.class)
                .hasMessageContaining("leccion.md");
    }

    @Test
    void rechazaSiFaltaElConfig() {
        Map<String, byte[]> archivos = leccionMinima("COMPILAR_EXITOSO");
        archivos.remove("config.txt");

        assertThatThrownBy(() -> validador.validar("leccion", archivos))
                .isInstanceOf(LeccionInvalidaException.class)
                .hasMessageContaining("config.txt");
    }

    @ParameterizedTest
    @ValueSource(strings = {"..", "../leccion", "con espacio", "lección", ".oculta", "-guion", "a/b", "a\\b", ""})
    void rechazaNombresDeCarpetaNoValidos(String carpeta) {
        assertThatThrownBy(() -> validador.validar(carpeta, leccionMinima("COMPILAR_EXITOSO")))
                .isInstanceOf(LeccionInvalidaException.class);
    }

    @Test
    void rechazaMarkdownDemasiadoGrande() {
        Map<String, byte[]> archivos = leccionMinima("COMPILAR_EXITOSO");
        archivos.put("leccion.md", new byte[ValidadorLeccion.MAX_BYTES_MARKDOWN + 1]);

        assertThatThrownBy(() -> validador.validar("leccion", archivos))
                .isInstanceOf(LeccionInvalidaException.class)
                .hasMessageContaining("KB");
    }

    @Test
    void rechazaMarkdownQueNoEsUtf8() {
        Map<String, byte[]> archivos = leccionMinima("COMPILAR_EXITOSO");
        archivos.put("leccion.md", new byte[]{'#', ' ', (byte) 0xE1, 'r', 'b', 'o', 'l'}); // "á" en ISO-8859-1

        assertThatThrownBy(() -> validador.validar("leccion", archivos))
                .isInstanceOf(LeccionInvalidaException.class)
                .hasMessageContaining("UTF-8");
    }

    @Test
    void rechazaImagenConFirmaFalsa() {
        Map<String, byte[]> archivos = leccionMinima("COMPILAR_EXITOSO");
        archivos.put("foto.png", utf8("<script>alert(1)</script>"));

        assertThatThrownBy(() -> validador.validar("leccion", archivos))
                .isInstanceOf(LeccionInvalidaException.class)
                .hasMessageContaining("foto.png");
    }

    @Test
    void ignoraSvgOcultosYNombresNoValidos() {
        Map<String, byte[]> archivos = leccionMinima("COMPILAR_EXITOSO");
        archivos.put("dibujo.svg", utf8("<svg onload=alert(1)>"));
        archivos.put(".DS_Store", new byte[]{1, 2, 3});
        archivos.put("notas.txt", utf8("borrador"));
        archivos.put("mi foto.png", PNG);

        Leccion leccion = validador.validar("leccion", archivos);

        assertThat(leccion.recursos()).isEmpty();
    }

    @Test
    void reconoceLosCuatroFormatosPorExtensionYFirma() {
        byte[] gif = utf8("GIF89a....");
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        byte[] webp = utf8("RIFF\0\0\0\0WEBPVP8 ");

        assertThat(TipoImagen.desdeNombreArchivo("a.PNG")).contains(TipoImagen.PNG);
        assertThat(TipoImagen.desdeNombreArchivo("a.jpeg")).contains(TipoImagen.JPEG);
        assertThat(TipoImagen.desdeNombreArchivo("a.svg")).isEmpty();
        assertThat(TipoImagen.desdeNombreArchivo("sin-extension")).isEmpty();
        assertThat(TipoImagen.GIF.coincideFirma(gif)).isTrue();
        assertThat(TipoImagen.JPEG.coincideFirma(jpeg)).isTrue();
        assertThat(TipoImagen.WEBP.coincideFirma(webp)).isTrue();
        assertThat(TipoImagen.PNG.coincideFirma(jpeg)).isFalse();
        assertThat(TipoImagen.WEBP.coincideFirma(utf8("RIFF"))).isFalse();
    }
}
