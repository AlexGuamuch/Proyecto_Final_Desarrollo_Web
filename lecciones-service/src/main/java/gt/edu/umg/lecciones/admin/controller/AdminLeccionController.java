package gt.edu.umg.lecciones.admin.controller;

import gt.edu.umg.lecciones.admin.dto.ResultadoImportacionDto;
import gt.edu.umg.lecciones.admin.service.AdminLeccionService;
import gt.edu.umg.lecciones.comun.error.SolicitudInvalidaException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Opciones de Configuración sobre las lecciones (importar, eliminar, exportar).
 *
 * <p>Todo vive bajo {@code /api/admin/**}: en la fase 2 basta una regla de Spring Security sobre
 * ese prefijo para exigir un administrador autenticado. Se activa con
 * {@code app.admin-preview.enabled} (variable {@code APP_ADMIN_PREVIEW_ENABLED}); si está apagado,
 * estas rutas no existen (404).
 */
@RestController
@RequestMapping("/api/admin")
@ConditionalOnProperty(name = "app.admin-preview.enabled", havingValue = "true")
public class AdminLeccionController {

    private final AdminLeccionService servicio;

    public AdminLeccionController(AdminLeccionService servicio) {
        this.servicio = servicio;
    }

    /** Permite al frontend saber si debe mostrar estas opciones. */
    @GetMapping("/estado")
    public Map<String, Object> estado() {
        return Map.of("habilitado", true, "autenticacion", false);
    }

    @PostMapping(path = "/lecciones/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultadoImportacionDto importar(@RequestParam("archivo") MultipartFile archivo) throws IOException {
        if (archivo.isEmpty()) {
            throw new SolicitudInvalidaException("El archivo está vacío");
        }
        return servicio.importar(archivo.getBytes());
    }

    @DeleteMapping("/lecciones/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lecciones/exportar")
    public ResponseEntity<byte[]> exportar() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("lecciones.zip").build().toString())
                .body(servicio.exportar());
    }
}
