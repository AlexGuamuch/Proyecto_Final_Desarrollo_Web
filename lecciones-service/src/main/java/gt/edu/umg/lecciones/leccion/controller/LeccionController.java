package gt.edu.umg.lecciones.leccion.controller;

import gt.edu.umg.lecciones.leccion.dto.LeccionDetalleDto;
import gt.edu.umg.lecciones.leccion.dto.LeccionResumenDto;
import gt.edu.umg.lecciones.leccion.model.Recurso;
import gt.edu.umg.lecciones.leccion.service.LeccionService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/** API pública de lecciones (no requiere autenticación, tampoco en la fase 2). */
@RestController
@RequestMapping("/api/lecciones")
public class LeccionController {

    private final LeccionService servicio;

    public LeccionController(LeccionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<LeccionResumenDto> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    public LeccionDetalleDto obtener(@PathVariable String id) {
        return servicio.obtener(id);
    }

    @GetMapping("/{id}/recursos/{archivo}")
    public ResponseEntity<byte[]> recurso(@PathVariable String id, @PathVariable String archivo) {
        Recurso recurso = servicio.obtenerRecurso(id, archivo);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(recurso.tipo().tipoContenido()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(recurso.contenido());
    }
}
