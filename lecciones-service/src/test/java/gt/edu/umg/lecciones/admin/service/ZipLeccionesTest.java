package gt.edu.umg.lecciones.admin.service;

import gt.edu.umg.lecciones.comun.error.SolicitudInvalidaException;
import gt.edu.umg.lecciones.leccion.carga.ValidadorLeccion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.Map;

import static gt.edu.umg.lecciones.admin.ZipsDePrueba.leccion;
import static gt.edu.umg.lecciones.admin.ZipsDePrueba.zip;
import static gt.edu.umg.lecciones.leccion.Fixtures.PNG;
import static gt.edu.umg.lecciones.leccion.Fixtures.utf8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZipLeccionesTest {

    private final ZipLecciones zipLecciones = new ZipLecciones();

    @Test
    void aceptaZipConCarpetaRaizLecciones() {
        Map<String, byte[]> entradas = leccion("lecciones/", "leccion-05", "COMPILAR_EXITOSO");
        entradas.put("lecciones/leccion-05/foto.png", PNG);

        ZipLecciones.Contenido contenido = zipLecciones.leer(zip(entradas));

        assertThat(contenido.carpetas()).containsOnlyKeys("leccion-05");
        assertThat(contenido.carpetas().get("leccion-05")).containsOnlyKeys("leccion.md", "config.txt", "foto.png");
        assertThat(contenido.avisos()).isEmpty();
    }

    @Test
    void aceptaZipSinCarpetaRaiz() {
        Map<String, byte[]> entradas = leccion("", "leccion-05", "COMPILAR_EXITOSO");
        entradas.putAll(leccion("", "leccion-06", "EJECUTAR_EXITOSO"));

        assertThat(zipLecciones.leer(zip(entradas)).carpetas()).containsOnlyKeys("leccion-05", "leccion-06");
    }

    @Test
    void aceptaBarrasInvertidasDeZipsCreadosEnWindows() {
        Map<String, byte[]> entradas = new LinkedHashMap<>();
        entradas.put("lecciones\\leccion-05\\leccion.md", utf8("# A"));
        entradas.put("lecciones\\leccion-05\\config.txt", utf8("objetivo=COMPILAR_EXITOSO"));

        assertThat(zipLecciones.leer(zip(entradas)).carpetas().get("leccion-05")).containsOnlyKeys("leccion.md", "config.txt");
    }

    @Test
    void ignoraMacosxYArchivosOcultos() {
        Map<String, byte[]> entradas = leccion("lecciones/", "leccion-05", "COMPILAR_EXITOSO");
        entradas.put("__MACOSX/lecciones/leccion-05/._leccion.md", new byte[]{0, 5, 22, 7});
        entradas.put("lecciones/leccion-05/.DS_Store", new byte[]{1, 2, 3});
        entradas.put("lecciones/.oculta/leccion.md", utf8("# oculta"));
        entradas.put(".git/config", utf8("[core]"));

        ZipLecciones.Contenido contenido = zipLecciones.leer(zip(entradas));

        assertThat(contenido.carpetas()).containsOnlyKeys("leccion-05");
        assertThat(contenido.carpetas().get("leccion-05")).containsOnlyKeys("leccion.md", "config.txt");
        assertThat(contenido.avisos()).isEmpty();
    }

    @Test
    void avisaDeArchivosSueltosYSubcarpetas() {
        Map<String, byte[]> entradas = leccion("", "leccion-05", "COMPILAR_EXITOSO");
        entradas.put("suelto.md", utf8("# fuera de carpeta"));
        entradas.put("leccion-05/img/foto.png", PNG);

        ZipLecciones.Contenido contenido = zipLecciones.leer(zip(entradas));

        assertThat(contenido.carpetas().get("leccion-05")).containsOnlyKeys("leccion.md", "config.txt");
        assertThat(contenido.avisos()).hasSize(2)
                .anySatisfy(a -> assertThat(a).startsWith("suelto.md"))
                .anySatisfy(a -> assertThat(a).startsWith("leccion-05/img/foto.png"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../leccion-05/leccion.md",
            "lecciones/../../fuera/leccion.md",
            "leccion-05/../../leccion.md",
            "..\\..\\Windows\\leccion.md",
            "/etc/leccion-05/leccion.md",
            "C:/Users/leccion-05/leccion.md",
            "C:\\Users\\leccion-05\\leccion.md",
    })
    void rechazaZipSlip(String rutaMaliciosa) {
        Map<String, byte[]> entradas = leccion("lecciones/", "leccion-05", "COMPILAR_EXITOSO");
        entradas.put(rutaMaliciosa, utf8("# malicioso"));

        assertThatThrownBy(() -> zipLecciones.leer(zip(entradas)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("rutas");
    }

    @Test
    void rechazaDemasiadasEntradas() {
        ZipLecciones conLimite = new ZipLecciones(3, ZipLecciones.MAX_BYTES_DESCOMPRIMIDOS);
        Map<String, byte[]> entradas = leccion("", "leccion-05", "COMPILAR_EXITOSO");
        entradas.putAll(leccion("", "leccion-06", "COMPILAR_EXITOSO"));

        assertThatThrownBy(() -> conLimite.leer(zip(entradas)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("más de 3 entradas");
    }

    @Test
    void rechazaZipBomb() {
        ZipLecciones conLimite = new ZipLecciones(ZipLecciones.MAX_ENTRADAS, 1000);
        Map<String, byte[]> entradas = leccion("", "leccion-05", "COMPILAR_EXITOSO");
        entradas.put("leccion-05/foto.png", new byte[2000]);

        assertThatThrownBy(() -> conLimite.leer(zip(entradas)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("descomprimido");
    }

    @Test
    void cuentaTambienLasEntradasIgnoradasContraElLimite() {
        ZipLecciones conLimite = new ZipLecciones(ZipLecciones.MAX_ENTRADAS, 1000);
        Map<String, byte[]> entradas = leccion("", "leccion-05", "COMPILAR_EXITOSO");
        entradas.put("__MACOSX/bomba", new byte[5000]);

        assertThatThrownBy(() -> conLimite.leer(zip(entradas)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("descomprimido");
    }

    @Test
    void rechazaArchivosDeMasDe5Mb() {
        Map<String, byte[]> entradas = leccion("", "leccion-05", "COMPILAR_EXITOSO");
        entradas.put("leccion-05/enorme.png", new byte[ValidadorLeccion.MAX_BYTES_IMAGEN + 1]);

        assertThatThrownBy(() -> zipLecciones.leer(zip(entradas)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("enorme.png");
    }

    @Test
    void rechazaArchivosQueNoSonZip() {
        assertThatThrownBy(() -> zipLecciones.leer(utf8("esto no es un zip")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("no es un zip");
    }

    @Test
    void rechazaZipSinLecciones() {
        Map<String, byte[]> entradas = new LinkedHashMap<>();
        entradas.put("__MACOSX/._algo", new byte[]{1});

        assertThatThrownBy(() -> zipLecciones.leer(zip(entradas)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("no contiene lecciones");
    }

    @Test
    void escribirYLeerSonSimetricos() {
        Map<String, Map<String, byte[]>> carpetas = new LinkedHashMap<>();
        carpetas.put("leccion-05", Map.of("leccion.md", utf8("# A"), "config.txt", utf8("objetivo=COMPILAR_EXITOSO\n"), "foto.png", PNG));

        byte[] exportado = zipLecciones.escribir(carpetas);
        ZipLecciones.Contenido leido = zipLecciones.leer(exportado);

        assertThat(leido.carpetas()).containsOnlyKeys("leccion-05");
        assertThat(leido.carpetas().get("leccion-05").get("foto.png")).isEqualTo(PNG);
        assertThat(leido.carpetas().get("leccion-05").get("config.txt")).isEqualTo(utf8("objetivo=COMPILAR_EXITOSO\n"));
    }
}
