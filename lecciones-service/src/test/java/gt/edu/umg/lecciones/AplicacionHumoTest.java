package gt.edu.umg.lecciones;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de humo con la aplicación completa en un puerto real: lo mismo que revisa la nube
 * al desplegar, más el rechazo de rutas codificadas por parte del servidor embebido.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AplicacionHumoTest {

    @LocalServerPort
    private int puerto;

    private final HttpClient cliente = HttpClient.newHttpClient();

    private HttpResponse<String> get(String ruta) throws Exception {
        // URI.create no vuelve a codificar la ruta: llega al servidor tal como está escrita.
        HttpRequest solicitud = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).GET().build();
        return cliente.send(solicitud, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthRespondeUp() throws Exception {
        HttpResponse<String> respuesta = get("/actuator/health");

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void cargaLasCuatroLeccionesAlArrancar() throws Exception {
        HttpResponse<String> respuesta = get("/api/lecciones");

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("leccion-01-hola-mundo", "leccion-02-errores-compilacion",
                "leccion-03-variables-y-tipos", "leccion-04-errores-ejecucion");
    }

    @Test
    void sirveLaImagenDeLaLeccionUno() throws Exception {
        HttpResponse<String> respuesta = get("/api/lecciones/leccion-01-hola-mundo/recursos/ciclo.png");

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.headers().firstValue("Content-Type")).hasValue("image/png");
    }

    @Test
    void sirveElFrontend() throws Exception {
        HttpResponse<String> respuesta = get("/");

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("<html lang=\"es\"");
    }

    @Test
    void soloExponeHealthEnActuator() throws Exception {
        assertThat(get("/actuator/env").statusCode()).isEqualTo(404);
        assertThat(get("/actuator/beans").statusCode()).isEqualTo(404);
    }

    @Test
    void rechazaRutasCodificadasQueIntentanSalirDeLaLeccion() throws Exception {
        for (String ruta : new String[]{
                "/api/lecciones/leccion-01-hola-mundo/recursos/..%2F..%2Fconfig.txt",
                "/api/lecciones/leccion-01-hola-mundo/recursos/%2e%2e%2fconfig.txt",
                "/api/lecciones/leccion-01-hola-mundo/recursos/..%5Cconfig.txt"}) {
            HttpResponse<String> respuesta = get(ruta);
            assertThat(respuesta.statusCode()).as(ruta).isIn(400, 404);
            assertThat(respuesta.body()).as(ruta).doesNotContain("objetivo=");
        }
    }

    @Test
    void lasOpcionesDeConfiguracionSobreLeccionesEstanActivasPorDefecto() throws Exception {
        HttpResponse<String> estado = get("/api/admin/estado");

        assertThat(estado.statusCode()).isEqualTo(200);
        assertThat(estado.body()).contains("\"habilitado\":true");
        assertThat(get("/api/admin/lecciones/exportar").headers().firstValue("Content-Type")).hasValue("application/zip");
    }
}
