package gt.edu.umg.lecciones.comun.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;

/**
 * Traduce las excepciones a respuestas JSON con la forma de {@link ErrorRespuesta}.
 * Nunca expone trazas ni detalles internos; los errores inesperados se registran en el log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(RecursoNoEncontradoException e, HttpServletRequest solicitud) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage(), solicitud);
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorRespuesta> invalida(SolicitudInvalidaException e, HttpServletRequest solicitud) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage(), solicitud);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorRespuesta> demasiadoGrande(MaxUploadSizeExceededException e, HttpServletRequest solicitud) {
        return responder(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo permitido (10 MB)", solicitud);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> general(Exception e, HttpServletRequest solicitud) {
        // Excepciones propias de Spring MVC (404 de estáticos, 405, 415...) ya traen su código HTTP.
        if (e instanceof ErrorResponse respuesta) {
            HttpStatusCode codigo = respuesta.getStatusCode();
            return responder(codigo, mensajePara(codigo), solicitud);
        }
        log.error("Error inesperado en {} {}", solicitud.getMethod(), solicitud.getRequestURI(), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado en el servidor", solicitud);
    }

    private static String mensajePara(HttpStatusCode codigo) {
        return switch (codigo.value()) {
            case 400 -> "La solicitud no es válida";
            case 404 -> "No existe el recurso solicitado";
            case 405 -> "Método HTTP no permitido en esta ruta";
            case 406 -> "No se puede responder en el formato solicitado";
            case 415 -> "Tipo de contenido no soportado";
            default -> "No se pudo procesar la solicitud";
        };
    }

    private static ResponseEntity<ErrorRespuesta> responder(HttpStatusCode codigo, String mensaje, HttpServletRequest solicitud) {
        HttpStatus estado = HttpStatus.resolve(codigo.value());
        String error = estado != null ? estado.getReasonPhrase() : String.valueOf(codigo.value());
        ErrorRespuesta cuerpo = new ErrorRespuesta(Instant.now(), codigo.value(), error, mensaje, solicitud.getRequestURI());
        return ResponseEntity.status(codigo).contentType(MediaType.APPLICATION_JSON).body(cuerpo);
    }
}
