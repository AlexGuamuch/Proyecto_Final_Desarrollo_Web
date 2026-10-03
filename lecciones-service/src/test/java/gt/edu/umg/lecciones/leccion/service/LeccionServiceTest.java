package gt.edu.umg.lecciones.leccion.service;

import gt.edu.umg.lecciones.comun.error.RecursoNoEncontradoException;
import gt.edu.umg.lecciones.comun.error.SolicitudInvalidaException;
import gt.edu.umg.lecciones.leccion.carga.ValidadorLeccion;
import gt.edu.umg.lecciones.leccion.dto.LeccionDetalleDto;
import gt.edu.umg.lecciones.leccion.model.Objetivo;
import gt.edu.umg.lecciones.leccion.repository.InMemoryLeccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static gt.edu.umg.lecciones.leccion.Fixtures.PNG;
import static gt.edu.umg.lecciones.leccion.Fixtures.leccionMinima;
import static gt.edu.umg.lecciones.leccion.Fixtures.utf8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeccionServiceTest {

    private final InMemoryLeccionRepository repositorio = new InMemoryLeccionRepository();
    private final LeccionService servicio = new LeccionService(repositorio, new MarkdownRenderer());

    @BeforeEach
    void preparar() {
        Map<String, byte[]> archivos = leccionMinima("EJECUTAR_EXITOSO");
        archivos.put("leccion.md", utf8("# Hola\n\n![Ciclo](ciclo.png)"));
        archivos.put("ciclo.png", PNG);
        repositorio.save(new ValidadorLeccion().validar("leccion-01", archivos));
    }

    @Test
    void obtieneLaLeccionConHtmlRenderizado() {
        LeccionDetalleDto detalle = servicio.obtener("leccion-01");

        assertThat(detalle.objetivo()).isEqualTo(Objetivo.EJECUTAR_EXITOSO);
        assertThat(detalle.html()).contains("<h1>Hola</h1>")
                .contains("/api/lecciones/leccion-01/recursos/ciclo.png");
    }

    @Test
    void obtieneUnaImagenExistente() {
        assertThat(servicio.obtenerRecurso("leccion-01", "ciclo.png").contenido()).isEqualTo(PNG);
    }

    @ParameterizedTest
    @ValueSource(strings = {"../config.txt", "..", "..%2Fconfig.txt", "a/b.png", "a\\b.png", "/etc/passwd", ".ciclo.png", "C:ciclo.png"})
    void rechazaRutasQueIntentanSalirDeLaLeccion(String archivo) {
        assertThatThrownBy(() -> servicio.obtenerRecurso("leccion-01", archivo))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"config.txt", "leccion.md", "dibujo.svg", "ciclo"})
    void soloSirveExtensionesDeImagenPermitidas(String archivo) {
        assertThatThrownBy(() -> servicio.obtenerRecurso("leccion-01", archivo))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void imagenInexistenteEs404() {
        assertThatThrownBy(() -> servicio.obtenerRecurso("leccion-01", "otra.png"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-existe", "../leccion-01", "leccion 01"})
    void leccionInexistenteOConIdNoValidoEs404(String id) {
        assertThatThrownBy(() -> servicio.obtener(id)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.obtenerRecurso(id, "ciclo.png")).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
