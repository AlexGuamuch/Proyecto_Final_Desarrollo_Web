// Editor de código: un <textarea> sin ayudas inteligentes. Solo agrega lo mínimo para
// escribir Java a mano: Tab inserta 4 espacios, números de línea y renglón actual resaltado.

const SANGRIA = '    ';

export function crearEditor({ texto, numeros, posicion }) {
    let salirConTab = false;
    let lineasNumeradas = 0;

    function actualizarNumeros() {
        const total = texto.value.split('\n').length;
        if (total === lineasNumeradas) {
            return;
        }
        const fragmento = document.createDocumentFragment();
        for (let i = 1; i <= total; i++) {
            const numero = document.createElement('span');
            numero.textContent = String(i);
            fragmento.append(numero);
        }
        numeros.replaceChildren(fragmento);
        lineasNumeradas = total;
        numeros.scrollTop = texto.scrollTop;
    }

    function actualizarPosicion() {
        const antes = texto.value.slice(0, texto.selectionStart);
        const linea = antes.split('\n').length;
        const columna = antes.length - antes.lastIndexOf('\n');
        posicion.textContent = `Línea ${linea}, col ${columna}`;
        texto.style.setProperty('--linea-actual', String(linea - 1));
    }

    function actualizar() {
        actualizarNumeros();
        actualizarPosicion();
    }

    /** Reemplaza la selección conservando el historial de deshacer cuando el navegador lo permite. */
    function reemplazarSeleccion(nuevoTexto) {
        if (!document.execCommand('insertText', false, nuevoTexto)) {
            texto.setRangeText(nuevoTexto, texto.selectionStart, texto.selectionEnd, 'end');
            texto.dispatchEvent(new Event('input', { bubbles: true }));
        }
    }

    function seleccionarLineasCompletas() {
        const valor = texto.value;
        const inicio = valor.lastIndexOf('\n', texto.selectionStart - 1) + 1;
        let fin = valor.indexOf('\n', texto.selectionEnd === texto.selectionStart ? texto.selectionEnd : texto.selectionEnd - 1);
        if (fin === -1) {
            fin = valor.length;
        }
        texto.setSelectionRange(inicio, fin);
        return valor.slice(inicio, fin);
    }

    function sangrar() {
        if (!texto.value.slice(texto.selectionStart, texto.selectionEnd).includes('\n')) {
            reemplazarSeleccion(SANGRIA);
            return;
        }
        const bloque = seleccionarLineasCompletas();
        const inicio = texto.selectionStart;
        const nuevo = bloque.split('\n').map((l) => SANGRIA + l).join('\n');
        reemplazarSeleccion(nuevo);
        texto.setSelectionRange(inicio, inicio + nuevo.length);
    }

    function quitarSangria() {
        const unaLinea = !texto.value.slice(texto.selectionStart, texto.selectionEnd).includes('\n');
        const cursor = texto.selectionStart;
        const bloque = seleccionarLineasCompletas();
        const inicio = texto.selectionStart;
        const lineas = bloque.split('\n');
        const quitados = lineas.map((l) => l.match(/^ {0,4}/)[0].length);
        const nuevo = lineas.map((l, i) => l.slice(quitados[i])).join('\n');
        if (nuevo === bloque) {
            texto.setSelectionRange(cursor, cursor);
            return;
        }
        reemplazarSeleccion(nuevo);
        if (unaLinea) {
            const destino = Math.max(inicio, cursor - quitados[0]);
            texto.setSelectionRange(destino, destino);
        } else {
            texto.setSelectionRange(inicio, inicio + nuevo.length);
        }
    }

    texto.addEventListener('keydown', (evento) => {
        if (evento.key === 'Escape') {
            // Esc y luego Tab saca el foco del editor (evita una trampa de teclado).
            salirConTab = true;
            return;
        }
        if (evento.key === 'Tab' && !evento.ctrlKey && !evento.altKey && !evento.metaKey) {
            if (salirConTab || texto.readOnly) {
                salirConTab = false;
                return;
            }
            evento.preventDefault();
            if (evento.shiftKey) {
                quitarSangria();
            } else {
                sangrar();
            }
            actualizar();
            return;
        }
        salirConTab = false;
    });

    texto.addEventListener('input', actualizar);
    for (const tipo of ['keyup', 'click', 'select', 'focus']) {
        texto.addEventListener(tipo, actualizarPosicion);
    }
    texto.addEventListener('blur', () => {
        salirConTab = false;
    });
    texto.addEventListener('scroll', () => {
        numeros.scrollTop = texto.scrollTop;
    });

    actualizar();

    return {
        tieneContenido() {
            return texto.value.trim().length > 0;
        },
        limpiar() {
            texto.value = '';
            texto.scrollTop = 0;
            texto.scrollLeft = 0;
            actualizar();
        },
        habilitar(activo) {
            texto.readOnly = !activo;
            texto.placeholder = activo
                ? 'Escribe aquí el código, a mano.'
                : 'Primero abre una lección.';
        },
        enfocar() {
            texto.focus();
        },
    };
}
