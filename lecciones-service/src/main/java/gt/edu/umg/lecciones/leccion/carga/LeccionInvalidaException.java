package gt.edu.umg.lecciones.leccion.carga;

/** Una carpeta de lección no cumple el formato esperado; el mensaje explica qué falla. */
public class LeccionInvalidaException extends RuntimeException {

    public LeccionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
