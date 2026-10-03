# Errores de compilación

El compilador revisa tu código antes de ejecutarlo. Si algo no sigue las reglas de Java, se detiene y te dice qué encontró y en qué línea. En esta lección vas a provocar un error a propósito para aprender a leer ese mensaje.

## Copia este código tal como está

Tiene un error escondido. No lo corrijas todavía.

```java
public class Main {
    public static void main(String[] args) {
        int edad = 20
        System.out.println("Tengo " + edad + " años");
    }
}
```

## Cómo leer el mensaje

Al compilar, la consola mostrará algo parecido a esto:

```
Main.java:3: error: ';' expected
        int edad = 20
                     ^
1 error
```

- `Main.java:3` es el archivo y la **línea** donde el compilador se dio cuenta del problema.
- `';' expected` es la descripción: esperaba un punto y coma.
- El `^` señala la columna exacta.
- `1 error` es el total. Corrige siempre el primero: a veces uno solo provoca varios.

## Errores que vas a ver seguido

| Mensaje | Qué significa |
|---|---|
| `';' expected` | Falta un punto y coma al final de la instrucción. |
| `cannot find symbol` | Usaste un nombre que no existe o está mal escrito (revisa mayúsculas). |
| `incompatible types` | Guardaste un valor de un tipo en una variable de otro tipo. |
| `reached end of file while parsing` | Falta cerrar una llave `}`. |

## Objetivo

Compila el código y consigue que el compilador reporte al menos un error. Después, si quieres, agrega el `;` y vuelve a compilar para ver la diferencia.
