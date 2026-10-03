import { textoObjetivo } from '../textos.js';

// Panel de la lección y barra de "Lección activa".
// El HTML llega del servidor ya renderizado y seguro (el HTML crudo del markdown viene escapado).

export function crearPanelLeccion({ vacio, contenido, titulo, objetivo }) {

    /** Numera los bloques de código Java y da a la salida de consola su propio estilo. */
    function prepararCodigo() {
        for (const bloque of contenido.querySelectorAll('pre')) {
            const codigo = bloque.querySelector('code');
            if (!codigo) {
                continue;
            }
            const esCodigo = [...codigo.classList].some((c) => c.startsWith('language-'));
            if (!esCodigo) {
                bloque.classList.add('salida-modelo');
                continue;
            }
            bloque.classList.add('codigo-modelo');
            const lineas = codigo.textContent.replace(/\n$/, '').split('\n');
            const fragmento = document.createDocumentFragment();
            for (const linea of lineas) {
                const span = document.createElement('span');
                span.className = 'linea';
                span.textContent = linea;
                fragmento.append(span);
            }
            codigo.replaceChildren(fragmento);
            bloque.setAttribute('aria-label', `Código de ${lineas.length} líneas`);
        }
    }

    /** Las tablas anchas se desplazan horizontalmente en pantallas chicas. */
    function envolverTablas() {
        for (const tabla of contenido.querySelectorAll('table')) {
            const envoltura = document.createElement('div');
            envoltura.className = 'tabla-scroll';
            tabla.replaceWith(envoltura);
            envoltura.append(tabla);
        }
    }

    return {
        mostrar(leccion) {
            contenido.innerHTML = leccion.html;
            prepararCodigo();
            envolverTablas();
            contenido.hidden = false;
            contenido.scrollTop = 0;
            contenido.parentElement.scrollTop = 0;
            vacio.hidden = true;

            titulo.textContent = leccion.titulo;
            titulo.classList.remove('barra-leccion__titulo--vacio');
            const etiqueta = document.createElement('strong');
            etiqueta.textContent = textoObjetivo(leccion.objetivo);
            objetivo.replaceChildren('Objetivo: ', etiqueta);
            objetivo.hidden = false;
            document.title = `${leccion.titulo} - Práctica de Java`;
        },
        limpiar() {
            contenido.replaceChildren();
            contenido.hidden = true;
            vacio.hidden = false;

            titulo.textContent = 'Ninguna lección abierta';
            titulo.classList.add('barra-leccion__titulo--vacio');
            objetivo.replaceChildren();
            objetivo.hidden = true;
            document.title = 'Práctica de Java';
        },
    };
}
