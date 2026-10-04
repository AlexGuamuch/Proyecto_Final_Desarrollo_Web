# Checklist de despliegue — lecciones-service, fase 1

**Fecha:** 2026-10-03 | **Destino:** Render, plan gratuito, blueprint `render.yaml` | **Tipo:** primer despliegue

No hay base de datos, migraciones ni usuarios existentes: el riesgo principal es que el servicio no arranque o que el frontend no cargue en la URL pública. No hay ambiente de staging; su lugar lo ocupa el job de CI que construye la imagen Docker, la ejecuta y la prueba con curl.

## Antes de desplegar

- [x] `mvnw verify` local: 118 pruebas en verde (unitarias, `@WebMvcTest` y humo con la app completa).
- [x] El jar arranca (`Started ... in 2.3 s`) y carga `4 de 4` lecciones.
- [x] Endpoints probados con curl: health `UP`, lista, detalle, imagen, 404 JSON, traversal rechazado (400), CSP presente, `/actuator/env` cerrado (404).
- [x] Importar, exportar y eliminar lecciones probados en la interfaz (zip de ejemplo, zip inválido, eliminación con confirmación).
- [x] Interfaz revisada en navegador: escritorio y móvil, temas claro y oscuro; flujos de abrir, cerrar (con y sin confirmación), tamaño de letra y ayuda.
- [x] Puerto desde `PORT` (`server.port=${PORT:8080}`); health check en `/actuator/health`.
- [x] Imagen con usuario sin privilegios (uid 10001) y memoria ajustada a 512 MB (`JAVA_TOOL_OPTIONS`).
- [x] Sin secretos en el repositorio (no hay credenciales en la fase 1).
- [x] Importar, exportar y eliminar activos (`APP_ADMIN_PREVIEW_ENABLED=true`), con límites de tamaño y de memoria porque no tienen autenticación hasta la fase 2.
- [x] Plan de rollback documentado en [DEPLOY.md](../DEPLOY.md#volver-a-una-versión-anterior).
- [ ] `docker build` y `docker run` locales: **pendiente de instalar Docker Desktop**. Mientras tanto lo cubre el job `imagen` del CI.
- [ ] CI en verde en GitHub (compilar y probar + imagen Docker).
- [x] Revisión de código final (`engineering:code-review`): 2 ajustes menores del frontend corregidos.

## Despliegue

- [ ] Push de `main` al repositorio de GitHub.
- [ ] Render → New → Blueprint → elegir el repositorio → Apply.
- [ ] En los logs de Render: `Lecciones cargadas: 4 de 4` y `Tomcat started on port`.
- [ ] Render marca el servicio como **Live** (health check aprobado).
- [ ] `./scripts/verificar-despliegue.sh <URL>` sin fallos.
- [ ] Flujos clave en el navegador con la URL pública, desde computadora y desde un teléfono:
  - Abrir lección: lista las 4, carga contenido, imagen y título.
  - Cerrar lección: limpia panel, editor y título.
  - Tamaño de letra y tema cambian y vuelven al inicial al recargar.
  - Compilar, Ejecutar e Historial muestran "Disponible en fase 2".
  - Ayuda abre y cierra con Esc.
  - Configuración › Administración: importar `docs/ejemplos/lecciones-extra.zip`, verla en Abrir lección, eliminarla y exportar.

## Después de desplegar

- [ ] Anotar la URL en `README.md` y en la carátula de `docs/documentacion-fase1.md`.
- [ ] Esperar 20 minutos sin tráfico y abrir la URL: confirmar que el servicio despierta (≈1 minuto) y funciona.
- [ ] Revisar los logs de Render: sin `ERROR` ni reinicios por memoria.

## Cuándo hacer rollback

- El health check falla y Render no pone la versión nueva en **Live** (Render mantiene la anterior; revisar logs antes de reintentar).
- `/api/lecciones` devuelve menos de 4 lecciones (una lección quedó inválida; el log dice cuál).
- La página carga sin estilos o sin JavaScript (por ejemplo, la CSP bloquea un recurso).
- Alguien abusa de importar/eliminar en la URL pública: poner `APP_ADMIN_PREVIEW_ENABLED=false` en Render (se redespliega y vuelven las 4 lecciones de ejemplo).
- Aparecen respuestas 5xx o `OutOfMemoryError` en los logs.

Rollback: servicio → Events/Deploys → despliegue anterior → **Rollback**.
