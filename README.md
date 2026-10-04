# Práctica de Java a mano — Fase 1

Aplicación web para que estudiantes de los primeros cursos de Ingeniería en Sistemas practiquen Java **escribiendo el código a mano**, sin autocompletado ni asistentes de IA. Cada lección corta muestra una explicación y un código modelo; el estudiante lo copia o edita en el editor y, en la fase 2, lo compila y ejecuta para enfrentarse a los errores reales del compilador.

Proyecto del curso Desarrollo Web, Universidad Mariano Gálvez de Guatemala. El enunciado está en [`docs/enunciado.pdf`](docs/enunciado.pdf).

**URL pública:** <https://lecciones-service.onrender.com> (plan gratuito de Render: si estuvo inactiva, la primera visita tarda alrededor de un minuto).

## Alcance de la fase 1

| Incluido | Para la fase 2 |
|---|---|
| Interfaz completa según los mocks (menú, lección activa, panel de lección, editor, botones, consola) | Compilar y Ejecutar con `guali-dev.jar` |
| Lecciones › Abrir lección y Cerrar lección | Historial de sesión persistente |
| Configuración: tamaño de letra (12–24 px) y tema claro/oscuro, solo en memoria | Base de datos MariaDB con JPA |
| Configuración › Administración: importar lecciones (zip), exportarlas y eliminar una, en memoria | Autenticación de administradores para esas opciones |
| Ayuda | Spring Security, administradores, estadísticas de visitas |
| API REST de lecciones y health check para la nube | Separación en microservicios detrás de un API Gateway |

Compilar, Ejecutar e Historial están visibles pero sin acción, con el aviso "Disponible en fase 2".

## Tecnologías

