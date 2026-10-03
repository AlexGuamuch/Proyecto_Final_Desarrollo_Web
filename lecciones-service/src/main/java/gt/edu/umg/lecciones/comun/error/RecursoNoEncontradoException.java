package gt.edu.umg.lecciones.comun.error;

/** Lo solicitado no existe; se responde con 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
