// Ventanas modales con <dialog> nativo: atrapan el foco, cierran con Esc y lo devuelven
// al elemento que las abrió.

/** Prepara un diálogo: botones [data-cierra-dialogo] y clic en el fondo lo cierran. */
export function prepararDialogo(dialogo) {
    dialogo.addEventListener('click', (evento) => {
        if (evento.target === dialogo || evento.target.closest('[data-cierra-dialogo]')) {
            dialogo.close();
        }
    });
    return dialogo;
}

export function abrirDialogo(dialogo) {
    if (!dialogo.open) {
        dialogo.showModal();
    }
}

/**
 * Pide confirmación. Resuelve true solo si se elige la acción; Esc o "cancelar" dan false.
 * La opción enfocada al abrir es la que no borra nada.
 */
export function confirmar(dialogo, { titulo, texto, aceptar, cancelar }) {
    dialogo.querySelector('#confirmar-titulo').textContent = titulo;
    dialogo.querySelector('#confirmar-texto').textContent = texto;
    dialogo.querySelector('#confirmar-si').textContent = aceptar;
    dialogo.querySelector('#confirmar-no').textContent = cancelar;
    dialogo.returnValue = '';
    return new Promise((resolver) => {
        const escuchas = new AbortController();
        const terminar = (aceptado) => {
            escuchas.abort();
            resolver(aceptado);
        };
        // "submit" llega en cuanto se pulsa un botón; "close" cubre Esc.
        dialogo.querySelector('form').addEventListener('submit',
            (evento) => terminar(evento.submitter?.value === 'si'), { signal: escuchas.signal });
        dialogo.addEventListener('close', () => terminar(dialogo.returnValue === 'si'), { signal: escuchas.signal });
        dialogo.showModal();
    });
}
