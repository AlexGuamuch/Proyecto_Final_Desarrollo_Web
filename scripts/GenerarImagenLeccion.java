import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Genera la imagen ciclo.png de la lección 1 (escribir → compilar → ejecutar).
 *
 * Uso, desde la raíz del repositorio (Java 11 o superior, sin compilar antes):
 *   java scripts/GenerarImagenLeccion.java
 */
public class GenerarImagenLeccion {

    private static final int ANCHO = 1200;
    private static final int ALTO = 470;
    private static final Color PAPEL = new Color(0xFCFDFE);
    private static final Color RENGLON = new Color(0xD5E0EF);
    private static final Color TINTA = new Color(0x1A2238);
    private static final Color GRAFITO = new Color(0x4B5567);
    private static final Color ACENTO = new Color(0x3341B5);
    private static final Color ERROR = new Color(0xB3261E);
    private static final String DESTINO =
            "lecciones-service/src/main/resources/lecciones/leccion-01-hola-mundo/ciclo.png";

    public static void main(String[] args) throws IOException {
        BufferedImage imagen = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Hoja de cuaderno: fondo, renglones y margen.
        g.setColor(PAPEL);
        g.fillRect(0, 0, ANCHO, ALTO);
        g.setColor(RENGLON);
        g.setStroke(new BasicStroke(2));
        for (int y = 46; y < ALTO; y += 46) {
            g.drawLine(0, y, ANCHO, y);
        }
        g.setColor(new Color(0xE58A97));
        g.drawLine(60, 0, 60, ALTO);

        String[][] pasos = {
                {"1", "Escribir", "Main.java", "tu código, a mano"},
                {"2", "Compilar", "javac Main.java", "revisa las reglas"},
                {"3", "Ejecutar", "java Main", "el programa corre"},
        };
        int anchoCaja = 300;
        int altoCaja = 190;
        int separacion = 70;
        int x0 = 105;
        int y0 = 70;
        for (int i = 0; i < pasos.length; i++) {
            int x = x0 + i * (anchoCaja + separacion);
            dibujarPaso(g, x, y0, anchoCaja, altoCaja, pasos[i]);
            if (i < pasos.length - 1) {
                flecha(g, x + anchoCaja + 12, y0 + altoCaja / 2, x + anchoCaja + separacion - 12, y0 + altoCaja / 2, ACENTO);
            }
        }

        // Flecha de regreso: si el compilador encuentra un error, se corrige y se vuelve a compilar.
        int xCompilar = x0 + anchoCaja + separacion + anchoCaja / 2;
        int xEscribir = x0 + anchoCaja / 2;
        int yBase = y0 + altoCaja + 16;
        int yRegreso = yBase + 80;
        g.setColor(ERROR);
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, new float[]{14, 10}, 0));
        Path2D camino = new Path2D.Double();
        camino.moveTo(xCompilar, yBase);
        camino.lineTo(xCompilar, yRegreso);
        camino.lineTo(xEscribir, yRegreso);
        camino.lineTo(xEscribir, yBase + 18);
        g.draw(camino);
        punta(g, xEscribir, yBase + 4, 0, -1, ERROR);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        String aviso = "¿Hay un error? Lee la línea que indica, corrige y vuelve a compilar";
        FontMetrics fm = g.getFontMetrics();
        int anchoAviso = fm.stringWidth(aviso);
        int xAviso = (ANCHO - anchoAviso) / 2;
        g.setColor(PAPEL);
        g.fillRect(xAviso - 12, yRegreso + 18, anchoAviso + 24, 40);
        g.setColor(ERROR);
        g.drawString(aviso, xAviso, yRegreso + 48);

        g.dispose();
        File archivo = new File(DESTINO);
        if (!archivo.getParentFile().isDirectory()) {
            throw new IOException("Ejecuta el script desde la raíz del repositorio; no existe " + archivo.getParent());
        }
        ImageIO.write(imagen, "png", archivo);
        System.out.println("Imagen generada: " + archivo.getPath());
    }

    private static void dibujarPaso(Graphics2D g, int x, int y, int ancho, int alto, String[] paso) {
        RoundRectangle2D caja = new RoundRectangle2D.Double(x, y, ancho, alto, 18, 18);
        g.setColor(Color.WHITE);
        g.fill(caja);
        g.setColor(ACENTO);
        g.setStroke(new BasicStroke(4));
        g.draw(caja);

        g.setColor(ACENTO);
        g.fillOval(x + 22, y + 22, 52, 52);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        centrado(g, paso[0], x + 48, y + 59);

        g.setColor(TINTA);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 38));
        g.drawString(paso[1], x + 90, y + 62);

        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 27));
        g.drawString(paso[2], x + 24, y + 122);

        g.setColor(GRAFITO);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 25));
        g.drawString(paso[3], x + 24, y + 162);
    }

    private static void centrado(Graphics2D g, String texto, int cx, int baseline) {
        g.drawString(texto, cx - g.getFontMetrics().stringWidth(texto) / 2, baseline);
    }

    private static void flecha(Graphics2D g, int x1, int y1, int x2, int y2, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x1, y1, x2 - 14, y2);
        punta(g, x2, y2, 1, 0, color);
    }

    /** Punta de flecha en (x, y) apuntando en la dirección (dx, dy). */
    private static void punta(Graphics2D g, int x, int y, int dx, int dy, Color color) {
        g.setColor(color);
        int largo = 22;
        int medio = 13;
        Polygon p = new Polygon();
        p.addPoint(x, y);
        p.addPoint(x - dx * largo - dy * medio, y - dy * largo - dx * medio);
        p.addPoint(x - dx * largo + dy * medio, y - dy * largo + dx * medio);
        g.fillPolygon(p);
    }
}
