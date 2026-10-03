package gt.edu.umg.lecciones.comun.error;

/** La solicitud está mal formada o pide algo no permitido; se responde con 400. */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
