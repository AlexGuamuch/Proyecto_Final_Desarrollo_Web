#!/usr/bin/env bash
# Pruebas de humo contra una URL desplegada (o local).
#
# Uso:
#   ./scripts/verificar-despliegue.sh https://lecciones-service.onrender.com
#   ./scripts/verificar-despliegue.sh http://localhost:8080
#
# En el plan gratuito de Render el servicio puede estar dormido: el script espera hasta
# 3 minutos a que /actuator/health responda antes de empezar.
set -uo pipefail

base="${1:-}"
if [[ -z "$base" ]]; then
    echo "Uso: $0 <url-base>" >&2
    exit 1
fi
base="${base%/}"
fallos=0

revisar() {
    local descripcion="$1" esperado="$2" obtenido="$3"
    if [[ "$obtenido" == *"$esperado"* ]]; then
        echo "  OK    $descripcion"
    else
        echo "  FALLA $descripcion (esperado: $esperado; obtenido: ${obtenido:0:120})"
        fallos=$((fallos + 1))
    fi
}

codigo() {
    curl -s -o /dev/null -w "%{http_code}" "$@"
}

echo "Esperando a que $base responda..."
for _ in $(seq 1 36); do
    if curl -fsS --max-time 10 "$base/actuator/health" >/dev/null 2>&1; then
        break
    fi
    sleep 5
done

echo "Pruebas:"
revisar "health en UP" '"status":"UP"' "$(curl -s "$base/actuator/health")"
lista="$(curl -s "$base/api/lecciones")"
for id in leccion-01-hola-mundo leccion-02-errores-compilacion leccion-03-variables-y-tipos leccion-04-errores-ejecucion; do
    revisar "lista incluye $id" "$id" "$lista"
done
revisar "detalle con html" '"html":"<h1>Hola mundo</h1>' "$(curl -s "$base/api/lecciones/leccion-01-hola-mundo")"
revisar "imagen de la lección 1" "200 image/png" "$(curl -s -o /dev/null -w '%{http_code} %{content_type}' "$base/api/lecciones/leccion-01-hola-mundo/recursos/ciclo.png")"
revisar "lección inexistente da 404" "404" "$(codigo "$base/api/lecciones/no-existe")"
revisar "config.txt no se sirve" "400" "$(codigo "$base/api/lecciones/leccion-01-hola-mundo/recursos/config.txt")"
revisar "traversal codificado rechazado" "400" "$(codigo "$base/api/lecciones/leccion-01-hola-mundo/recursos/..%2F..%2Fconfig.txt")"
revisar "página principal" "200" "$(codigo "$base/")"
revisar "hoja de estilos" "200" "$(codigo "$base/css/app.css")"
revisar "JavaScript" "200" "$(codigo "$base/js/main.js")"
revisar "fuente del editor" "200" "$(codigo "$base/fonts/atkinson-hyperlegible-mono-latin-wght-normal.woff2")"
revisar "CSP presente" "default-src 'self'" "$(curl -sI "$base/" | tr -d '\r')"
revisar "actuator solo expone health" "404" "$(codigo "$base/actuator/env")"
revisar "admin apagado" "404" "$(codigo "$base/api/admin/estado")"

if [[ $fallos -eq 0 ]]; then
    echo "Todo bien."
else
    echo "$fallos prueba(s) fallaron."
    exit 1
fi
