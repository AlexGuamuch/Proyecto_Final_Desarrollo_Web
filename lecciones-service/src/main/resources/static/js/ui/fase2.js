// Controles que existen en la interfaz pero se implementan en la fase 2 (Compilar, Ejecutar,
// Historial). Usan aria-disabled en lugar de disabled para seguir siendo enfocables: así el
// aviso "Disponible en fase 2" aparece con mouse, con teclado y al tocar en pantallas táctiles.

const DURACION_AVISO = 2500;

export function prepararControlesFase2(consola) {
    for (const boton of document.querySelectorAll('[data-fase2]')) {
        const tip = document.getElementById(boton.getAttribute('aria-describedby'));
        let temporizador;

        boton.addEventListener('click', () => {
            tip.classList.remove('tip--oculto');
            tip.classList.add('tip--visible');
            clearTimeout(temporizador);
            temporizador = setTimeout(() => tip.classList.remove('tip--visible'), DURACION_AVISO);
            consola.escribir('sistema', `${boton.dataset.fase2} estará disponible en la fase 2.`);
        });

        // Esc oculta el aviso aunque el mouse siga encima (WCAG 1.4.13).
        boton.addEventListener('keydown', (evento) => {
            if (evento.key === 'Escape') {
                tip.classList.remove('tip--visible');
                tip.classList.add('tip--oculto');
            }
        });
        const reactivar = () => tip.classList.remove('tip--oculto');
        boton.addEventListener('pointerleave', reactivar);
        boton.addEventListener('blur', reactivar);
    }
}
