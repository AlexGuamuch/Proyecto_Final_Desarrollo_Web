#!/usr/bin/env bash
# Genera el zip de entrega: proyecto-fase-<fase>-<carné>.zip en la raíz del proyecto.
#
# Uso:
#   ./scripts/empaquetar.sh 0000-00-000        # fase 1
#   ./scripts/empaquetar.sh 0000-00-000 2      # fase 2
#
# Excluye target/, .git/, .idea/ y node_modules/ en cualquier nivel, además de Fonts/
# (descarga original de las fuentes), .claude/ y zips de entregas anteriores.
# Usa `zip` si está instalado; si no, Python 3.
set -euo pipefail

carne="${1:-}"
fase="${2:-1}"

if [[ ! "$carne" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{3,6}$ ]]; then
    echo "Uso: $0 <carné> [fase]   (ejemplo: $0 0000-00-000)" >&2
    exit 1
fi
if [[ "$fase" != "1" && "$fase" != "2" ]]; then
    echo "La fase debe ser 1 o 2" >&2
    exit 1
fi

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
nombre="proyecto-fase-${fase}-${carne}"
destino="${raiz}/${nombre}.zip"
rm -f "$destino"

# Copia temporal con el nombre de la entrega, para que el zip contenga esa carpeta raíz.
temporal="$(mktemp -d)"
trap 'rm -rf "$temporal"' EXIT

if command -v zip >/dev/null 2>&1; then
    ln -s "$raiz" "${temporal}/${nombre}"
    (
        cd "$temporal"
        zip -qr "$destino" "$nombre" \
            -x "*/target/*" "*/.git/*" "*/.idea/*" "*/node_modules/*" \
               "${nombre}/Fonts/*" "${nombre}/.claude/*" "${nombre}/proyecto-fase-*.zip"
    )
else
    # En Windows, "python3" puede ser un acceso directo de Microsoft Store que no ejecuta nada:
    # se prueba cada candidato antes de usarlo.
    python_cmd=""
    for candidato in python3 python py; do
        if command -v "$candidato" >/dev/null 2>&1 && "$candidato" -c 'import zipfile' >/dev/null 2>&1; then
            python_cmd="$candidato"
            break
        fi
    done
    if [[ -z "$python_cmd" ]]; then
        echo "Se necesita 'zip' o Python 3 para empaquetar" >&2
        exit 1
    fi
    "$python_cmd" - "$raiz" "$destino" "$nombre" <<'PY'
import os, sys, zipfile
raiz, destino, nombre = sys.argv[1:4]
cualquier_nivel = {"target", ".git", ".idea", "node_modules"}
en_raiz = {"Fonts", ".claude"}
with zipfile.ZipFile(destino, "w", zipfile.ZIP_DEFLATED) as zf:
    for actual, carpetas, archivos in os.walk(raiz):
        relativa = os.path.relpath(actual, raiz)
        nivel_raiz = relativa == "."
        carpetas[:] = [c for c in carpetas
                       if c not in cualquier_nivel and not (nivel_raiz and c in en_raiz)]
        for archivo in archivos:
            if nivel_raiz and archivo.startswith("proyecto-fase-") and archivo.endswith(".zip"):
                continue
            ruta = os.path.join(actual, archivo)
            interna = os.path.normpath(os.path.join(nombre, relativa, archivo)).replace(os.sep, "/")
            zf.write(ruta, interna)
PY
fi

echo "Listo: ${destino}"
du -h "$destino" | cut -f1 | xargs echo "Tamaño:"
echo "Antes de subirlo, descomprímelo en otra carpeta y verifica que compile (./mvnw verify)."
