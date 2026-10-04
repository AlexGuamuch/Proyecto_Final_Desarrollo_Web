import { adminDisponible, eliminarLeccion, importarZip, listarLecciones } from '../api.js';
import { textoObjetivo } from '../textos.js';
import { abrirDialogo, confirmar, prepararDialogo } from './dialogos.js';

// Opciones de Configuración que modifican las lecciones: importar un zip, exportar todas y
// eliminar una. Funcionan en memoria (fase 1); en la fase 2 pedirán usuario y contraseña.
// Si el servidor las tiene apagadas, la sección no se muestra.

const MAX_BYTES_ZIP = 10 * 1024 * 1024;

export async function prepararAdmin({ consola, leccionActiva }) {
    if (!(await adminDisponible())) {
        return;
    }
    const $ = (id) => document.getElementById(id);
    $('admin').hidden = false;

    const entradaZip = $('admin-zip');
    const dlgEliminar = prepararDialogo($('dlg-eliminar'));
    const dlgConfirmar = $('dlg-confirmar');

    // ---------- Importar ----------
    entradaZip.addEventListener('change', async () => {
        const archivo = entradaZip.files[0];
        entradaZip.value = '';
        if (!archivo) {
            return;
        }
        if (archivo.size > MAX_BYTES_ZIP) {
            consola.escribir('sistema', `No se importó ${archivo.name}: pesa más de 10 MB.`);
            return;
        }
        consola.escribir('sistema', `Importando ${archivo.name}…`);
        try {
            const resultado = await importarZip(archivo);
            consola.escribir('sistema', resumenImportacion(resultado));
        } catch (error) {
            consola.escribir('sistema', `No se importó ${archivo.name}. ${error.message}`);
        }
    });

    // ---------- Eliminar ----------
    async function mostrarListaEliminar() {
        const estado = $('eliminar-estado');
        const lista = $('lista-eliminar');
        estado.textContent = 'Cargando lecciones…';
        estado.hidden = false;
        lista.replaceChildren();
        abrirDialogo(dlgEliminar);
        try {
            const lecciones = await listarLecciones();
            if (lecciones.length === 0) {
                estado.textContent = 'No hay lecciones para eliminar.';
                return;
            }
            estado.textContent = 'Elige la lección que quieres eliminar.';
            for (const leccion of lecciones) {
                const boton = document.createElement('button');
                boton.type = 'button';
                boton.className = 'lista-lecciones__item';
                const nombre = document.createElement('span');
                nombre.className = 'lista-lecciones__nombre';
                nombre.textContent = leccion.titulo;
                const objetivo = document.createElement('span');
                objetivo.className = 'lista-lecciones__objetivo';
                objetivo.textContent = `Objetivo: ${textoObjetivo(leccion.objetivo)}`;
                boton.append(nombre, objetivo);
                boton.addEventListener('click', () => eliminar(leccion));
                const item = document.createElement('li');
                item.append(boton);
                lista.append(item);
            }
        } catch (error) {
            estado.textContent = `No se pudo cargar la lista de lecciones. ${error.message}`;
        }
    }

    async function eliminar(leccion) {
        dlgEliminar.close();
        const continuar = await confirmar(dlgConfirmar, {
            titulo: `¿Eliminar ${leccion.titulo}?`,
            texto: 'Dejará de aparecer en Abrir lección. Si es una de las lecciones de ejemplo, volverá cuando se reinicie el servidor.',
            aceptar: 'Eliminar lección',
            cancelar: 'Cancelar',
        });
        if (!continuar) {
            return;
        }
        try {
            await eliminarLeccion(leccion.id);
            const activa = leccionActiva()?.id === leccion.id;
            consola.escribir('sistema', activa
                ? `Lección eliminada: ${leccion.titulo}. Sigue abierta en pantalla, pero ya no aparece en la lista.`
                : `Lección eliminada: ${leccion.titulo}.`);
        } catch (error) {
            consola.escribir('sistema', `No se pudo eliminar ${leccion.titulo}. ${error.message}`);
        }
    }

    $('admin-eliminar').addEventListener('click', mostrarListaEliminar);

    // ---------- Exportar ----------
    // El enlace descarga lecciones.zip directamente; solo se informa en la consola.
    $('admin-exportar').addEventListener('click', () => {
        consola.escribir('sistema', 'Descargando lecciones.zip con todas las lecciones.');
    });
}

function resumenImportacion({ importadas, omitidas, avisos }) {
    const partes = [];
    if (importadas.length > 0) {
        const nombres = importadas.map((l) => (l.reemplazada ? `${l.id} (reemplazada)` : l.id));
        partes.push(`Lecciones importadas (${importadas.length}): ${nombres.join(', ')}.`);
    } else {
        partes.push('No se importó ninguna lección.');
    }
    for (const omitida of omitidas) {
        partes.push(`Omitida ${omitida.carpeta}: ${omitida.motivo}.`);
    }
    for (const aviso of avisos) {
        partes.push(aviso);
    }
    return partes.join('\n');
}
