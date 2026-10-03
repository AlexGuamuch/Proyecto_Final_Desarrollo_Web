package gt.edu.umg.lecciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del servicio de lecciones.
 *
 * <p>Fase 1: un solo servicio que expone la API REST de lecciones y sirve el frontend
 * estático. Está organizado como monolito modular (un paquete por módulo de negocio)
 * para poder separar cada módulo en su propio microservicio en la fase 2.
 */
@SpringBootApplication
public class LeccionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeccionesApplication.class, args);
    }
}
