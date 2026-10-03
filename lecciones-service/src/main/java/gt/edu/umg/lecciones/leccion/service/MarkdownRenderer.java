package gt.edu.umg.lecciones.leccion.service;

import gt.edu.umg.lecciones.leccion.model.Nombres;
import gt.edu.umg.lecciones.leccion.model.TipoImagen;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Renderiza el markdown de una lección a HTML en el servidor.
 *
 * <ul>
 *   <li>El HTML crudo dentro del markdown se escapa (se muestra como texto), así una lección
 *       no puede inyectar scripts (XSS).</li>
 *   <li>Las URLs peligrosas (por ejemplo {@code javascript:}) se neutralizan.</li>
 *   <li>Las imágenes con ruta relativa ({@code foto.png}, {@code ./foto.png}) se reescriben hacia
 *       {@code /api/lecciones/{id}/recursos/foto.png}.</li>
 *   <li>Los enlaces externos se abren en otra pestaña para no perder lo escrito en el editor.</li>
 * </ul>
 */
@Component
public class MarkdownRenderer {

    private static final Pattern ESQUEMA = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:");

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownRenderer() {
        List<Extension> extensiones = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensiones).build();
        this.renderer = HtmlRenderer.builder()
                .extensions(extensiones)
                .escapeHtml(true)
                .sanitizeUrls(true)
                .attributeProviderFactory(contexto -> new AtributosSeguros())
                .build();
    }

    public String renderizar(String idLeccion, String markdown) {
        Node documento = parser.parse(markdown);
        documento.accept(new ReescritorRutas(idLeccion));
        return renderer.render(documento);
    }

    /** Ruta pública de un recurso de la lección. */
    static String rutaRecurso(String idLeccion, String archivo) {
        return "/api/lecciones/" + UriUtils.encodePathSegment(idLeccion, StandardCharsets.UTF_8)
                + "/recursos/" + UriUtils.encodePathSegment(archivo, StandardCharsets.UTF_8);
    }

    /** Devuelve el nombre de archivo si la ruta es relativa y simple ({@code foto.png} o {@code ./foto.png}). */
    static String archivoRelativo(String destino) {
        if (destino == null || destino.isBlank() || ESQUEMA.matcher(destino).find()
                || destino.startsWith("/") || destino.startsWith("\\") || destino.startsWith("#")) {
            return null;
        }
        String nombre = destino;
        while (nombre.startsWith("./")) {
            nombre = nombre.substring(2);
        }
        return Nombres.esArchivoValido(nombre) ? nombre : null;
    }

    private static final class ReescritorRutas extends AbstractVisitor {

        private final String idLeccion;

        private ReescritorRutas(String idLeccion) {
            this.idLeccion = idLeccion;
        }

        @Override
        public void visit(Image imagen) {
            String archivo = archivoRelativo(imagen.getDestination());
            if (archivo != null) {
                imagen.setDestination(rutaRecurso(idLeccion, archivo));
            }
            visitChildren(imagen);
        }

        @Override
        public void visit(Link enlace) {
            // Un enlace directo a una imagen de la lección (ej. "ver en grande") también se reescribe.
            String archivo = archivoRelativo(enlace.getDestination());
            if (archivo != null && esImagen(archivo)) {
                enlace.setDestination(rutaRecurso(idLeccion, archivo));
            }
            visitChildren(enlace);
        }

        private static boolean esImagen(String archivo) {
            return TipoImagen.desdeNombreArchivo(archivo).isPresent();
        }
    }

    private static final class AtributosSeguros implements AttributeProvider {

        @Override
        public void setAttributes(Node nodo, String etiqueta, Map<String, String> atributos) {
            if (nodo instanceof Link && atributos.getOrDefault("href", "").matches("(?i)^https?://.*")) {
                atributos.put("target", "_blank");
                atributos.put("rel", "noopener noreferrer");
            }
            if (nodo instanceof Image) {
                atributos.put("loading", "lazy");
                atributos.put("decoding", "async");
            }
        }
    }
}
