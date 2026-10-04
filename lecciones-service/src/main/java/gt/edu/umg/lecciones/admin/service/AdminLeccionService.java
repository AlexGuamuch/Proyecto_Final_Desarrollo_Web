package gt.edu.umg.lecciones.admin.service;

import gt.edu.umg.lecciones.admin.dto.ResultadoImportacionDto;
import gt.edu.umg.lecciones.admin.dto.ResultadoImportacionDto.Importada;
import gt.edu.umg.lecciones.admin.dto.ResultadoImportacionDto.Omitida;
import gt.edu.umg.lecciones.leccion.carga.LeccionInvalidaException;
import gt.edu.umg.lecciones.leccion.service.GestionLeccionesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Opciones de Configuración que modifican las lecciones: importar un compendio (zip),
 * eliminar una lección y exportar todas. En la fase 1 trabajan sobre el repositorio en memoria;
 * en la fase 2 requerirán un administrador autenticado.
 */
@Service
public class AdminLeccionService {

    private static final Logger log = LoggerFactory.getLogger(AdminLeccionService.class);

    private final ZipLecciones zip;
    private final GestionLeccionesService gestion;

    public AdminLeccionService(ZipLecciones zip, GestionLeccionesService gestion) {
        this.zip = zip;
        this.gestion = gestion;
    }

    public ResultadoImportacionDto importar(byte[] archivoZip) {
        ZipLecciones.Contenido contenido = zip.leer(archivoZip);
        List<Importada> importadas = new ArrayList<>();
        List<Omitida> omitidas = new ArrayList<>();

        for (Map.Entry<String, Map<String, byte[]>> carpeta : contenido.carpetas().entrySet()) {
            try {
                GestionLeccionesService.Registro registro = gestion.registrar(carpeta.getKey(), carpeta.getValue());
                importadas.add(new Importada(registro.id(), registro.objetivo(), registro.reemplazada()));
            } catch (LeccionInvalidaException e) {
                omitidas.add(new Omitida(carpeta.getKey(), e.getMessage()));
            }
        }
        log.info("Importación de lecciones: {} importadas, {} omitidas", importadas.size(), omitidas.size());
        return new ResultadoImportacionDto(importadas, omitidas, contenido.avisos());
    }

    public void eliminar(String id) {
        gestion.eliminar(id);
        log.info("Lección eliminada: {}", id);
    }

    public byte[] exportar() {
        return zip.escribir(gestion.exportar());
    }
}
