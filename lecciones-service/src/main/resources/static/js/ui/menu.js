// Menús desplegables con el patrón "disclosure": un botón con aria-expanded que muestra un
// panel. Funcionan con mouse, teclado (Enter, Espacio, flechas, Esc) y toque.

const ENFOCABLES = 'button:not([disabled]), a[href], input:not([disabled]), [tabindex="0"]';

export function crearMenus(barra) {
    const botones = [...barra.querySelectorAll('[data-menu]')];
    let abierto = null;

    function panelDe(boton) {
        return document.getElementById(boton.getAttribute('aria-controls'));
    }

    function posicionar(boton, panel) {
        const caja = boton.getBoundingClientRect();
        const ancho = panel.offsetWidth;
        const x = Math.max(8, Math.min(caja.left, window.innerWidth - ancho - 8));
        panel.style.setProperty('--panel-x', `${x}px`);
        panel.style.setProperty('--panel-y', `${caja.bottom + 4}px`);
    }

    function abrir(boton, { enfocarPrimero = false } = {}) {
        cerrar();
        const panel = panelDe(boton);
        panel.hidden = false;
        boton.setAttribute('aria-expanded', 'true');
        posicionar(boton, panel);
        abierto = { boton, panel };
        if (enfocarPrimero) {
            panel.querySelector(ENFOCABLES)?.focus();
        }
    }

    function cerrar({ devolverFoco = false } = {}) {
        if (!abierto) {
            return;
        }
        abierto.panel.hidden = true;
        abierto.boton.setAttribute('aria-expanded', 'false');
        if (devolverFoco) {
            abierto.boton.focus();
        }
        abierto = null;
    }

    function moverFoco(panel, direccion) {
        const elementos = [...panel.querySelectorAll(ENFOCABLES)].filter((e) => e.offsetParent !== null);
        if (elementos.length === 0) {
            return;
        }
        const actual = elementos.indexOf(document.activeElement);
        const siguiente = (actual + direccion + elementos.length) % elementos.length;
        elementos[siguiente].focus();
    }

    for (const boton of botones) {
        const panel = panelDe(boton);

        boton.addEventListener('click', () => {
            if (abierto?.boton === boton) {
                cerrar();
            } else {
                abrir(boton);
            }
        });

        boton.addEventListener('keydown', (evento) => {
            if (evento.key === 'ArrowDown') {
                evento.preventDefault();
                abrir(boton, { enfocarPrimero: true });
            }
        });

        panel.addEventListener('keydown', (evento) => {
            if (evento.key === 'ArrowDown' || evento.key === 'ArrowUp') {
                // Los radios usan las flechas para cambiar de opción; ahí no se interfiere.
                if (evento.target.type === 'radio') {
                    return;
                }
                evento.preventDefault();
                moverFoco(panel, evento.key === 'ArrowDown' ? 1 : -1);
            }
        });

        panel.addEventListener('click', (evento) => {
            if (evento.target.closest('[data-cierra-menu]')) {
                cerrar();
            }
        });
    }

    document.addEventListener('keydown', (evento) => {
        if (evento.key === 'Escape' && abierto) {
            cerrar({ devolverFoco: true });
        }
    });

    document.addEventListener('pointerdown', (evento) => {
        if (abierto && !abierto.panel.contains(evento.target) && !abierto.boton.contains(evento.target)) {
            cerrar();
        }
    });

    // Si el foco sale del menú abierto (por ejemplo con Tab), se cierra.
    document.addEventListener('focusin', (evento) => {
        if (abierto && !abierto.panel.contains(evento.target) && evento.target !== abierto.boton) {
            cerrar();
        }
    });

    window.addEventListener('resize', () => cerrar());

    return { cerrar };
}
