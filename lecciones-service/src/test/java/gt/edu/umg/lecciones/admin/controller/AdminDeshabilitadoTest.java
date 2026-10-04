package gt.edu.umg.lecciones.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Con APP_ADMIN_PREVIEW_ENABLED=false las rutas /api/admin/** no existen. */
@WebMvcTest(controllers = AdminLeccionController.class, properties = "app.admin-preview.enabled=false")
class AdminDeshabilitadoTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void lasRutasAdministrativasNoExisten() throws Exception {
        mvc.perform(get("/api/admin/estado")).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/lecciones/exportar")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/lecciones/leccion-01")).andExpect(status().isNotFound());
    }
}
