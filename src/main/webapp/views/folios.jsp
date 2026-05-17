<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%--
    Vista de generacion de folios - Issue #1 (Arash)
    Sin logica de negocio (regla MVC).
    Lee de request:
      - folioGenerado: folio recien creado (si lo hay)
      - error: mensaje de error (si lo hay)
      - tipoUsado, anioUsado, obsUsada: para mostrar como referencia
--%>
<%
    String folioGenerado = (String) request.getAttribute("folioGenerado");
    String error         = (String) request.getAttribute("error");
    String tipoUsado     = (String) request.getAttribute("tipoUsado");
    Object anioUsado     = request.getAttribute("anioUsado");
    String obsUsada      = (String) request.getAttribute("obsUsada");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Generacion de Folios - SGL-AIR</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Segoe UI', sans-serif; background: #ecf0f1; }

        .barra {
            background: #1f2d3d; color: #fff; padding: 1rem 2rem;
            display: flex; justify-content: space-between; align-items: center;
        }
        .barra a { color: #fff; text-decoration: none; font-size: 0.85rem; }
        .salir { background: #c0392b; padding: 0.5rem 1rem; border-radius: 4px; }

        .contenido { padding: 2rem; max-width: 600px; margin: 0 auto; }

        h2 { color: #1f2d3d; margin-bottom: 1.25rem; font-size: 1.3rem; }

        .tarjeta {
            background: #fff; padding: 1.75rem; border-radius: 6px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.05); margin-bottom: 1rem;
        }

        .campo { margin-bottom: 1rem; }
        label {
            display: block; font-size: 0.82rem; color: #34495e;
            margin-bottom: 0.3rem; font-weight: 500;
        }
        input {
            width: 100%; padding: 0.55rem 0.75rem;
            border: 1px solid #cfd8dc; border-radius: 4px;
            font-size: 0.92rem; font-family: inherit;
        }
        input:focus { outline: none; border-color: #2980b9; }

        .btn {
            padding: 0.65rem 1.25rem; background: #2980b9; color: #fff;
            border: none; border-radius: 4px; font-size: 0.92rem;
            cursor: pointer; font-family: inherit; margin-top: 0.5rem;
        }
        .btn:hover { background: #2471a3; }

        .alerta {
            padding: 0.8rem 1.1rem; border-radius: 4px;
            font-size: 0.9rem; margin-bottom: 1rem;
        }
        .alerta-error { background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .alerta-ok    { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; }

        .folio-box {
            font-family: 'Consolas', 'Courier New', monospace;
            font-size: 1.5rem; font-weight: 700;
            color: #1f2d3d; padding: 0.75rem 1rem;
            background: #ecf0f1; border-radius: 4px;
            display: inline-block; margin-top: 0.5rem;
        }
        .detalle { color: #7f8c8d; font-size: 0.85rem; margin-top: 0.75rem; }
    </style>
</head>
<body>

    <div class="barra">
        <span>Sistema de Gestion Legislativa AIR &mdash; Generacion de Folios</span>
        <div>
            <a href="<%= request.getContextPath() %>/inicio">Inicio</a>
            &nbsp;|&nbsp;
            <a class="salir" href="<%= request.getContextPath() %>/auth/logout">
                Cerrar sesion
            </a>
        </div>
    </div>

    <div class="contenido">
        <h2>Generar nuevo folio</h2>

        <% if (error != null) { %>
            <div class="alerta alerta-error"><%= error %></div>
        <% } %>

        <% if (folioGenerado != null) { %>
            <div class="tarjeta">
                <div class="alerta alerta-ok">Folio generado correctamente.</div>
                <p><strong>Folio:</strong></p>
                <div class="folio-box"><%= folioGenerado %></div>
                <p class="detalle">
                    Tipo: <%= tipoUsado %> &nbsp;|&nbsp;
                    Anio: <%= anioUsado %>
                    <% if (obsUsada != null && !obsUsada.isBlank()) { %>
                        &nbsp;|&nbsp; Obs: <%= obsUsada %>
                    <% } %>
                </p>
            </div>
        <% } %>

        <div class="tarjeta">
            <form method="post" action="<%= request.getContextPath() %>/folios">

                <div class="campo">
                    <label for="tipoDocumento">Tipo de Documento</label>
                    <input type="text" id="tipoDocumento" name="tipoDocumento"
                           placeholder="ej. OFICIO, CERTIFICACION, ACUERDO" required>
                </div>

                <div class="campo">
                    <label for="anio">Anio</label>
                    <input type="number" id="anio" name="anio"
                           min="2020" max="2099" value="2026" required>
                </div>

                <div class="campo">
                    <label for="observacion">Observacion</label>
                    <input type="text" id="observacion" name="observacion"
                           placeholder="Opcional">
                </div>

                <button type="submit" class="btn">Generar Folio</button>
            </form>
        </div>
    </div>

</body>
</html>