- Java 21, Spring Boot 3.5 (Web, Actuator), Maven (incluye Maven Wrapper).
- [commonmark-java](https://github.com/commonmark/commonmark-java) para renderizar el markdown en el servidor.
- Frontend: HTML5, CSS3 y JavaScript sin frameworks (ES modules, sin build step).
- Fuentes Atkinson Hyperlegible Next y Mono (licencia OFL), empaquetadas: la app no usa CDNs y funciona sin internet.
- Docker (imagen multi-stage, usuario sin privilegios) y Render para el despliegue.
- GitHub Actions: compila, prueba y construye la imagen en cada push.

## Estructura

```
├─ lecciones-service/                    servicio único de la fase 1
│  ├─ Dockerfile
│  └─ src/main/
│     ├─ java/gt/edu/umg/lecciones/
│     │  ├─ leccion/                     módulo de lecciones (futuro microservicio)
│     │  │  ├─ controller/               API REST
│     │  │  ├─ service/                  casos de uso y renderizado de markdown
│     │  │  ├─ repository/               LeccionRepository + implementación en memoria
│     │  │  ├─ model/  dto/              dominio y objetos de la API
│     │  │  └─ carga/                    lectura y validación de lecciones del classpath
│     │  └─ comun/                       errores centralizados y cabeceras de seguridad
│     └─ resources/
│        ├─ lecciones/                   una carpeta por lección
│        └─ static/                      frontend (index.html, css/, js/, fonts/)
├─ docs/                                 decisiones, diagramas y documentación
├─ scripts/                              empaquetado de la entrega y generación de imágenes
├─ render.yaml                           blueprint de Render
└─ .github/workflows/ci.yml              integración continua
```

## Correr localmente

Requisitos: **Java 21**. Maven no hace falta: el proyecto trae el wrapper.

```bash
cd lecciones-service
./mvnw spring-boot:run
```

En Windows (PowerShell o CMD) usa `mvnw.cmd` en lugar de `./mvnw`. Abre <http://localhost:8080>.

Si el puerto 8080 ya está ocupado (por ejemplo, por un Tomcat instalado como servicio), elige otro con la variable `PORT`:

```bash
PORT=8090 ./mvnw spring-boot:run
```

En PowerShell: `$env:PORT=8090; .\mvnw.cmd spring-boot:run`.

Las opciones de importar, exportar y eliminar lecciones están activas por defecto. Para ocultarlas, define `APP_ADMIN_PREVIEW_ENABLED=false`.

### Pruebas

```bash
cd lecciones-service
./mvnw verify
```

Corre las pruebas unitarias, las de la API (`@WebMvcTest`) y una prueba de humo que levanta la aplicación completa. El plan de pruebas está en [`docs/plan-pruebas.md`](docs/plan-pruebas.md).

### Jar ejecutable

```bash
cd lecciones-service
./mvnw package
java -jar target/lecciones-service.jar
```

## Correr con Docker

```bash
docker build -t lecciones-service lecciones-service
docker run --rm -p 8080:8080 lecciones-service
```

Comprueba el estado con `curl http://localhost:8080/actuator/health`.

## Desplegar

Los pasos exactos para Render están en **[DEPLOY.md](DEPLOY.md)**. En resumen: sube el repositorio a GitHub, crea un Blueprint en Render apuntando al repositorio y Render construye la imagen con el `Dockerfile`, revisa `/actuator/health` y publica la URL.

## API

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/lecciones` | `[{id, titulo, objetivo}]` ordenado por id |
| GET | `/api/lecciones/{id}` | `{id, titulo, objetivo, html}` |
| GET | `/api/lecciones/{id}/recursos/{archivo}` | La imagen (png, jpg, jpeg, gif o webp) |
| GET | `/actuator/health` | `{"status":"UP"}` |
| GET | `/api/admin/estado` | `{habilitado: true}` si las opciones de administración están activas |
| POST | `/api/admin/lecciones/importar` | Zip en el campo multipart `archivo` → `{importadas, omitidas, avisos}` |
| DELETE | `/api/admin/lecciones/{id}` | `204`; `404` si no existe |
| GET | `/api/admin/lecciones/exportar` | `lecciones.zip` con todas las lecciones |

Las rutas `/api/admin/**` no piden autenticación en la fase 1; en la fase 2 se protegerán con Spring Security.

Los errores responden siempre con el mismo JSON:

```json
{ "timestamp": "2026-10-03T17:00:00Z", "estado": 404, "error": "Not Found",
  "mensaje": "No existe la lección leccion-99", "ruta": "/api/lecciones/leccion-99" }
```

## Agregar una lección

Crea una carpeta dentro de `lecciones-service/src/main/resources/lecciones/`. El nombre de la carpeta es el id y el título (solo letras sin tildes, dígitos, `-`, `_` y `.`):

```
leccion-05-condicionales/
├─ leccion.md      explicación en markdown (UTF-8)
├─ config.txt      exactamente una línea: objetivo=VALOR
└─ diagrama.png    imágenes opcionales (png, jpg, jpeg, gif, webp)
```

Valores de `objetivo`: `COMPILAR_CON_ERROR`, `COMPILAR_EXITOSO`, `EJECUTAR_CON_ERROR`, `EJECUTAR_EXITOSO`.

En el markdown, las imágenes se referencian con ruta relativa (`![Diagrama](diagrama.png)`); el servidor las reescribe hacia `/api/lecciones/{id}/recursos/diagrama.png`. Los bloques de código con lenguaje (` ```java `) se muestran numerados como en el editor, y los bloques sin lenguaje se muestran como salida de consola. El HTML escrito dentro del markdown se muestra como texto (no se ejecuta).

Si una lección no cumple el formato, el servicio la omite y lo registra en el log al arrancar (`Lección omitida '...': motivo`).

### Importar lecciones desde la interfaz

En **Configuración › Administración › Importar lecciones (.zip)** se sube un zip con una carpeta por lección, con o sin la carpeta raíz `lecciones/` (el mismo formato que genera **Exportar lecciones**). Hay un ejemplo listo en [`docs/ejemplos/lecciones-extra.zip`](docs/ejemplos/lecciones-extra.zip).

- Cada lección se valida con las mismas reglas de arriba; las inválidas se omiten y la consola dice por qué.
- Una lección con el mismo nombre que otra existente la reemplaza.
- Límites: zip de 10 MB, 500 entradas, 50 MB descomprimido, 5 MB por archivo; en memoria caben 100 lecciones y 64 MB.
- Se rechazan rutas que intentan salir de su carpeta (`../`, rutas absolutas) y se ignoran `__MACOSX/` y los archivos ocultos.
- Lo importado vive en memoria: se pierde al reiniciar el servidor (las 4 lecciones de ejemplo vuelven a cargarse).

## Documentación

- [Decisiones de diseño (ADRs)](docs/decisiones.md)
- [Diagrama entidad-relación (modelo de la fase 2)](docs/diagrama-er.md)
- [Diagrama de despliegue (fase 1 y fase 2)](docs/diagrama-despliegue.md)
- [Documento consolidado de la fase 1](docs/documentacion-fase1.md)
- [Plan de pruebas](docs/plan-pruebas.md)

## Entrega

```powershell
.\scripts\empaquetar.ps1 -Carne 0000-00-000
```

```bash
./scripts/empaquetar.sh 0000-00-000
```

Generan `proyecto-fase-1-<carné>.zip` en la raíz, sin `target/`, `.git/`, `.idea/` ni `node_modules/`.
