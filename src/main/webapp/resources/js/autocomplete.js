/* autocomplete.js
 Componente reutilizable de autocompletado para buscar asambleistas.
 Usado en el dashboard (Issue #16) para la exportacion individual.
 * Hace peticiones a /asambleistas/buscar?q=<texto> con un debounce
 * de 300ms para no saturar el servidor. Muestra los resultados en
 * la lista y guarda el id del asambleista seleccionado en el input
 * hidden. Habilita el boton cuando hay un asambleista seleccionado.*/
function inicializarBuscador(config) {
    const input         = document.getElementById(config.inputId);
    const lista         = document.getElementById(config.listaId);
    const hidden        = document.getElementById(config.hiddenId);
    const btnConsultar  = config.btnConsultarId
                          ? document.getElementById(config.btnConsultarId)
                          : null;

    if (!input || !lista || !hidden) {
        console.warn('autocomplete.js: faltan elementos del DOM. ' +
                     'Verifica los IDs en config.');
        return;
    }

    let timeoutBusqueda = null;
    const DEBOUNCE_MS = 300;

    // Listener: cuando el usuario escribe
    input.addEventListener('input', function () {
        const texto = input.value.trim();

        // Reset del id seleccionado al modificar el input
        hidden.value = '';
        if (btnConsultar) btnConsultar.disabled = true;

        // Cancelar busqueda anterior si todavia no se ejecuto
        if (timeoutBusqueda) {
            clearTimeout(timeoutBusqueda);
        }

        // Si el texto es muy corto, ocultar lista
        if (texto.length < 2) {
            lista.style.display = 'none';
            lista.innerHTML = '';
            return;
        }

        // Debounce: esperar 300ms antes de buscar
        timeoutBusqueda = setTimeout(function () {
            buscarYRenderizar(texto);
        }, DEBOUNCE_MS);
    });

    // Listener: cerrar la lista si se hace click fuera
    document.addEventListener('click', function (e) {
        if (!input.contains(e.target) && !lista.contains(e.target)) {
            lista.style.display = 'none';
        }
    });

    /* Hace la peticion AJAX al backend y renderiza los resultados.*/
    function buscarYRenderizar(texto) {
        const url = config.contextPath +
                    '/asambleistas/buscar?q=' + encodeURIComponent(texto);

        fetch(url)
            .then(function (resp) {
                if (!resp.ok) {
                    throw new Error('Error en la respuesta del servidor');
                }
                return resp.json();
            })
            .then(function (resultados) {
                renderizarResultados(resultados);
            })
            .catch(function (error) {
                console.error('Error al buscar asambleistas:', error);
                lista.innerHTML =
                    '<div class="autocomplete-item" style="color:#dc3545;">' +
                    'Error al buscar. Intente de nuevo.</div>';
                lista.style.display = 'block';
            });
    }

    /* Genera los items de la lista con los resultados encontrados.*/
    function renderizarResultados(resultados) {
        lista.innerHTML = '';

        if (!resultados || resultados.length === 0) {
            lista.innerHTML =
                '<div class="autocomplete-item" style="color:#6c757d;">' +
                'Sin resultados.</div>';
            lista.style.display = 'block';
            return;
        }

        resultados.forEach(function (asamb) {
            const item = document.createElement('div');
            item.className = 'autocomplete-item';
            item.innerHTML =
                '<div>' + escaparHTML(asamb.nombre) + '</div>' +
                '<div class="cedula">' + escaparHTML(asamb.cedula) + '</div>';

            item.addEventListener('click', function () {
                input.value = asamb.nombre + ' (' + asamb.cedula + ')';
                hidden.value = asamb.id;
                if (btnConsultar) btnConsultar.disabled = false;
                lista.style.display = 'none';
            });

            lista.appendChild(item);
        });

        lista.style.display = 'block';
    }

    /*Escape basico para evitar XSS al renderizar texto en HTML.*/
    function escaparHTML(texto) {
        if (texto == null) return '';
        return String(texto)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }
}