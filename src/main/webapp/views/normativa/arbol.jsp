<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Arbol del Reglamento - AIR</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', sans-serif; background-color: #ecf0f1; }

        .barra {
            background: #1f2d3d; color: #fff; padding: 1rem 2rem;
            display: flex; justify-content: space-between; align-items: center;
        }
        .barra a { color: #fff; text-decoration: none; font-size: 0.85rem; }
        .salir { background: #c0392b; padding: 0.5rem 1rem; border-radius: 4px; }

        .contenedor { max-width: 1000px; margin: 2rem auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        h1 { color: #1f2d3d; margin-bottom: 30px; text-align: center; }
        #arbol-reglamento { margin-top: 20px; }
        .elemento-arbol { margin-left: 20px; padding-left: 10px; margin-bottom: 5px; border-left: 3px solid #2980b9; }
        .elemento-arbol details { cursor: pointer; }
        .elemento-arbol summary { font-weight: bold; padding: 10px; background: #f9f9f9; border-radius: 4px; user-select: none; }
        .elemento-arbol summary:hover { background: #efefef; }
        .badge { display: inline-block; padding: 3px 8px; border-radius: 12px; font-size: 12px; font-weight: bold; margin-left: 10px; }
        .badge-vigente { background: #d4edda; color: #155724; }
        .elemento-contenido { padding: 15px; background: #f9f9f9; border-radius: 4px; margin-top: 10px; }
        .cargando { text-align: center; color: #666; padding: 40px; font-style: italic; }
        .error { background: #f8d7da; color: #721c24; padding: 15px; border-radius: 4px; }
    </style>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/air-unificado.css">
</head>
<body>

    <div class="barra">
        <span>Sistema de Gestion Legislativa AIR &mdash; Normativa Institucional</span>
        <div>
            <a href="<%= request.getContextPath() %>/inicio">Inicio</a>
            &nbsp;|&nbsp;
            <a class="salir" href="<%= request.getContextPath() %>/auth/logout">
                Cerrar sesion
            </a>
        </div>
    </div>

    <div class="contenedor">
        <h1>Arbol de Reglamentos - AIR</h1>
        <div id="arbol-reglamento" class="cargando">Cargando arbol...</div>
    </div>

    <script>
        async function cargarArbol(idReglamento = 1) {
            try {
                const response = await fetch(`<%= request.getContextPath() %>/api/normativa/arbol/\${idReglamento}`);
                const data = await response.json();

                if (data.success) {
                    renderizarArbol(data.datos);
                } else {
                    mostrarError(data.error);
                }
            } catch (error) {
                console.error("Error:", error);
                mostrarError("Error al cargar el arbol");
            }
        }

        function renderizarArbol(elementos) {
            const contenedor = document.getElementById('arbol-reglamento');
            contenedor.innerHTML = '';
            contenedor.className = '';

            const elementosPorPadre = {};
            elementos.forEach(elem => {
                const padre = elem.id_elemento_padre || 'raiz';
                if (!elementosPorPadre[padre]) elementosPorPadre[padre] = [];
                elementosPorPadre[padre].push(elem);
            });

            function renderizarHijos(padreId = 'raiz') {
                const hijos = elementosPorPadre[padreId] || [];
                return hijos.map(elem => `
                    <div class="elemento-arbol">
                        <details>
                            <summary>
                                <strong>\${elem.numero_etiqueta}</strong> - \${elem.nivel}
                                <span class="badge badge-vigente">Vigente</span>
                            </summary>
                            <div class="elemento-contenido">
                                <p><strong>Contenido:</strong></p>
                                <p>\${elem.contenido_texto}</p>
                                \${renderizarHijos(elem.id_elemento)}
                            </div>
                        </details>
                    </div>
                `).join('');
            }
            contenedor.innerHTML = renderizarHijos('raiz');
        }

        function mostrarError(mensaje) {
            document.getElementById('arbol-reglamento').innerHTML =
                `<div class="error">Error: \${mensaje}</div>`;
        }

        cargarArbol(1);
    </script>
</body>
</html>