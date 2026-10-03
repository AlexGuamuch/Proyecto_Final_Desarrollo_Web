# Diagrama de despliegue

Dos vistas: lo que está desplegado hoy (fase 1) y la arquitectura objetivo de la fase 2, que agrega seguridad perimetral, API Gateway, microservicios y MariaDB.

## Fase 1 (actual)

![Despliegue fase 1](img/despliegue-fase1.png)

```mermaid
flowchart LR
    subgraph dispositivo["Dispositivo del estudiante"]
        nav["Navegador<br/>HTML, CSS y JS (ES modules)<br/>estado de configuración en memoria"]
    end

    subgraph github["GitHub"]
        repo["Repositorio<br/>rama main"]
        ci["GitHub Actions<br/>mvn verify, docker build,<br/>prueba del contenedor"]
        repo --> ci
    end

    subgraph render["Render, plan gratuito, región Ohio"]
        borde["Borde de Render<br/>HTTPS y certificado TLS<br/>dominio onrender.com"]
        subgraph contenedor["Contenedor Docker, usuario uid 10001, 512 MB"]
            app["lecciones-service<br/>Spring Boot 3.5, Java 21<br/>escucha en PORT"]
            memoria[("Lecciones en memoria<br/>ConcurrentHashMap")]
            estaticos["Frontend estático<br/>static/"]
            app --> memoria
            app --- estaticos
        end
        salud["Health check<br/>GET /actuator/health"]
        borde --> app
        salud -.-> app
    end

    nav -- "HTTPS 443<br/>/ y /api/lecciones/**" --> borde
    ci -- "checks en verde:<br/>build de la imagen y despliegue" --> render
```

| Nodo | Qué corre | Detalle |
|---|---|---|
| Navegador | Interfaz de práctica | Sin dependencias externas: fuentes, íconos y scripts vienen del servidor. Tamaño de letra y tema solo en memoria. |
| Borde de Render | Terminación TLS y enrutamiento | Render publica el servicio en `https://<servicio>.onrender.com` y reenvía al puerto `PORT` del contenedor. |
| Contenedor | `lecciones-service` (jar de Spring Boot sobre `eclipse-temurin:21-jre`) | Sirve la API REST y el frontend. Al arrancar carga las lecciones del classpath. |
| Health check | `GET /actuator/health` | Render no envía tráfico a una versión nueva hasta que responde `UP`. |
| GitHub Actions | Compilación, pruebas e imagen | Render despliega solo los commits cuyo CI pasó (`autoDeployTrigger: checksPass`). |

Limitaciones aceptadas en la fase 1: una sola instancia, el servicio se duerme tras 15 minutos sin tráfico, y los datos en memoria se reinician en cada despliegue (las lecciones de ejemplo se vuelven a cargar del classpath).

## Fase 2 (objetivo)

![Despliegue fase 2](img/despliegue-fase2.png)

```mermaid
flowchart LR
    nav["Navegador<br/>estudiante o administrador"]

    subgraph borde["Perímetro"]
        cdn["CDN seguro y WAF<br/>TLS, caché de estáticos,<br/>reglas OWASP, anti-DDoS"]
    end

    subgraph plataforma["Plataforma de contenedores"]
        gw["API Gateway / reverse proxy<br/>Spring Cloud Gateway<br/>rutas, rate limiting, CORS,<br/>validación de sesión o JWT,<br/>registro de visitas"]
        web["web-frontend<br/>HTML, CSS y JS estáticos"]
        lec["lecciones-service<br/>/api/lecciones/**"]
        his["historial-service<br/>/api/historial/**"]
        eje["ejecucion-service<br/>/api/ejecucion/**<br/>guali-dev.jar, límites de<br/>CPU, memoria y tiempo"]
        adm["admin-service<br/>/api/admin/**<br/>Spring Security, CRUD de<br/>administradores, estadísticas,<br/>importar y exportar lecciones"]
    end

    subgraph datos["Datos"]
        db[("MariaDB<br/>esquemas: lecciones, historial, admin<br/>respaldos automáticos")]
    end

    nav -- HTTPS --> cdn
    cdn --> gw
    gw -- "/" --> web
    gw -- "/api/lecciones/**" --> lec
    gw -- "/api/historial/**" --> his
    gw -- "/api/ejecucion/**" --> eje
    gw -- "/api/admin/**<br/>solo autenticado" --> adm

    eje -- "registra intentos" --> his
    his -- "consulta objetivo" --> lec
    adm -- "importa y elimina" --> lec

    lec --> db
    his --> db
    adm --> db
```

| Componente | Responsabilidad | Requisito del enunciado que cubre |
|---|---|---|
| CDN seguro + WAF | TLS, caché de archivos estáticos, filtro de ataques comunes y mitigación de denegación de servicio | Seguridad: WAF, anti-DoS, CDN seguro |
| API Gateway / reverse proxy | Único punto de entrada: enruta por prefijo, limita peticiones por IP, valida la sesión del administrador en `/api/admin/**` y registra cada visita | Seguridad: reverse proxy con seguridad integrada; estadísticas de visitas |
| `web-frontend` | Los mismos archivos de `static/` de la fase 1 | Interfaz gráfica |
| `lecciones-service` | El servicio de la fase 1 con `LeccionRepository` implementado sobre JPA | Lecciones en base de datos |
| `historial-service` | Sesiones de lección e intentos; decide si la sesión cumplió su objetivo | Panel Historial de Sesión |
| `ejecucion-service` | Compila y ejecuta con `guali-dev.jar`. Aislado y con límites de recursos, porque ejecuta código de terceros | Botones Compilar y Ejecutar |
| `admin-service` | Autenticación con Spring Security, CRUD de administradores, importar, exportar y eliminar lecciones, estadísticas | Administración del sistema |
| MariaDB | Persistencia, un esquema por servicio | Base de datos relacional MariaDB con JPA |

## Paso de la fase 1 a la fase 2

1. Implementar `JpaLeccionRepository` en `lecciones-service`. Controllers y services no cambian (ver [ADR-002](decisiones.md#adr-002-repositorio-de-lecciones-en-memoria-detrás-de-una-interfaz)).
2. Agregar el gateway delante, con las mismas rutas `/api/**` que ya usa el frontend.
3. Mover `static/` a `web-frontend` (o dejar que el gateway lo sirva).
4. Crear `historial-service`, `ejecucion-service` y `admin-service` con sus tablas del [diagrama ER](diagrama-er.md).
5. Activar Spring Security en `/api/admin/**`. Las opciones de administración en vista previa de la fase 1 ya viven bajo ese prefijo.
6. Poner el CDN con WAF delante del gateway.
