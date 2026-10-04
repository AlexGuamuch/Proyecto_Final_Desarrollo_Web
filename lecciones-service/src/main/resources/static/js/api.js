// Cliente de la API REST. Las rutas son absolutas desde la raíz del sitio, así siguen
// funcionando cuando en la fase 2 un API Gateway reparta /api/** entre microservicios.

async function pedir(ruta, opciones = {}) {
    let respuesta;
    try {
        respuesta = await fetch(ruta, { headers: { Accept: 'application/json' }, ...opciones });
    } catch {
        throw new Error('No hay conexión con el servidor. Revisa tu conexión y vuelve a intentarlo.');
    }
    if (!respuesta.ok) {
        let mensaje = `El servidor respondió con el código ${respuesta.status}.`;
        try {
            const cuerpo = await respuesta.json();
            if (cuerpo && typeof cuerpo.mensaje === 'string') {
                mensaje = cuerpo.mensaje;
            }
        } catch {
            // La respuesta de error no era JSON; se usa el mensaje genérico.
        }
        const error = new Error(mensaje);
        error.estado = respuesta.status;
        throw error;
    }
    return respuesta.status === 204 ? null : respuesta.json();
}

/** @returns {Promise<Array<{id: string, titulo: string, objetivo: string}>>} */
export function listarLecciones() {
    return pedir('/api/lecciones');
}

/** @returns {Promise<{id: string, titulo: string, objetivo: string, html: string}>} */
export function obtenerLeccion(id) {
    return pedir(`/api/lecciones/${encodeURIComponent(id)}`);
}

// ---- Configuración › Administración: importar, eliminar y exportar lecciones ----

/** Devuelve true si el servidor tiene activas las opciones administrativas. */
export async function adminDisponible() {
    try {
        await pedir('/api/admin/estado');
        return true;
    } catch {
        return false;
    }
}

export function importarZip(archivo) {
    const datos = new FormData();
    datos.append('archivo', archivo);
    return pedir('/api/admin/lecciones/importar', { method: 'POST', body: datos });
}

export function eliminarLeccion(id) {
    return pedir(`/api/admin/lecciones/${encodeURIComponent(id)}`, { method: 'DELETE' });
}
