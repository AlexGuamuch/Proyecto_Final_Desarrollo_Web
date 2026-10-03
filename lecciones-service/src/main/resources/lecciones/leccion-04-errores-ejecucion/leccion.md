# Errores de ejecución

Que un programa compile no significa que funcione. El compilador solo revisa que el código esté bien escrito; algunos problemas aparecen hasta que el programa **se ejecuta**. A esos se les llama errores de ejecución o excepciones.

## Copia este código

Este programa compila sin errores. El problema aparece al ejecutarlo.

```java
public class Main {
    public static void main(String[] args) {
        int[] notas = {70, 85, 92};
        System.out.println("Primera nota: " + notas[0]);
        System.out.println("Cuarta nota: " + notas[3]);
    }
}
```

## Qué pasa

El arreglo `notas` tiene 3 elementos y sus posiciones son 0, 1 y 2. En la línea 5 el programa pide la posición 3, que no existe. Al ejecutarlo verás algo así:

```
Primera nota: 70
Exception in thread "main" java.lang.ArrayIndexOutOfBoundsException: Index 3 out of bounds for length 3
	at Main.main(Main.java:5)
```

- La primera línea sí se imprimió: el programa funcionó hasta la línea 4.
- `ArrayIndexOutOfBoundsException` es el tipo de error: posición fuera del arreglo.
- `Index 3 out of bounds for length 3` explica el detalle: pediste la posición 3 de un arreglo de 3.
- `Main.java:5` indica la línea donde ocurrió.

## Compilación o ejecución

| | Error de compilación | Error de ejecución |
|---|---|---|
| Cuándo aparece | Al compilar | Al ejecutar |
| ¿Se ejecuta algo? | No, nada | Sí, hasta la línea del error |
| Ejemplo | `';' expected` | `ArrayIndexOutOfBoundsException` |

## Objetivo

Compila el programa sin errores y ejecútalo para que produzca el error de ejecución. Después, si quieres, cambia `notas[3]` por `notas[2]` y ejecútalo de nuevo.
