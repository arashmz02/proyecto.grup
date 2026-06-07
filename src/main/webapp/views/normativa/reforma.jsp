<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Crear Elemento - AIR</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: Arial, sans-serif; background-color: #f5f5f5; padding: 20px; }
        .contenedor { max-width: 700px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        h2 { color: #333; margin-bottom: 30px; text-align: center; }
        .grupo-formulario { margin-bottom: 20px; }
        label { display: block; font-weight: bold; margin-bottom: 8px; color: #555; }
        input, select, textarea { width: 100%; padding: 10px; border: 1px solid #ccc; border-radius: 4px; font-family: Arial, sans-serif; }
        textarea { resize: vertical; min-height: 120px; }
        button { width: 100%; padding: 12px; background: #007bff; color: white; border: none; border-radius: 4px; font-size: 16px; font-weight: bold; cursor: pointer; }
        button:hover { background: #0056b3; }
        #mensaje { margin-top: 20px; padding: 15px; border-radius: 4px; display: none; }
        #mensaje.exito { background: #d4edda; color: #155724; }
        #mensaje.error { background: #f8d7da; color: #721c24; }
    </style>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/air-unificado.css">
</head>
<body>
<div class="air-bar"><span class="marca">SGL-AIR &middot; Asamblea Institucional Representativa</span><nav><a href="${pageContext.request.contextPath}/inicio">Inicio</a><a href="${pageContext.request.contextPath}/auth/logout" class="salir">Salir</a></nav></div>
    <div class="contenedor">
        <h2>✏️ Crear Elemento Normativo</h2>

        <form id="form-elemento">
            <div class="grupo-formulario">
                <label for="reglamento"><strong>Reglamento:</strong></label>
                <select id="reglamento" name="id_reglamento" required>
                    <option value="1">Estatuto Orgánico del ITCR</option>
                    <option value="2">Reglamento de la AIR</option>
                </select>
            </div>

            <div class="grupo-formulario">
                <label for="nivel"><strong>Tipo de Elemento:</strong></label>
                <select id="nivel" name="id_nivel_reglamento" required>
                    <option value="">-- Selecciona --</option>
                    <option value="1">🏛️ Título</option>
                    <option value="2">📘 Capítulo</option>
                    <option value="3">📄 Artículo</option>
                    <option value="4">→ Inciso</option>
                    <option value="5">→→ Sub-inciso</option>
                </select>
            </div>

            <div class="grupo-formulario">
                <label for="numero"><strong>Número/Etiqueta:</strong></label>
                <input type="text" id="numero" name="numero_etiqueta" required placeholder="Ej: 18, a), i.">
            </div>

            <div class="grupo-formulario">
                <label for="orden"><strong>Orden:</strong></label>
                <input type="number" id="orden" name="orden" required value="1" min="1">
            </div>

            <div class="grupo-formulario">
                <label for="contenido"><strong>Contenido:</strong></label>
                <textarea id="contenido" name="contenido_texto" required placeholder="Escribe el texto del elemento"></textarea>
            </div>

            <button type="submit">✅ Crear Elemento</button>
        </form>

        <div id="mensaje"></div>
    </div>

    <script>
        document.getElementById('form-elemento').addEventListener('submit', async (e) => {
            e.preventDefault();

            const datos = {
                id_reglamento: parseInt(document.getElementById('reglamento').value),
                id_nivel_reglamento: parseInt(document.getElementById('nivel').value),
                numero_etiqueta: document.getElementById('numero').value,
                contenido_texto: document.getElementById('contenido').value,
                orden: parseInt(document.getElementById('orden').value)
            };

            try {
                const response = await fetch('/proyecto/api/normativa/reforma', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(datos)
                });

                const result = await response.json();

                if (result.success) {
                    mostrarMensaje('✓ ' + result.mensaje, 'exito');
                    document.getElementById('form-elemento').reset();
                } else {
                    mostrarMensaje('✗ Error: ' + result.error, 'error');
                }
            } catch (error) {
                mostrarMensaje('✗ Error: ' + error.message, 'error');
            }
        });

        function mostrarMensaje(texto, tipo) {
            const mensajeDiv = document.getElementById('mensaje');
            mensajeDiv.textContent = texto;
            mensajeDiv.className = tipo;
            mensajeDiv.style.display = 'block';
        }
    </script>
</body>
</html>