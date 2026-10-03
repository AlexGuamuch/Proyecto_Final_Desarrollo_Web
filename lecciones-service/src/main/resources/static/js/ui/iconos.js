const SVG = 'http://www.w3.org/2000/svg';

/** Crea un <svg> que reutiliza un símbolo del sprite de index.html. */
export function icono(nombre, clase = 'icono') {
    const svg = document.createElementNS(SVG, 'svg');
    svg.setAttribute('class', clase);
    svg.setAttribute('aria-hidden', 'true');
    svg.setAttribute('focusable', 'false');
    const uso = document.createElementNS(SVG, 'use');
    uso.setAttribute('href', `#${nombre}`);
    svg.append(uso);
    return svg;
}
