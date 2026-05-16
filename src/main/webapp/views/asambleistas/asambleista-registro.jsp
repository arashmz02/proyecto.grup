<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="controllers.AsambleistaController.OpcionCatalogo" %>
<%--
    Vista: formulario de alta de asambleistas - Issue #9

    Esta vista NO contiene logica de negocio (regla del MVC).
    Solo lee atributos que el controlador haya colocado:
      - sectores, puestos: opciones de los selects
      - error: mensaje de error a mostrar (si hubo)
      - *Prev: valores que el usuario ya habia escrito, para
        preservarlos en caso de error
--%>
<%
    @SuppressWarnings("unchecked")
    List<OpcionCatalogo> sectores =
        (List<OpcionCatalogo>) request.getAttribute("sectores");
    @SuppressWarnings("unchecked")
    List<OpcionCatalogo> puestos =
        (List<OpcionCatalogo>) request.getAttribute("puestos");

    String error = (String) request.getAttribute("error");

    // Valores previos (para no perder lo escrito al volver con error)
    String cedulaPrev      = (String) request.getAttribute("cedulaPrev");
    String nombrePrev      = (String) request.getAttribute("nombrePrev");
    String correoPrev      = (String) request.getAttribute("correoPrev");
    String idSectorPrev    = (String) request.getAttribute("idSectorPrev");
    String idPuestoPrev    = (String) request.getAttribute("idPuestoPrev");
    String fechaInicioPrev = (String) request.getAttribute("fechaInicioPrev");
    String fechaFinPrev    = (String) request.getAttribute("fechaFinPrev");

    // Helpers locales para no repetir codigo en cada input
    String c  = cedulaPrev      == null ? "" : cedulaPrev;
    String n  = nombrePrev      == null ? "" : nombrePrev;
    String co = correoPrev      == null ? "" : correoPrev;
    String fi = fechaInicioPrev == null ? "" : fechaInicioPrev;
    String ff = fechaFinPrev    == null ? "" : fechaFinPrev;
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Nuevo asambleista - SGL-AIR</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Segoe UI', sans-serif; background: #ecf0f1; }

        .barra {
            background: #1f2d3d; color: #fff; padding: 1rem 2rem;
            display: flex; justify-content: space-between; align-items: center;
        }
        .barra a { color: #fff; text-decoration: none; font-size: 0.85rem; }
        .salir { background: #c0392b; padding: 0.5rem 1rem; border-radius: 4px; }

        .contenido { padding: 2rem; max-width: 700px; margin: 0 auto; }

        h2 { color: #1f2d3d; margin-bottom: 1.25rem; font-size: 1.3rem; }

        .tarjeta {
            background: #fff; padding: 1.75rem; border-radius: 6px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
        }

        .seccion-titulo {
            color: #1f2d3d; font-size: 1rem; margin: 1.25rem 0 0.75rem;
            padding-bottom: 0.4rem; border-bottom: 1px solid #ecf0f1;
        }
        .seccion-titulo:first-child { margin-top: 0; }

        .campo { margin-bottom: 1rem; }
        .fila { display: flex; gap: 1rem; }
        .fila .campo { flex: 1; }

        label {
            display: block; font-size: 0.82rem; color: #34495e;
            margin-bottom: 0.3rem; font-weight: 500;
        }
        .obligatorio::after { content: " *"; color: #c0392b; }

        input, select {
            width: 100%; padding: 0.55rem 0.75rem;
            border: 1px solid #cfd8dc; border-radius: 4px;
            font-size: 0.92rem; font-family: inherit;
        }
        input:focus, select:focus { outline: none; border-color: #2980b9; }

        .ayuda {
            font-size: 0.75rem; color: #7f8c8d; margin-top: 0.25rem;
        }

        .alerta-error {
            padding: 0.7rem 1rem; background: #f8d7da; color: #721c24;
            border: 1px solid #f5c6cb; border-radius: 4px;
            margin-bottom: 1rem; font-size: 0.88rem;
        }

        .botones { margin-top: 1.5rem; display: flex; gap: 0.6rem; }
        .btn {
            padding: 0.65rem 1.25rem; background: #2980b9; color: #fff;
            border: none; border-radius: 4px; font-size: 0.92rem;
            text-decoration: none; cursor: pointer; font-family: inherit;
        }
        .btn:hover { background: #2471a3; }
        .btn-cancelar { background: #95a5a6; }
        .btn-cancelar:hover { background: #7f8c8d; }
    </style>
</head>
<body>

    <div class="barra">
        <span>Sistema de Gestion Legislativa AIR &mdash; Nuevo asambleista</span>
        <div>
            <a href="<%= request.getContextPath() %>/inicio">Inicio</a>
            &nbsp;|&nbsp;
            <a class="salir" href="<%= request.getContextPath() %>/auth/logout">
                Cerrar sesion
            </a>
        </div>
    </div>

    <div class="contenido">
        <h2>Registrar nuevo asambleista</h2>

        <div class="tarjeta">

            <% if (error != null) { %>
                <div class="alerta-error"><%= error %></div>
            <% } %>

            <form method="post" action="<%= request.getContextPath() %>/asambleistas/nuevo">

                <div class="seccion-titulo">Datos personales</div>

                <div class="fila">
                    <div class="campo">
                        <label class="obligatorio" for="cedula">Cedula</label>
                        <input type="text" id="cedula" name="cedula"
                               pattern="^[0-9]-[0-9]{4}-[0-9]{4}$"
                               placeholder="1-1234-5678"
                               value="<%= c %>" required>
                        <div class="ayuda">Formato: X-XXXX-XXXX</div>
                    </div>

                    <div class="campo">
                        <label class="obligatorio" for="nombre">Nombre completo</label>
                        <input type="text" id="nombre" name="nombre" maxlength="150"
                               value="<%= n %>" required>
                    </div>
                </div>

                <div class="campo">
                    <label class="obligatorio" for="correo">Correo institucional</label>
                    <input type="email" id="correo" name="correo"
                           placeholder="usuario@itcr.ac.cr"
                           value="<%= co %>" required>
                    <div class="ayuda">
                        Debe terminar en @itcr.ac.cr o @estudiantec.cr
                    </div>
                </div>

                <div class="seccion-titulo">Nombramiento inicial</div>

                <div class="fila">
                    <div class="campo">
                        <label class="obligatorio" for="idSector">Sector</label>
                        <select id="idSector" name="idSector" required>
                            <option value="">-- Seleccione --</option>
                            <% if (sectores != null) {
                                 for (OpcionCatalogo o : sectores) {
                                     String sel = (idSectorPrev != null
                                            && idSectorPrev.equals(String.valueOf(o.id)))
                                            ? "selected" : "";
                            %>
                                <option value="<%= o.id %>" <%= sel %>>
                                    <%= o.nombre %>
                                </option>
                            <%   }
                               }
                            %>
                        </select>
                    </div>

                    <div class="campo">
                        <label for="idPuesto">Puesto</label>
                        <select id="idPuesto" name="idPuesto">
                            <option value="">-- Sin asignar --</option>
                            <% if (puestos != null) {
                                 for (OpcionCatalogo o : puestos) {
                                     String sel = (idPuestoPrev != null
                                            && idPuestoPrev.equals(String.valueOf(o.id)))
                                            ? "selected" : "";
                            %>
                                <option value="<%= o.id %>" <%= sel %>>
                                    <%= o.nombre %>
                                </option>
                            <%   }
                               }
                            %>
                        </select>
                    </div>
                </div>

                <div class="fila">
                    <div class="campo">
                        <label class="obligatorio" for="fechaInicio">Fecha de inicio</label>
                        <input type="date" id="fechaInicio" name="fechaInicio"
                               value="<%= fi %>" required>
                    </div>

                    <div class="campo">
                        <label for="fechaFin">Fecha de fin</label>
                        <input type="date" id="fechaFin" name="fechaFin"
                               value="<%= ff %>">
                        <div class="ayuda">Vacio si es vigente sin fecha de terminacion</div>
                    </div>
                </div>

                <div class="botones">
                    <button type="submit" class="btn">Registrar</button>
                    <a href="<%= request.getContextPath() %>/asambleistas"
                       class="btn btn-cancelar">Cancelar</a>
                </div>
            </form>

        </div>
    </div>

</body>
</html>
