package gt.edu.umg.lecciones.comun.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Cabeceras de seguridad para todas las respuestas. La política de contenido solo permite
 * recursos del propio servidor: la app no depende de CDNs y funciona sin internet, y si un
 * markdown intentara inyectar algo externo el navegador lo bloquearía.
 * En la fase 2 estas cabeceras pueden moverse al reverse proxy / API Gateway.
 */
@Component
public class CabecerasSeguridadFilter extends OncePerRequestFilter {

    static final String POLITICA_CONTENIDO = String.join("; ",
            "default-src 'self'",
            "img-src 'self' data:",
            "script-src 'self'",
            "style-src 'self'",
            "font-src 'self'",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Override
    protected void doFilterInternal(HttpServletRequest solicitud, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        respuesta.setHeader("Content-Security-Policy", POLITICA_CONTENIDO);
        respuesta.setHeader("X-Content-Type-Options", "nosniff");
        respuesta.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        respuesta.setHeader("X-Frame-Options", "DENY");
        cadena.doFilter(solicitud, respuesta);
    }
}
