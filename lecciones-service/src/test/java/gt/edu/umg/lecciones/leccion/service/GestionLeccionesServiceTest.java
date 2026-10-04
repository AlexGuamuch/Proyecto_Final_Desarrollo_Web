package gt.edu.umg.lecciones.leccion.service;

import gt.edu.umg.lecciones.comun.error.RecursoNoEncontradoException;
import gt.edu.umg.lecciones.leccion.carga.LeccionInvalidaException;
import gt.edu.umg.lecciones.leccion.carga.ValidadorLeccion;
import gt.edu.umg.lecciones.leccion.model.Objetivo;
import gt.edu.umg.lecciones.leccion.repository.InMemoryLeccionRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static gt.edu.umg.lecciones.leccion.Fixtures.PNG;
import static gt.edu.umg.lecciones.leccion.Fixtures.leccionMinima;
import static gt.edu.umg.lecciones.leccion.Fixtures.utf8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestionLeccionesServiceTest {

    private final InMemoryLeccionRepository repositorio = new InMemoryLeccionRepository();
    private final ValidadorLeccion validador = new ValidadorLeccion();

    @Test
    void registraYReemplazaPorId() {
        GestionLeccionesService gestion = new GestionLeccionesService(repositorio, validador);

        GestionLeccionesService.Registro primera = gestion.registrar("leccion-05", leccionMinima("COMPILAR_EXITOSO"));
        GestionLeccionesService.Registro segunda = gestion.registrar("leccion-05", leccionMinima("EJECUTAR_EXITOSO"));

        assertThat(primera.reemplazada()).isFalse();
        assertThat(segunda.reemplazada()).isTrue();
        assertThat(repositorio.findById("leccion-05").orElseThrow().objetivo()).isEqualTo(Objetivo.EJECUTAR_EXITOSO);
    }

    @Test
    void propagaLasReglasDelValidador() {
        GestionLeccionesService gestion = new GestionLeccionesService(repositorio, validador);
        Map<String, byte[]> sinConfig = leccionMinima("COMPILAR_EXITOSO");
        sinConfig.remove("config.txt");

        assertThatThrownBy(() -> gestion.registrar("leccion-05", sinConfig))
                .isInstanceOf(LeccionInvalidaException.class)
                .hasMessageContaining("config.txt");
        assertThat(repositorio.findAll()).isEmpty();
    }

    @Test
    void respetaElMaximoDeLecciones() {
        GestionLeccionesService gestion = new GestionLeccionesService(repositorio, validador, 2, Long.MAX_VALUE);
        gestion.registrar("a", leccionMinima("COMPILAR_EXITOSO"));
        gestion.registrar("b", leccionMinima("COMPILAR_EXITOSO"));

        assertThatThrownBy(() -> gestion.registrar("c", leccionMinima("COMPILAR_EXITOSO")))
                .isInstanceOf(LeccionInvalidaException.class)
                .hasMessageContaining("máximo de 2 lecciones");
        // Reemplazar una existente sí se permite: no aumenta la cantidad.
        assertThat(gestion.registrar("a", leccionMinima("EJECUTAR_EXITOSO")).reemplazada()).isTrue();
    }

    @Test
    void respetaElMaximoDeBytesEnMemoria() {
        GestionLeccionesService gestion = new GestionLeccionesService(repositorio, validador, 100, 1000);
        Map<String, byte[]> conImagen = leccionMinima("COMPILAR_EXITOSO");
        byte[] imagenGrande = new byte[2000];
        System.arraycopy(PNG, 0, imagenGrande, 0, PNG.length);
        conImagen.put("grande.png", imagenGrande);

        assertThatThrownBy(() -> gestion.registrar("pesada", conImagen))
                .isInstanceOf(LeccionInvalidaException.class)
                .hasMessageContaining("no hay espacio");
    }

    @Test
    void eliminaYAvisaSiNoExiste() {
        GestionLeccionesService gestion = new GestionLeccionesService(repositorio, validador);
        gestion.registrar("leccion-05", leccionMinima("COMPILAR_EXITOSO"));

        gestion.eliminar("leccion-05");

        assertThat(repositorio.existsById("leccion-05")).isFalse();
        assertThatThrownBy(() -> gestion.eliminar("leccion-05")).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> gestion.eliminar("../x")).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void exportaEnFormatoDeCarpeta() {
        GestionLeccionesService gestion = new GestionLeccionesService(repositorio, validador);
        Map<String, byte[]> archivos = leccionMinima("EJECUTAR_CON_ERROR");
        archivos.put("foto.png", PNG);
        gestion.registrar("leccion-05", archivos);

        Map<String, Map<String, byte[]>> exportado = gestion.exportar();

        assertThat(exportado).containsOnlyKeys("leccion-05");
        assertThat(exportado.get("leccion-05")).containsOnlyKeys("leccion.md", "config.txt", "foto.png");
        assertThat(exportado.get("leccion-05").get("config.txt")).isEqualTo(utf8("objetivo=EJECUTAR_CON_ERROR\n"));
    }
}
