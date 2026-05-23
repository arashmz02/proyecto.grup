function inicializarBuscador(config) {
    const input        = document.getElementById(config.inputId);
    const lista        = document.getElementById(config.listaId);
    const hiddenId     = document.getElementById(config.hiddenId);
    const btnConsultar = document.getElementById(config.btnConsultarId);
    const infoCard     = document.getElementById(config.infoCardId);

    let timer = null;

    input.addEventListener('input', function () {
        const q = this.value.trim();
        clearTimeout(timer);
        hiddenId.value = '';
        btnConsultar.disabled = true;
        if (infoCard) infoCard.style.display = 'none';

        if (q.length < 2) { cerrarLista(); return; }

        timer = setTimeout(function () {
            fetch(config.contextPath + '/api/asambleistas/buscar?q=' + encodeURIComponent(q))
                .then(r => r.json())
                .then(data => renderizarLista(data))
                .catch(() => cerrarLista());
        }, 300);
    });

    function renderizarLista(resultados) {
        lista.innerHTML = '';
        if (!resultados || resultados.length === 0) {
            lista.innerHTML = '<div class="autocomplete-item" style="color:#888;">Sin resultados</div>';
            lista.style.display = 'block';
            return;
        }
        resultados.forEach(item => {
            const div = document.createElement('div');
            div.className = 'autocomplete-item';
            div.innerHTML = '<strong>' + esc(item.nombre) + '</strong>' +
                            '<br><span class="cedula">Cédula: ' + esc(item.cedula) + '</span>';
            div.addEventListener('click', () => seleccionar(item));
            lista.appendChild(div);
        });
        lista.style.display = 'block';
    }

    function seleccionar(item) {
        input.value = item.nombre + ' — ' + item.cedula;
        hiddenId.value = item.id;
        cerrarLista();

        fetch(config.contextPath + '/api/asambleistas/buscar?id=' + item.id)
            .then(r => r.json())
            .then(detalle => {
                if (detalle.encontrado) {
                    mostrarInfo(detalle);
                    btnConsultar.disabled = false;
                }
            })
            .catch(err => console.error('Error cargando detalle:', err));
    }

    function mostrarInfo(d) {
        const set = (id, val) => {
            const el = document.getElementById(id);
            if (el) el.textContent = val || '—';
        };
        set(config.infoNombreId, d.nombre);
        set(config.infoCedulaId, d.cedula);
        set(config.infoSectorId, d.sector);
        set(config.infoFechaInicioId, d.fechaInicio);

        const estadoEl = document.getElementById(config.infoEstadoId);
        if (estadoEl) {
            estadoEl.innerHTML = d.estadoNombramiento === 'Vigente'
                ? '<span class="badge-vigente">VIGENTE</span>'
                : '<span class="badge-inactivo">' + esc(d.estadoNombramiento) + '</span>';
        }
        if (infoCard) infoCard.style.display = 'block';
    }

    btnConsultar.addEventListener('click', function () {
        const id = hiddenId.value;
        if (!id) { alert('Seleccione un asambleísta primero.'); return; }

        const desde = config.fechaDesdeId
            ? (document.getElementById(config.fechaDesdeId) || {}).value || ''
            : '';
        const hasta = config.fechaHastaId
            ? (document.getElementById(config.fechaHastaId) || {}).value || ''
            : '';

        if (desde && hasta && desde > hasta) {
            alert('La fecha "Desde" no puede ser mayor a "Hasta".');
            return;
        }

        if (config.urlConsultar && config.urlConsultar !== '#') {
            let url = config.urlConsultar + '?asambleistaId=' + encodeURIComponent(id);
            if (desde) url += '&fechaDesde=' + encodeURIComponent(desde);
            if (hasta) url += '&fechaHasta=' + encodeURIComponent(hasta);
            window.location.href = url;
        }
    });

    document.addEventListener('click', e => {
        if (e.target !== input) cerrarLista();
    });

    function cerrarLista() {
        lista.style.display = 'none';
        lista.innerHTML = '';
    }

    function esc(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }
}