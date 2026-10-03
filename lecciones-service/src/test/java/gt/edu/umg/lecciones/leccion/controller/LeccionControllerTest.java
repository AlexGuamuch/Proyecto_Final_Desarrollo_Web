package gt.edu.umg.lecciones.leccion.controller;

import gt.edu.umg.lecciones.leccion.carga.ValidadorLeccion;
import gt.edu.umg.lecciones.leccion.repository.InMemoryLeccionRepository;
import gt.edu.umg.lecciones.leccion.repository.LeccionRepository;
import gt.edu.umg.lecciones.leccion.service.LeccionService;
import gt.edu.umg.lecciones.leccion.service.MarkdownRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static gt.edu.umg.lecciones.leccion.Fixtures.PNG;
import static gt.edu.umg.lecciones.leccion.Fixtures.leccionMinima;
import static gt.edu.umg.lecciones.leccion.Fixtures.utf8;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la capa HTTP con el servicio y el repositorio reales (en memoria): el controller,
 * el manejo centralizado de errores y el filtro de cabeceras de seguridad.
 */
@WebMvcTest(LeccionController.class)
@Import({LeccionService.class, MarkdownRenderer.class, InMemoryLeccionRepository.class})
class LeccionControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LeccionRepository repositorio;

    @BeforeEach
    void preparar() {
        ValidadorLeccion validador = new ValidadorLeccion();
        Map<String, byte[]> primera = leccionMinima("EJECUTAR_EXITOSO");
        primera.put("leccion.md", utf8("# Hola mundo\n\n![Ciclo](ciclo.png)\n\n<script>alert(1)</script>"));
        primera.put("ciclo.png", PNG);
        repositorio.save(validador.validar("leccion-01-hola-mundo", primera));
        repositorio.save(validador.validar("leccion-02-errores-compilacion", leccionMinima("COMPILAR_CON_ERROR")));
    }

    @Test
    void listaLasLeccionesConIdTituloYObjetivo() throws Exception {
        mvc.perform(get("/api/lecciones"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("leccion-01-hola-mundo"))
                .andExpect(jsonPath("$[0].titulo").value("leccion-01-hola-mundo"))
                .andExpect(jsonPath("$[0].objetivo").value("EJECUTAR_EXITOSO"))
                .andExpect(jsonPath("$[0].html").doesNotExist())
                .andExpect(jsonPath("$[1].objetivo").value("COMPILAR_CON_ERROR"));
    }

    @Test
    void devuelveElDetalleConHtmlSeguro() throws Exception {
        mvc.perform(get("/api/lecciones/leccion-01-hola-mundo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("leccion-01-hola-mundo"))
                .andExpect(jsonPath("$.objetivo").value("EJECUTAR_EXITOSO"))
                .andExpect(jsonPath("$.html", containsString("<h1>Hola mundo</h1>")))
                .andExpect(jsonPath("$.html", containsString("src=\"/api/lecciones/leccion-01-hola-mundo/recursos/ciclo.png\"")))
                .andExpect(jsonPath("$.html", not(containsString("<script>"))));
    }

    @Test
    void leccionInexistenteDevuelve404ConErrorJson() throws Exception {
        mvc.perform(get("/api/lecciones/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensaje").value("No existe la lección no-existe"))
                .andExpect(jsonPath("$.ruta").value("/api/lecciones/no-existe"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void sirveLaImagenConSuTipoYCache() throws Exception {
        mvc.perform(get("/api/lecciones/leccion-01-hola-mundo/recursos/ciclo.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")));
    }

    @Test
    void rechazaExtensionesNoPermitidas() throws Exception {
        mvc.perform(get("/api/lecciones/leccion-01-hola-mundo/recursos/config.txt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));
        mvc.perform(get("/api/lecciones/leccion-01-hola-mundo/recursos/dibujo.svg"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaPathTraversal() throws Exception {
        mvc.perform(get("/api/lecciones/leccion-01-hola-mundo/recursos/..%5Cconfig.txt"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/lecciones/leccion-01-hola-mundo/recursos/.."))
                .andExpect(status().isBadRequest());
    }

    @Test
    void imagenInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/lecciones/leccion-01-hola-mundo/recursos/otra.png"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("La lección leccion-01-hola-mundo no tiene el recurso otra.png"));
    }

    @Test
    void metodoNoPermitidoDevuelve405ConErrorJson() throws Exception {
        mvc.perform(post("/api/lecciones"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.estado").value(405));
    }

    @Test
    void agregaCabecerasDeSeguridad() throws Exception {
        mvc.perform(get("/api/lecciones"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
}
