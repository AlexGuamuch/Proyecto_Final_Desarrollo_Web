package gt.edu.umg.lecciones.leccion.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownRendererTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();

    @ParameterizedTest
    @ValueSource(strings = {"ciclo.png", "./ciclo.png"})
    void reescribeImagenesRelativasHaciaElEndpointDeRecursos(String ruta) {
        String html = renderer.renderizar("leccion-01", "![Ciclo](" + ruta + ")");

        assertThat(html).contains("src=\"/api/lecciones/leccion-01/recursos/ciclo.png\"")
                .contains("alt=\"Ciclo\"");
    }

    @Test
    void reescribeEnlacesRelativosAImagenes() {
        String html = renderer.renderizar("l1", "[ver en grande](ciclo.png) y [otra lección](otra.md)");

        assertThat(html).contains("href=\"/api/lecciones/l1/recursos/ciclo.png\"")
                .contains("href=\"otra.md\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/a.png", "/img/a.png", "../a.png", "sub/a.png"})
    void noReescribeRutasAbsolutasNiConCarpetas(String ruta) {
        String html = renderer.renderizar("l1", "![x](" + ruta + ")");

        assertThat(html).doesNotContain("/api/lecciones/");
    }

    @Test
    void escapaHtmlCrudo() {
        String html = renderer.renderizar("l1", "Hola <script>alert('xss')</script>\n\n<img src=x onerror=alert(1)>");

        assertThat(html).doesNotContain("<script>").doesNotContain("<img src=x")
                .contains("&lt;script&gt;");
    }

    @Test
    void neutralizaUrlsJavascript() {
        String html = renderer.renderizar("l1", "[clic](javascript:alert(1)) ![x](javascript:alert(2))");

        assertThat(html).doesNotContainIgnoringCase("javascript:");
    }

    @Test
    void enlacesExternosAbrenEnOtraPestana() {
        String html = renderer.renderizar("l1", "[Java](https://dev.java)");

        assertThat(html).contains("target=\"_blank\"").contains("rel=\"noopener noreferrer\"");
    }

    @Test
    void renderizaCodigoConLenguajeYTablas() {
        String markdown = """
                ```java
                int x = 1 < 2 ? 1 : 0;
                ```

                | Tipo | Ejemplo |
                |---|---|
                | `int` | `19` |
                """;

        String html = renderer.renderizar("l1", markdown);

        assertThat(html).contains("<code class=\"language-java\">int x = 1 &lt; 2 ? 1 : 0;")
                .contains("<table>")
                .contains("<td><code>int</code></td>");
    }
}
