package gt.edu.umg.lecciones.leccion.carga;

import gt.edu.umg.lecciones.leccion.model.Leccion;
import gt.edu.umg.lecciones.leccion.model.Objetivo;
import gt.edu.umg.lecciones.leccion.repository.InMemoryLeccionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class CargadorLeccionesTest {

    private final InMemoryLeccionRepository repositorio = new InMemoryLeccionRepository();

    @Test
    void cargaLasCuatroLeccionesDeEjemplo() {
        int cargadas = new CargadorLecciones(repositorio, new ValidadorLeccion(), "classpath*:lecciones").cargar();

        assertThat(cargadas).isEqualTo(4);
        assertThat(repositorio.findAll())
                .extracting(Leccion::id, Leccion::objetivo)
                .containsExactly(
                        tuple("leccion-01-hola-mundo", Objetivo.EJECUTAR_EXITOSO),
                        tuple("leccion-02-errores-compilacion", Objetivo.COMPILAR_CON_ERROR),
                        tuple("leccion-03-variables-y-tipos", Objetivo.COMPILAR_EXITOSO),
                        tuple("leccion-04-errores-ejecucion", Objetivo.EJECUTAR_CON_ERROR));
        assertThat(repositorio.findById("leccion-01-hola-mundo").orElseThrow().recurso("ciclo.png")).isPresent();
    }

    @Test
    void omiteLeccionesInvalidasSinLanzarExcepcion() {
        int cargadas = new CargadorLecciones(repositorio, new ValidadorLeccion(), "classpath*:lecciones-prueba").cargar();

        // Solo "valida-a" cumple el formato; sin-config, config-invalido, sin-markdown e imagen-falsa se omiten.
        assertThat(cargadas).isEqualTo(1);
        assertThat(repositorio.findAll()).extracting(Leccion::id).containsExactly("valida-a");
    }

    @Test
    void ubicacionVaciaNoCargaNada() {
        int cargadas = new CargadorLecciones(repositorio, new ValidadorLeccion(), "classpath*:no-existe").cargar();

        assertThat(cargadas).isZero();
        assertThat(repositorio.findAll()).isEmpty();
    }
}
