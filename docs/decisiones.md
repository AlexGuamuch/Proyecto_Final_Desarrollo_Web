# Decisiones de diseño — Fase 1

Este documento justifica las decisiones funcionales y no funcionales de la fase 1. Las decisiones de arquitectura están escritas como ADR (Architecture Decision Record): contexto, decisión, alternativas consideradas, consecuencias y lo que queda pendiente para la fase 2.

**Restricciones que condicionan todas las decisiones**

- La fase 1 debe quedar desplegada en la nube con una URL pública y entregarse el 3 de octubre de 2026.
- La arquitectura objetivo del enunciado es cloud-native: HTML, CSS, JavaScript, Spring, MVC, REST, Security, JPA, microservicios y MariaDB.
- La fase 1 **no** incluye base de datos, seguridad, administración, Compilar, Ejecutar ni Historial.
- La configuración del estudiante (tamaño de letra y tema) solo puede vivir en memoria.
- Proyecto individual, con presupuesto cero para infraestructura.

## Resumen

| ADR | Decisión | Estado |
|---|---|---|
| [001](#adr-001-monolito-modular-en-la-fase-1-microservicios-en-la-fase-2) | Un solo servicio organizado como monolito modular; se separa en microservicios en la fase 2 | Aceptada |
| [002](#adr-002-repositorio-de-lecciones-en-memoria-detrás-de-una-interfaz) | Repositorio en memoria detrás de la interfaz `LeccionRepository` | Aceptada |
| [003](#adr-003-markdown-renderizado-en-el-servidor-con-html-escapado) | Markdown renderizado en el servidor con commonmark-java, HTML crudo escapado | Aceptada |
| [004](#adr-004-imágenes-servidas-desde-memoria-con-lista-blanca) | Imágenes servidas desde memoria, con lista blanca de formatos y nombres | Aceptada |
| [005](#adr-005-frontend-con-html-css-y-javascript-sin-frameworks-ni-build) | Frontend con HTML, CSS y JavaScript sin frameworks ni build step | Aceptada |
| [006](#adr-006-configuración-del-estudiante-solo-en-memoria) | Tamaño de letra y tema solo en variables JavaScript | Aceptada (fase 1) |
| [007](#adr-007-editor-de-código-sin-ayudas) | Editor `<textarea>` sin autocompletado ni ayudas inteligentes | Aceptada |
| [008](#adr-008-render-como-plataforma-de-nube-con-docker) | Render (plan gratuito) con imagen Docker | Aceptada (fase 1) |

---

## ADR-001: Monolito modular en la fase 1, microservicios en la fase 2

**Estado:** Aceptada · **Fecha:** 2026-10-03 · **Decide:** autor del proyecto, con revisión del catedrático

### Contexto

El enunciado pide microservicios como arquitectura objetivo. La fase 1 solo necesita un dominio, las lecciones, y tiene que estar desplegada en días. Separar ahora en varios servicios implicaría un gateway, descubrimiento de servicios, varias imágenes y varios despliegues gratuitos para un solo caso de uso.

### Decisión

Un solo servicio, `lecciones-service`, que expone la API REST y sirve el frontend estático. Por dentro se organiza como **monolito modular**:

- Un paquete por módulo de negocio (`leccion/`; en la fase 2 se suman `historial/`, `ejecucion/`, `admin/` y `estadisticas/`).
- Dentro de cada módulo, las capas MVC: `controller → service → repository → model/dto`.
- Un módulo solo usa los `service` y DTO de otro, nunca su repositorio.
- El frontend llama rutas absolutas `/api/<módulo>/**`, las mismas que expondrá el API Gateway.

### Alternativas consideradas

**A. Monolito modular (elegida)**

| Dimensión | Evaluación |
|---|---|
| Complejidad | Baja: un proceso, un despliegue, una imagen |
| Costo | Un servicio gratuito en Render |
| Escalabilidad | Suficiente para la fase 1; escala horizontal sin estado |
| Familiaridad | Alta: Spring Boot estándar |

A favor: entrega rápida y simple; las fronteras ya están marcadas por paquetes y por prefijos de URL.
En contra: la separación es por convención; nada impide técnicamente que un módulo use el repositorio de otro.

**B. Microservicios desde la fase 1**

| Dimensión | Evaluación |
|---|---|
| Complejidad | Alta: gateway, varios servicios, configuración de red |
| Costo | Varios servicios; en planes gratuitos cada uno se duerme por separado |
| Escalabilidad | Independiente por servicio, sin necesidad real todavía |
| Familiaridad | Media |

A favor: coincide desde ya con la arquitectura objetivo.
En contra: hoy solo existe un dominio. Habría servicios vacíos, más puntos de falla y más tiempo de arranque en frío.

**C. Monolito sin módulos (por capas globales)**

A favor: lo más rápido de escribir.
En contra: separar después obliga a reorganizar todo el código.

### Análisis

Se paga una sola vez la estructura modular, que cuesta poco, y se aplaza la complejidad operativa de los microservicios hasta que haya más de un dominio (fase 2: historial, ejecución, administración y estadísticas).

### Consecuencias

- Más fácil: desplegar, probar de punta a punta y depurar.
- Más difícil: no hay escalado independiente por módulo (no hace falta en la fase 1).
- A revisar en la fase 2: extraer cada módulo a su servicio, agregar el API Gateway (que enruta `/api/lecciones/**`, `/api/historial/**`, etc.) y servir el frontend desde el gateway o un CDN.

### Pendientes para la fase 2

1. [ ] Si los módulos crecen, agregar una prueba de arquitectura (ArchUnit o Spring Modulith) que verifique la regla "sin acceso a repositorios ajenos".
2. [ ] Crear `historial-service`, `ejecucion-service` (con `guali-dev.jar`) y `admin-service`, más el gateway.

---

## ADR-002: Repositorio de lecciones en memoria detrás de una interfaz

**Estado:** Aceptada · **Fecha:** 2026-10-03

### Contexto

La fase 1 no lleva base de datos. Las lecciones vienen dentro del proyecto (carpeta `lecciones/` del classpath) y se cargan al arrancar. En la fase 2 se guardarán en MariaDB con JPA, junto con las importadas por los administradores.

### Decisión

- Interfaz `LeccionRepository` con `findAll`, `findById`, `save`, `deleteById` y `existsById`. Son los mismos nombres que usa Spring Data, así la fase 2 puede implementarla con `JpaRepository` sin tocar controllers ni services.
- Implementación `InMemoryLeccionRepository` con `ConcurrentHashMap`, segura para peticiones concurrentes.
- El repositorio guarda la lección "cruda" (markdown e imágenes en bytes); el HTML se genera en el service al consultarla.

### Alternativas consideradas

| Opción | Complejidad | Ventaja | Desventaja |
|---|---|---|---|
| **Interfaz + memoria (elegida)** | Baja | Cambio a JPA sin tocar otras capas | Datos volátiles (aceptable: las lecciones vienen del classpath) |
| H2 en memoria con JPA | Media | Adelanta el modelo de la fase 2 | Mete JPA en una fase que lo excluye y obliga a migrar de dialecto |
| Leer los archivos en cada petición | Baja | Sin estado | E/S en cada consulta, path traversal más delicado, nada que reemplazar por JPA |

### Consecuencias

- Las lecciones importadas en la vista previa de administración (prioridad 2) se pierden al reiniciar y no se comparten entre instancias. Está documentado y lo resuelve la base de datos de la fase 2.
- La carga valida cada lección (`ValidadorLeccion`). Una lección inválida se registra como advertencia y se omite, sin detener el servicio.

### Pendientes para la fase 2

1. [ ] `JpaLeccionRepository` con las entidades del [diagrama ER](diagrama-er.md).
2. [ ] Cargar las lecciones de ejemplo como datos iniciales (migración Flyway o `CommandLineRunner` idempotente).

---

## ADR-003: Markdown renderizado en el servidor con HTML escapado

**Estado:** Aceptada · **Fecha:** 2026-10-03

### Contexto

Las lecciones son archivos `leccion.md`. En la fase 2 los administradores podrán importar lecciones de terceros en un zip, así que el contenido no es de confianza: un markdown puede incluir HTML crudo (`<script>`, `<img onerror>`) o enlaces `javascript:`.

### Decisión

Renderizar en el servidor con **commonmark-java** (más la extensión de tablas GFM) usando:

- `escapeHtml(true)`: el HTML crudo se muestra como texto.
- `sanitizeUrls(true)`: neutraliza `javascript:` y esquemas peligrosos.
- Un visitor que reescribe las imágenes relativas hacia `/api/lecciones/{id}/recursos/{archivo}`.
- Enlaces externos con `target="_blank"` y `rel="noopener noreferrer"`, para no perder lo escrito en el editor.

La API entrega `html` listo. Como segunda barrera, la página envía una Content-Security-Policy que solo permite scripts, estilos e imágenes del propio servidor.

### Alternativas consideradas

| Opción | Seguridad | Dependencias en el navegador | Comentario |
|---|---|---|---|
| **commonmark en el servidor (elegida)** | Un solo punto que sanitiza, con pruebas | Ninguna | La API ya entrega el resultado final |
| marked + DOMPurify en el navegador | Depende de dos librerías en el cliente | Dos librerías JS | Contradice "sin frameworks ni CDN" y hay que empaquetarlas |
| flexmark-java | Similar | Ninguna | Más pesada y con más opciones de las necesarias |

### Consecuencias

- Las lecciones no pueden usar HTML incrustado. Es una limitación aceptada: el markdown alcanza para texto, código, tablas e imágenes.
- La sanitización se prueba en `MarkdownRendererTest`.
- El frontend solo agrega presentación: numera los bloques de código Java y da a los bloques sin lenguaje el aspecto de la consola.

---

## ADR-004: Imágenes servidas desde memoria con lista blanca

**Estado:** Aceptada · **Fecha:** 2026-10-03

### Contexto

Las imágenes de una lección se sirven por `GET /api/lecciones/{id}/recursos/{archivo}`. Un parámetro de ruta que nombra un archivo es el caso clásico de path traversal (`../../config.txt`).

### Decisión

- Las imágenes se leen una sola vez al cargar la lección y quedan **en memoria**. La petición busca el nombre en un mapa; nunca se arma una ruta del sistema de archivos con datos del usuario.
- Los nombres se validan con `[A-Za-z0-9][A-Za-z0-9._-]*` y no pueden contener `..`.
- Solo se aceptan png, jpg, jpeg, gif y webp. **No se acepta svg**, porque puede contener scripts y se serviría desde el mismo dominio.
- Se verifica la firma de bytes de cada imagen al cargarla: un `.png` que no es PNG invalida la lección.
- Las respuestas llevan el `Content-Type` correcto, `X-Content-Type-Options: nosniff` y caché de 1 hora.

### Consecuencias

- `config.txt` y `leccion.md` nunca se pueden descargar por esta ruta (responde 400).
- Las lecciones no admiten subcarpetas de imágenes; el formato del enunciado tampoco las define.
- Lo cubren pruebas unitarias, de API y de humo contra el servidor real con rutas codificadas (`..%2F`, `..%5C`).

---

## ADR-005: Frontend con HTML, CSS y JavaScript sin frameworks ni build

**Estado:** Aceptada · **Fecha:** 2026-10-03

### Contexto

La interfaz tiene una sola pantalla con menús, diálogos, un editor y una consola. El enunciado menciona HTML, CSS y JavaScript. La app debe funcionar sin internet (sin CDNs) y desplegarse junto con el servicio.

### Decisión

HTML5, CSS3 con custom properties y JavaScript con **ES modules** nativos, servidos desde `src/main/resources/static`. No hay frameworks, npm ni build step.

- Módulos pequeños: `api`, `estado`, `textos` y `ui/` (`menu`, `dialogos`, `editor`, `consola`, `leccion`, `configuracion`, `fase2`).
- Diálogos con `<dialog>` nativo: atrapan el foco y cierran con Esc sin código extra.
- Temas con tokens CSS en `:root` y el atributo `data-theme`.
- El tamaño de letra depende de la variable `--fs`, y todo el texto se mide en `rem`.
- Las fuentes se empaquetan en woff2 con licencia OFL.

### Alternativas consideradas

| Opción | Complejidad | Peso | Comentario |
|---|---|---|---|
| **JS sin frameworks (elegida)** | Baja | ~30 KB de JS sin minificar | Suficiente para una pantalla; nada que compilar |
| React o Vue con Vite | Media | Más dependencias y un build | Más código del necesario y un paso extra en Docker y CI |
| Thymeleaf (renderizado en servidor) | Baja | — | Menos adecuado para editor y consola interactivos; mezcla el frontend con el servicio que se separará en la fase 2 |

### Consecuencias

- Cualquier persona puede leer y modificar el frontend sin herramientas.
- No hay pruebas automáticas del frontend en la fase 1; se usa la lista de verificación manual del [plan de pruebas](plan-pruebas.md) con capturas en escritorio y móvil.
- A revisar en la fase 2: si el panel de historial y los módulos de administración hacen crecer mucho la interfaz, evaluar componentes web nativos antes que un framework.

---

## ADR-006: Configuración del estudiante solo en memoria

**Estado:** Aceptada para la fase 1 · **Fecha:** 2026-10-03

### Contexto

El enunciado indica que, en la fase 1, las opciones de Configuración funcionan pero la información solo se guarda en memoria.

### Decisión

El tamaño de letra (12 a 24 px, en pasos de 2) y el tema viven en variables del módulo `estado.js`. No se usan `localStorage`, `sessionStorage` ni cookies.

- Al recargar, el tamaño vuelve a 16 px y el tema al que prefiera el sistema operativo (`prefers-color-scheme`). Leer esa preferencia no guarda nada.
- La nota "Estos ajustes duran mientras tengas la página abierta" lo explica en la propia interfaz.

### Consecuencias

- Cumple el enunciado y no requiere aviso de cookies.
- El estudiante debe volver a elegir su tamaño en cada visita.
- Fase 2: decidir si las preferencias se guardan por navegador (`localStorage`, sin datos personales) o en el servidor. El estudiante no se autentica, así que no hay a quién asociarlas en la base de datos.

---

## ADR-007: Editor de código sin ayudas

**Estado:** Aceptada · **Fecha:** 2026-10-03

### Contexto

El objetivo pedagógico es que el estudiante escriba el código y enfrente sus propios errores. Editores como CodeMirror o Monaco traen autocompletado, cierre automático de llaves y resaltado de errores, justo lo que el ejercicio quiere evitar.

### Decisión

Un `<textarea>` con `autocomplete`, `autocorrect`, `autocapitalize` y `spellcheck` desactivados, `wrap="off"` y fuente monoespaciada (Atkinson Hyperlegible Mono, diseñada para distinguir 0/O y 1/l/I). Solo agrega lo necesario para escribir código a mano:

- **Tab** inserta 4 espacios y **Shift+Tab** los quita.
- Números de línea y renglón actual resaltado, para ubicar la línea que mencionará el compilador.
- **Esc y luego Tab** sacan el foco del editor, para no crear una trampa de teclado (WCAG 2.1.2).

No hay sangría automática, cierre de llaves, autocompletado ni resaltado de sintaxis.

### Alternativas consideradas

| Opción | Ayudas | Peso | Comentario |
|---|---|---|---|
| **textarea (elegida)** | Ninguna | 0 KB extra | Control total de lo que se ofrece |
| CodeMirror 6 con extensiones mínimas | Se pueden desactivar | ~150 KB | Hay que empaquetarlo y vigilar cada extensión |
| Monaco | Muchas | > 2 MB | Desproporcionado y orientado a IDE |

### Consecuencias

- El editor no colorea el código, a propósito. La legibilidad viene de la tipografía y de los renglones.
- En la fase 2 el contenido del textarea se envía tal cual al servicio de ejecución.

---

## ADR-008: Render como plataforma de nube, con Docker

**Estado:** Aceptada para la fase 1 · **Fecha:** 2026-10-03

### Contexto

Se necesita una URL pública estable, con presupuesto cero, desplegada desde GitHub, que admita un servicio Java con health check. La fase 2 sumará MariaDB y varios servicios.

### Decisión

**Render**, plan gratuito, servicio web Docker definido en `render.yaml` (blueprint):

- Imagen multi-stage: Maven para compilar y `eclipse-temurin:21-jre` para ejecutar, con usuario sin privilegios.
- Puerto tomado de `PORT` y health check en `/actuator/health`.
- Despliegue automático solo cuando pasa GitHub Actions (`autoDeployTrigger: checksPass`).
- JVM ajustada al límite de 512 MB.

### Alternativas consideradas

| Opción | Costo | Complejidad | Comentario |
|---|---|---|---|
| **Render (elegida)** | Gratis | Baja: blueprint en el repo | Se duerme tras 15 min sin tráfico (primer acceso ≈1 min) |
| Railway | Solo crédito de prueba | Baja | No hay plan gratuito permanente para mantener la URL hasta la fase 2 |
| Azure Container Apps | Crédito de Azure for Students | Media-alta | Más cercano a la fase 2 (ingress, escalado), pero más pasos y consume crédito |

### Consecuencias

- La configuración vive en el repositorio y se puede reproducir.
- El servicio no es portable a otra nube sin cambios en `render.yaml`, aunque la imagen Docker sí lo es.
- El arranque en frío del plan gratuito se compensa abriendo la URL antes de la presentación (ver [DEPLOY.md](../DEPLOY.md)).
- Fase 2: Render no ofrece MariaDB gestionada. Las opciones son MariaDB en un servicio privado de Render con disco persistente (plan de pago) o un proveedor externo de MariaDB gestionada. Ver el [diagrama de despliegue](diagrama-despliegue.md).

---

## Decisiones funcionales

| Decisión | Justificación |
|---|---|
| La consola va en la columna derecha, debajo de los botones | Así está en el mock de la página 4, aunque el texto la describa como franja inferior. En móvil queda al final, como franja. |
| Barra "Lección activa" con la etiqueta en minúsculas | Misma posición y peso que en el mock. Además muestra el objetivo de la lección en palabras ("Objetivo: Ejecutar sin errores"). |
| Cerrar lección pide confirmación solo si el editor tiene texto | Evita perder lo escrito sin molestar cuando no hay nada que perder. La opción enfocada por defecto es "Seguir escribiendo". |
| Abrir otra lección con texto en el editor también pide confirmación | Abrir una lección limpia el editor; es la misma pérdida que al cerrar. |
| El editor empieza vacío y queda en solo lectura sin lección abierta | La práctica consiste en escribir el código a mano; no se precarga nada. |
| Compilar, Ejecutar e Historial usan `aria-disabled` en vez de `disabled` | Siguen siendo enfocables: el aviso "Disponible en fase 2" aparece con mouse, teclado y toque, y al pulsarlos la consola explica por qué no hacen nada. |
| Ayuda en un diálogo con pasos, objetivos, ejemplos de consola y atajos | El enunciado deja la Ayuda a criterio. Se explica lo que el estudiante necesita para usar la app sin preguntar, incluidos los 4 tipos de objetivo. |
| La consola ya distingue éxito, error de compilación y error de ejecución | Cada tipo tiene ícono, nombre y borde propios (no solo color), así la fase 2 solo conecta los resultados. |
| Los bloques de código de la lección se numeran igual que el editor | El estudiante copia línea por línea, y la numeración coincide con la que usará el compilador. |

## Decisiones no funcionales

| Área | Decisión | Justificación |
|---|---|---|
| Seguridad | CSP `default-src 'self'`, `nosniff`, `X-Frame-Options: DENY` y `Referrer-Policy` | Segunda barrera contra XSS y clickjacking. En la fase 2 pueden moverse al reverse proxy. |
| Seguridad | Actuator expone solo `health` y sin detalles | No filtra configuración ni variables de entorno. |
| Seguridad | Opciones administrativas bajo `/api/admin/**`, apagadas por defecto (`APP_ADMIN_PREVIEW_ENABLED`) | En la fase 2 basta una regla de Spring Security para ese prefijo. |
| Errores | `@RestControllerAdvice` con un JSON uniforme `{timestamp, estado, error, mensaje, ruta}` y sin trazas | El frontend muestra `mensaje` tal cual y no se exponen detalles internos. |
| Accesibilidad | Contraste WCAG AA en ambos temas (calculado), foco visible, diálogos nativos, áreas táctiles de 44 px, `prefers-reduced-motion` | Estudiantes en computadora, tablet y móvil, con distintas necesidades visuales. |
| Tipografía | Atkinson Hyperlegible Next y Mono empaquetadas (OFL) | Legibilidad y distinción de caracteres parecidos; sin dependencias externas. |
| Funcionamiento sin internet | Sin CDNs: fuentes, íconos (sprite SVG en línea) y scripts salen del propio servidor | Requisito de uso offline y coherente con la CSP. |
| Rendimiento | Lecciones en memoria, compresión HTTP y caché de imágenes de 1 hora | Respuestas en milisegundos; el cuello de botella es el arranque en frío del plan gratuito. |
| Operación | Health check, logs que explican por qué se omitió una lección, imagen sin privilegios de root | Diagnóstico rápido en la nube y menor superficie de ataque. |
| Calidad | GitHub Actions corre `mvn verify`, construye la imagen y la prueba en cada push | Render solo despliega lo que pasó el CI. |
