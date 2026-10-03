// Estado de la sesión, solo en variables de memoria. No se usa localStorage, sessionStorage
// ni cookies: al recargar la página todo vuelve a los valores iniciales (requisito de la fase 1).

export const FUENTE = Object.freeze({ minimo: 12, maximo: 24, paso: 2, inicial: 16 });

const estado = {
    tamanoFuente: FUENTE.inicial,
    tema: null,
    leccion: null,
};

export function tamanoFuente() {
    return estado.tamanoFuente;
}

/** Cambia el tamaño de letra en ±paso dentro del rango permitido y devuelve el nuevo valor. */
export function ajustarTamanoFuente(direccion) {
    const nuevo = estado.tamanoFuente + direccion * FUENTE.paso;
    estado.tamanoFuente = Math.min(FUENTE.maximo, Math.max(FUENTE.minimo, nuevo));
    return estado.tamanoFuente;
}

export function tema() {
    return estado.tema;
}

export function establecerTema(valor) {
    estado.tema = valor === 'oscuro' ? 'oscuro' : 'claro';
    return estado.tema;
}

export function leccionActiva() {
    return estado.leccion;
}

export function establecerLeccion(leccion) {
    estado.leccion = leccion;
}
