# Despliegue en Render

La fase 1 se despliega como un solo servicio web Docker en el plan gratuito de Render. Todo lo necesario está en [`render.yaml`](render.yaml) (blueprint) y [`lecciones-service/Dockerfile`](lecciones-service/Dockerfile).

## Qué vas a obtener

- Una URL pública del tipo `https://lecciones-service.onrender.com` (si el nombre ya está tomado, Render agrega un sufijo).
- Health check en `/actuator/health`: Render no envía tráfico a una versión nueva hasta que responde `UP`.
- Despliegue automático cada vez que haces push a `main` **y** el workflow de GitHub Actions pasa (`autoDeployTrigger: checksPass`).

## Requisitos

- Cuenta de GitHub con el proyecto subido a un repositorio (puede ser privado).
- Cuenta de Render (puedes entrar con tu cuenta de GitHub en <https://dashboard.render.com>).

## Paso 1. Subir el proyecto a GitHub

1. En GitHub crea un repositorio **vacío** (sin README ni .gitignore), por ejemplo `proyecto-desarrollo-web`.
2. Desde la raíz del proyecto:

   ```bash
   git remote add origin https://github.com/<tu-usuario>/proyecto-desarrollo-web.git
   git push -u origin main
   ```

3. En la pestaña **Actions** del repositorio revisa que el workflow **CI** termine en verde (compila, corre las pruebas, construye la imagen Docker y la prueba).

## Paso 2. Crear el servicio desde el blueprint

1. En Render: **New +** → **Blueprint**.
2. Conecta tu cuenta de GitHub si te lo pide y elige el repositorio.
3. Render lee `render.yaml` y muestra el servicio `lecciones-service` (Docker, plan Free, región Ohio). Pulsa **Apply** / **Deploy Blueprint**.
4. Espera el primer build (5 a 10 minutos: descarga dependencias de Maven y construye la imagen). En **Logs** debe aparecer:

   ```
   Lecciones cargadas: 4 de 4 carpetas encontradas en classpath*:lecciones
   Tomcat started on port 10000 (http)
   ```

   Render define la variable `PORT` (normalmente 10000); la aplicación la toma con `server.port=${PORT:8080}`.

## Paso 3. Verificar

Reemplaza la URL por la tuya:

```bash
curl https://lecciones-service.onrender.com/actuator/health
```

Debe responder `{"status":"UP","groups":["liveness","readiness"]}`. Luego:

```bash
curl https://lecciones-service.onrender.com/api/lecciones
```

Debe listar las 4 lecciones. Abre la URL en el navegador, prueba **Lecciones › Abrir lección**, cambia el tema en **Configuración** e importa `docs/ejemplos/lecciones-extra.zip` desde **Configuración › Administración**: debe aparecer `leccion-05-condicionales` en Abrir lección.

También puedes correr todas las comprobaciones de una vez:

```bash
./scripts/verificar-despliegue.sh https://lecciones-service.onrender.com
```

Anota la URL en la carátula de `docs/documentacion-fase1.md`.

## Variables de entorno

| Variable | Valor en Render | Para qué sirve |
|---|---|---|
| `PORT` | la define Render | Puerto HTTP del servicio |
| `APP_ADMIN_PREVIEW_ENABLED` | `true` | Muestra en Configuración las opciones de importar, exportar y eliminar lecciones, que la fase 1 pide funcionando en memoria. No tienen autenticación hasta la fase 2; con `false` se ocultan y `/api/admin/**` responde 404. |
| `JAVA_TOOL_OPTIONS` | (definida en el Dockerfile) | Ajustes de memoria para los 512 MB del plan gratuito |

Para cambiar una variable: servicio → **Environment** → editar → **Save Changes** (Render vuelve a desplegar).

## Antes de presentar

El plan gratuito **apaga el servicio tras 15 minutos sin tráfico**. La primera visita después tarda alrededor de un minuto en responder mientras arranca. Abre la URL uno o dos minutos antes de mostrarla.

## Volver a una versión anterior

Servicio → **Events** (o **Deploys**) → elige un despliegue anterior que funcionaba → **Rollback**. Como la fase 1 no tiene base de datos, el rollback es inmediato y no hay datos que migrar.

## Problemas frecuentes

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| El despliegue no arranca tras un push | El workflow de GitHub Actions falló (`checksPass`) | Revisa la pestaña Actions, corrige y vuelve a hacer push |
| `Health check failed` | La app tardó demasiado en arrancar o falló al iniciar | Revisa **Logs**; busca `APPLICATION FAILED TO START` |
| `Lecciones cargadas: 3 de 4` en los logs | Una lección tiene un `config.txt` o `leccion.md` inválido | El log indica cuál y por qué (`Lección omitida '...'`) |
| La página carga pero sin estilos | Caché del navegador con una versión anterior | Recarga con Ctrl+F5 |
| Error de memoria (`OutOfMemoryError`, reinicios) | Límite de 512 MB del plan gratuito | Revisa que `JAVA_TOOL_OPTIONS` siga en el Dockerfile |

## Probar la imagen localmente (opcional, requiere Docker)

```bash
docker build -t lecciones-service lecciones-service
docker run --rm -p 8080:8080 lecciones-service
```

Si el puerto 8080 está ocupado en tu máquina, usa otro del lado del host: `-p 8090:8080` y abre <http://localhost:8090>.
