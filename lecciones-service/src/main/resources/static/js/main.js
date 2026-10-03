import { listarLecciones, obtenerLeccion } from './api.js';
import { establecerLeccion, leccionActiva } from './estado.js';
import { OBJETIVOS, textoObjetivo } from './textos.js';
import { crearConsola, crearMensaje } from './ui/consola.js';
import { abrirDialogo, confirmar, prepararDialogo } from './ui/dialogos.js';
import { crearEditor } from './ui/editor.js';
import { prepararConfiguracion } from './ui/configuracion.js';
import { prepararControlesFase2 } from './ui/fase2.js';
import { crearPanelLeccion } from './ui/leccion.js';
import { crearMenus } from './ui/menu.js';
import { prepararAdmin } from './ui/admin.js';

const $ = (id) => document.getElementById(id);

const consola = crearConsola($('consola'));
const menus = crearMenus(document.querySelector('.barra-menu'));
const editor = crearEditor({ texto: $('editor'), numeros: $('editor-numeros'), posicion: $('editor-posicion') });
const panel = crearPanelLeccion({
    vacio: $('leccion-vacia'),
    contenido: $('contenido-leccion'),
    titulo: $('titulo-leccion'),
    objetivo: $('objetivo-leccion'),
});
const dlgAbrir = prepararDialogo($('dlg-abrir'));
const dlgConfirmar = $('dlg-confirmar');
const dlgAyuda = prepararDialogo($('dlg-ayuda'));

const AVISO_BORRADO = 'Se borrará lo que escribiste en el editor.';

// ---------- Abrir lección ----------

async function mostrarListaLecciones() {
    const estado = $('abrir-estado');
    const lista = $('lista-lecciones');
    estado.textContent = 'Cargando lecciones…';
    estado.hidden = false;
    lista.replaceChildren();
    abrirDialogo(dlgAbrir);

    try {
        const lecciones = await listarLecciones();
        if (lecciones.length === 0) {
            estado.textContent = 'Todavía no hay lecciones disponibles.';
            return;
        }
        estado.hidden = true;
        const activa = leccionActiva()?.id;
        for (const leccion of lecciones) {
            const boton = document.createElement('button');
            boton.type = 'button';
            boton.className = 'lista-lecciones__item';
            if (leccion.id === activa) {
                boton.setAttribute('aria-current', 'true');
            }
            const nombre = document.createElement('span');
            nombre.className = 'lista-lecciones__nombre';
            nombre.textContent = leccion.titulo;
            const objetivo = document.createElement('span');
            objetivo.className = 'lista-lecciones__objetivo';
            objetivo.textContent = `Objetivo: ${textoObjetivo(leccion.objetivo)}`;
            boton.append(nombre, objetivo);
            boton.addEventListener('click', () => seleccionarLeccion(leccion.id));
            const item = document.createElement('li');
            item.append(boton);
            lista.append(item);
        }
        lista.querySelector('button')?.focus();
    } catch (error) {
        estado.textContent = `No se pudo cargar la lista de lecciones. ${error.message}`;
    }
}

async function seleccionarLeccion(id) {
    dlgAbrir.close();
    if (leccionActiva() && editor.tieneContenido()) {
        const continuar = await confirmar(dlgConfirmar, {
            titulo: '¿Abrir otra lección?',
            texto: AVISO_BORRADO,
            aceptar: 'Abrir lección',
            cancelar: 'Seguir escribiendo',
        });
        if (!continuar) {
            return;
        }
    }
    try {
        const leccion = await obtenerLeccion(id);
        establecerLeccion(leccion);
        panel.mostrar(leccion);
        editor.limpiar();
        editor.habilitar(true);
        consola.escribir('sistema', `Lección cargada: ${leccion.titulo}. Objetivo: ${textoObjetivo(leccion.objetivo)}.`);
        // Con mouse y teclado se puede empezar a escribir de inmediato. En pantallas táctiles,
        // enfocar el editor abriría el teclado virtual encima de la lección que hay que leer.
        if (window.matchMedia('(pointer: fine)').matches) {
            editor.enfocar();
        } else {
            $('contenido-leccion').focus({ preventScroll: true });
        }
    } catch (error) {
        consola.escribir('sistema', `No se pudo abrir la lección ${id}. ${error.message}`);
    }
}

// ---------- Cerrar lección ----------

async function cerrarLeccion() {
    const leccion = leccionActiva();
    if (!leccion) {
        consola.escribir('sistema', 'No hay ninguna lección abierta.');
        return;
    }
    if (editor.tieneContenido()) {
        const continuar = await confirmar(dlgConfirmar, {
            titulo: '¿Cerrar la lección?',
            texto: AVISO_BORRADO,
            aceptar: 'Cerrar lección',
            cancelar: 'Seguir escribiendo',
        });
        if (!continuar) {
            return;
        }
    }
    establecerLeccion(null);
    panel.limpiar();
    editor.limpiar();
    editor.habilitar(false);
    consola.escribir('sistema', `Lección cerrada: ${leccion.titulo}.`);
    $('btn-vacio-abrir').focus();
}

// ---------- Ayuda ----------

function prepararAyuda() {
    const lista = $('ayuda-objetivos');
    for (const [clave, objetivo] of Object.entries(OBJETIVOS)) {
        const termino = document.createElement('dt');
        termino.textContent = objetivo.corto;
        const detalle = document.createElement('dd');
        const codigo = document.createElement('code');
        codigo.textContent = `objetivo=${clave}`;
        detalle.append(`${objetivo.descripcion} En config.txt: `, codigo);
        lista.append(termino, detalle);
    }
    $('ayuda-consola').append(
        crearMensaje('sistema', 'Lección cargada: leccion-01-hola-mundo.'),
        crearMensaje('exito', 'Compilación terminada con 0 errores.'),
        crearMensaje('error-compilacion', "Main.java:3: error: ';' expected\n        int edad = 20\n                     ^"),
        crearMensaje('error-ejecucion', 'Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException:\nIndex 3 out of bounds for length 3\n    at Main.main(Main.java:5)'),
    );
}

// ---------- Acciones ----------

const ACCIONES = {
    'abrir-leccion': mostrarListaLecciones,
    'cerrar-leccion': cerrarLeccion,
    ayuda: () => abrirDialogo(dlgAyuda),
};

document.addEventListener('click', (evento) => {
    const disparador = evento.target.closest('[data-accion]');
    if (disparador && ACCIONES[disparador.dataset.accion]) {
        ACCIONES[disparador.dataset.accion]();
    }
});

$('btn-limpiar-consola').addEventListener('click', () => consola.limpiar());

prepararConfiguracion({
    menos: $('btn-fuente-menos'),
    mas: $('btn-fuente-mas'),
    valor: $('fuente-actual'),
    radiosTema: [...document.querySelectorAll('input[name="tema"]')],
});
prepararControlesFase2(consola);
prepararAyuda();
prepararAdmin({ consola, menus, alCambiarLecciones: () => {} });
editor.habilitar(false);
consola.escribir('sistema', 'Abre una lección desde el menú Lecciones para empezar.');
