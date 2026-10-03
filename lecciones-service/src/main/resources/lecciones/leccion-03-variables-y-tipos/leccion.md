# Variables y tipos

Una variable es un espacio con nombre donde tu programa guarda un dato. En Java cada variable tiene un **tipo** que dice qué clase de dato puede guardar, y el compilador revisa que lo respetes.

## Los tipos de esta lección

| Tipo | Guarda | Ejemplo |
|---|---|---|
| `int` | números enteros | `19` |
| `double` | números con decimales | `85.5` |
| `boolean` | verdadero o falso | `true` |
| `char` | un solo carácter, entre comillas simples | `'A'` |
| `String` | texto, entre comillas dobles | `"Ana"` |

## Copia este código

```java
public class Main {
    public static void main(String[] args) {
        int edad = 19;
        double promedio = 85.5;
        boolean aprobado = promedio >= 61;
        char seccion = 'A';
        String nombre = "Ana";

        System.out.println(nombre + " tiene " + edad + " años");
        System.out.println("Promedio: " + promedio);
        System.out.println("Sección: " + seccion);
        System.out.println("Aprobado: " + aprobado);
    }
}
```

## Fíjate en

- `String` va con mayúscula; los otros cuatro tipos van en minúscula.
- `'A'` usa comillas simples porque es un `char`. `"Ana"` usa comillas dobles porque es un `String`.
- En la línea 5, `aprobado` guarda el resultado de comparar: `true` si el promedio es 61 o más.
- Los decimales se escriben con punto: `85.5`, no `85,5`.

## Objetivo

Compila el programa sin errores. Si el compilador te marca alguno, léelo, corrígelo y vuelve a compilar hasta que reporte 0 errores.
