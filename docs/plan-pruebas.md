# Plan de pruebas — Fase 1

Estrategia en pirámide: muchas pruebas unitarias rápidas sobre las reglas de negocio y de seguridad, algunas de integración sobre la capa HTTP y una prueba de humo con la aplicación completa. El frontend (JS sin build step) se verifica con una lista de comprobación manual y capturas en escritorio y móvil.

## Qué se prueba y cómo

| Área | Tipo | Por qué importa | Casos |
|---|---|---|---|
| `ConfigTxtParser` | Unitaria | Una lección mal configurada no debe tumbar la app ni cargarse con un objetivo equivocado | Los 4 objetivos válidos; BOM UTF-8; CRLF; espacios alrededor de `=`; líneas en blanco extra; archivo vacío; dos líneas; valor desconocido; valor en minúsculas; clave distinta; texto extra después del valor |
| `ValidadorLeccion` | Unitaria | Mismas reglas para el classpath y para el zip importado | Lección mínima válida; con imagen; falta `leccion.md`; falta `config.txt`; id con `..`, espacios o tildes; markdown > 512 KB; markdown que no es UTF-8; imagen con firma falsa; `.svg` y archivos ocultos ignorados |
| `MarkdownRenderer` | Unitaria | Es la frontera contra XSS y la que hace visibles las imágenes | `foto.png` y `./foto.png` reescritas a `/api/lecciones/{id}/recursos/...`; URL absoluta y ruta `/...` intactas; `../x.png` no se reescribe; `<script>` escapado; `javascript:` neutralizado; enlaces externos con `target` y `rel`; tablas GFM |
| `LeccionService` | Unitaria | Path traversal y respuestas 400/404 correctas | `../config.txt`, `..`, `a/b.png`, `a\b.png`, `config.txt`, `leccion.md`, `x.svg` → 400; imagen inexistente → 404; lección inexistente o id inválido → 404 |
| `CargadorLecciones` | Integración (classpath real) | La carga funciona igual desde carpetas y desde el jar | Carga las 4 lecciones de ejemplo con su objetivo; omite fixtures inválidos de `lecciones-prueba/` sin lanzar excepción |
| `LeccionController` + `GlobalExceptionHandler` + `CabecerasSeguridadFilter` | `@WebMvcTest` | Contrato de la API que consume el frontend | Lista con `{id, titulo, objetivo}`; detalle con `html`; 404 con el JSON de error; imagen con `Content-Type` y `Cache-Control`; 400 para svg y traversal; cabeceras CSP y `nosniff` |
| Aplicación completa | `@SpringBootTest` (humo) | Lo que revisa la nube al desplegar | `/actuator/health` responde `UP`; `/api/lecciones` devuelve 4; `index.html` se sirve; rutas codificadas (`..%2F`) rechazadas por el servidor real |
| Importar, exportar y eliminar | Unitaria + `@WebMvcTest` | Un zip malicioso no debe salir de su carpeta ni agotar memoria o CPU | Zip slip (`../`, `..\`, rutas absolutas y con unidad), límite de entradas y de tamaño descomprimido (también en entradas ignoradas), archivo de más de 5 MB, `__MACOSX` y ocultos ignorados, con y sin carpeta raíz `lecciones/`, barras de Windows, lección inválida reportada como omitida, reemplazo por id, tope de lecciones y bytes en memoria, exportar → importar simétrico, 404 cuando están apagadas |

## Qué no se prueba (y por qué)

- Getters de records y el repositorio en memoria por separado: son triviales y quedan cubiertos por las pruebas de servicio y de API.
- Configuración de Spring y de Actuator: es código del framework; la prueba de humo confirma el resultado.
- El script que genera la imagen: es de un solo uso.

## Frontend (manual, antes de cada entrega)

1. Abrir lección: lista las 4 con su objetivo; carga título, contenido e imagen.
2. Cerrar lección: con editor vacío cierra directo; con texto pide confirmación; limpia panel, editor y título.
3. A− / A+: cambia texto de menú, lección, editor y consola entre 12 y 24 px; al recargar vuelve a 16 (no se persiste).
4. Tema claro/oscuro: cambia todo; al recargar vuelve al del sistema (no se persiste).
5. Compilar, Ejecutar e Historial: visibles, sin acción, con aviso "Disponible en fase 2" al pasar el mouse, al enfocar con teclado y al tocar.
6. Editor: Tab inserta 4 espacios; Esc y luego Tab sale del editor; sin autocorrector ni corrector ortográfico.
7. Ayuda: abre y cierra con Esc.
8. Configuración › Administración: importar `docs/ejemplos/lecciones-extra.zip` (aparece la lección 5), importar un archivo que no es zip (mensaje claro), eliminar una lección (pide confirmación) y exportar (descarga `lecciones.zip`).
9. Capturas: escritorio (1440 px), tablet (768 px) y móvil (375 px), en ambos temas.

## Meta de cobertura

Sin herramienta de cobertura en la fase 1: la meta es que cada regla de validación y cada rama de seguridad tenga al menos un caso. En la fase 2 se puede agregar JaCoCo al CI.
