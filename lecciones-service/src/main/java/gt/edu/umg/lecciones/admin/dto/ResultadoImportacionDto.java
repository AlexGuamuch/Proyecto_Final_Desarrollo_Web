package gt.edu.umg.lecciones.admin.dto;

import gt.edu.umg.lecciones.leccion.model.Objetivo;

import java.util.List;

/**
 * Resumen de una importación. Una lección inválida no detiene el resto: queda en {@code omitidas}
 * con el motivo.
 */
public record ResultadoImportacionDto(List<Importada> importadas, List<Omitida> omitidas, List<String> avisos) {

    public record Importada(String id, Objetivo objetivo, boolean reemplazada) {
    }

    public record Omitida(String carpeta, String motivo) {
    }
}
