import { FUENTE, ajustarTamanoFuente, establecerTema, tamanoFuente } from '../estado.js';

// Tamaño de letra y tema. Se aplican con variables CSS sobre <html>; nada se guarda.

export function prepararConfiguracion({ menos, mas, valor, radiosTema, alCambiarTamano = () => {} }) {
    const raiz = document.documentElement;

    function aplicarTamano(px) {
        raiz.style.setProperty('--fs', `${px}px`);
        valor.textContent = `${px} px`;
        menos.disabled = px <= FUENTE.minimo;
        mas.disabled = px >= FUENTE.maximo;
        alCambiarTamano(px);
    }

    function aplicarTema(nombre) {
        raiz.dataset.theme = establecerTema(nombre);
        for (const radio of radiosTema) {
            radio.checked = radio.value === raiz.dataset.theme;
        }
    }

    // Al llegar al límite el botón se deshabilita; el foco pasa al otro para no perderse.
    menos.addEventListener('click', () => {
        aplicarTamano(ajustarTamanoFuente(-1));
        if (menos.disabled) {
            mas.focus();
        }
    });
    mas.addEventListener('click', () => {
        aplicarTamano(ajustarTamanoFuente(+1));
        if (mas.disabled) {
            menos.focus();
        }
    });
    for (const radio of radiosTema) {
        radio.addEventListener('change', () => aplicarTema(radio.value));
    }

    // Valores iniciales: 16 px y el tema que prefiera el sistema operativo.
    aplicarTamano(tamanoFuente());
    aplicarTema(window.matchMedia('(prefers-color-scheme: dark)').matches ? 'oscuro' : 'claro');
}
