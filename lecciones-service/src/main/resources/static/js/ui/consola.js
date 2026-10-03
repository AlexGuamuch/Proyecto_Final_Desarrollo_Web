import { icono } from './iconos.js';

// Cada tipo de mensaje se distingue por ícono, nombre y borde, no solo por color.
const TIPOS = {
    sistema: { nombre: 'Sistema', icono: 'i-sistema', nombreVisible: false },
    exito: { nombre: 'Correcto', icono: 'i-ok', nombreVisible: true },
    'error-compilacion': { nombre: 'Error de compilación', icono: 'i-error', nombreVisible: true },
    'error-ejecucion': { nombre: 'Error de ejecución', icono: 'i-alerta', nombreVisible: true },
};

/** Construye un mensaje de consola. El texto se inserta como texto, nunca como HTML. */
export function crearMensaje(tipo, texto) {
    const definicion = TIPOS[tipo] ?? TIPOS.sistema;
    const item = document.createElement('li');
    item.className = `msj msj--${tipo in TIPOS ? tipo : 'sistema'}`;
    item.append(icono(definicion.icono, 'icono msj__icono'));

    const cuerpo = document.createElement('div');
    const nombre = document.createElement('span');
    nombre.className = definicion.nombreVisible ? 'msj__tipo' : 'sr-only';
    nombre.textContent = definicion.nombreVisible ? definicion.nombre : `${definicion.nombre}: `;
    const contenido = document.createElement('span');
    contenido.className = 'msj__texto';
    contenido.textContent = texto;
    cuerpo.append(nombre, contenido);
    item.append(cuerpo);
    return item;
}

export function crearConsola(lista) {
    return {
        escribir(tipo, texto) {
            lista.append(crearMensaje(tipo, texto));
            lista.scrollTop = lista.scrollHeight;
        },
        limpiar() {
            lista.replaceChildren();
        },
    };
}
