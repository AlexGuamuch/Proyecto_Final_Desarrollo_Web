// Textos de la interfaz que dependen de datos de la API.

export const OBJETIVOS = {
    EJECUTAR_EXITOSO: {
        corto: 'Ejecutar sin errores',
        descripcion: 'Compila sin errores y ejecuta el programa hasta el final sin que se detenga por un error.',
    },
    COMPILAR_EXITOSO: {
        corto: 'Compilar sin errores',
        descripcion: 'Compila hasta que el compilador reporte 0 errores. Practicas escribir código con la sintaxis correcta.',
    },
    COMPILAR_CON_ERROR: {
        corto: 'Provocar un error de compilación',
        descripcion: 'Compila y consigue que el compilador reporte al menos un error. Aprendes a leer sus mensajes.',
    },
    EJECUTAR_CON_ERROR: {
        corto: 'Provocar un error de ejecución',
        descripcion: 'Compila sin errores y, al ejecutar, el programa se detiene con un error. Aprendes a distinguirlo de un error de compilación.',
    },
};

export function textoObjetivo(clave) {
    return OBJETIVOS[clave]?.corto ?? clave;
}
