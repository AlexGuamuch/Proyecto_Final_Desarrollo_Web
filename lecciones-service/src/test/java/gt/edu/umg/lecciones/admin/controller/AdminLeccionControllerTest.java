package gt.edu.umg.lecciones.admin.controller;

import gt.edu.umg.lecciones.admin.service.AdminLeccionService;
import gt.edu.umg.lecciones.admin.service.ZipLecciones;
import gt.edu.umg.lecciones.leccion.carga.ValidadorLeccion;
import gt.edu.umg.lecciones.leccion.repository.InMemoryLeccionRepository;
import gt.edu.umg.lecciones.leccion.repository.LeccionRepository;
import gt.edu.umg.lecciones.leccion.service.GestionLeccionesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static gt.edu.umg.lecciones.admin.ZipsDePrueba.leccion;
import static gt.edu.umg.lecciones.admin.ZipsDePrueba.zip;
import static gt.edu.umg.lecciones.leccion.Fixtures.leccionMinima;
import static gt.edu.umg.lecciones.leccion.Fixtures.utf8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminLeccionController.class, properties = "app.admin-preview.enabled=true")
@Import({AdminLeccionService.class, ZipLecciones.class, GestionLeccionesService.class,
        ValidadorLeccion.class, InMemoryLeccionRepository.class})
class AdminLeccionControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LeccionRepository repositorio;

    @BeforeEach
    void preparar() {
        repositorio.save(new ValidadorLeccion().validar("leccion-01", leccionMinima("EJECUTAR_EXITOSO")));
    }

    private static MockMultipartFile archivo(byte[] contenido) {
        return new MockMultipartFile("archivo", "lecciones.zip", "application/zip", contenido);
    }

    @Test
    void informaQueEstaHabilitado() throws Exception {
        mvc.perform(get("/api/admin/estado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitado").value(true));
    }

    @Test
    void importaLasValidasYReportaLasOmitidas() throws Exception {
        Map<String, byte[]> entradas = leccion("lecciones/", "leccion-05", "COMPILAR_EXITOSO");
        entradas.putAll(leccion("lecciones/", "leccion-01", "COMPILAR_CON_ERROR"));
        entradas.put("lecciones/sin-config/leccion.md", utf8("# sin config"));

        mvc.perform(multipart("/api/admin/lecciones/importar").file(archivo(zip(entradas))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importadas", hasSize(2)))
                .andExpect(jsonPath("$.importadas[?(@.id == 'leccion-01')].reemplazada").value(true))
                .andExpect(jsonPath("$.importadas[?(@.id == 'leccion-05')].objetivo").value("COMPILAR_EXITOSO"))
                .andExpect(jsonPath("$.omitidas", hasSize(1)))
                .andExpect(jsonPath("$.omitidas[0].carpeta").value("sin-config"))
                .andExpect(jsonPath("$.omitidas[0].motivo", containsString("config.txt")));

        assertThat(repositorio.findAll()).hasSize(2);
    }

    @Test
    void rechazaZipSlipConErrorJson() throws Exception {
        Map<String, byte[]> entradas = leccion("", "leccion-05", "COMPILAR_EXITOSO");
        entradas.put("../../fuera/leccion.md", utf8("# malicioso"));

        mvc.perform(multipart("/api/admin/lecciones/importar").file(archivo(zip(entradas))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje", containsString("salir de su carpeta")));

        assertThat(repositorio.existsById("leccion-05")).isFalse();
    }

    @Test
    void rechazaArchivosQueNoSonZipYArchivosVacios() throws Exception {
        mvc.perform(multipart("/api/admin/lecciones/importar").file(archivo(utf8("hola"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("no es un zip")));
        mvc.perform(multipart("/api/admin/lecciones/importar").file(archivo(new byte[0])))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminaUnaLeccion() throws Exception {
        mvc.perform(delete("/api/admin/lecciones/leccion-01")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/admin/lecciones/leccion-01"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe la lección leccion-01"));
    }

    @Test
    void exportaUnZipDescargable() throws Exception {
        byte[] cuerpo = mvc.perform(get("/api/admin/lecciones/exportar"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"lecciones.zip\""))
                .andReturn().getResponse().getContentAsByteArray();

        ZipLecciones.Contenido contenido = new ZipLecciones().leer(cuerpo);
        assertThat(contenido.carpetas()).containsOnlyKeys("leccion-01");
    }
}
