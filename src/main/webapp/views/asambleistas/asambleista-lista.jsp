<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="models.Asambleista" %>
<%@ page import="controllers.AsambleistaController.FilaAsambleista" %>
<%--
    Vista: lista de asambleistas - Issue #9
    Sirve dos casos:
      1. /asambleistas         -> tabla completa
      2. /asambleistas/detalle -> el mismo formato pero con
         el historial de un asambleista debajo
--%>
<%
    @SuppressWarnings("unchecked")
    List<FilaAsambleista> filas = (List<FilaAsambleista>) request.getAttribute("filas");

    Asambleista detalleAsambleista = (Asambleista) request.getAttribute("asambleista");
    Boolean vigente = (Boolean) request.getAttribute("vigente");

    @SuppressWarnings("unchecked")
    List<Asambleista.NombramientoVista> historial =
        (List<Asambleista.NombramientoVista>) request.getAttribute("historial");

    @SuppressWarnings("unchecked")
    java.util.List<String> permisosSesion =
        (java.util.List<String>) session.getAttribute("permisos");
    boolean puedeRegistrar = permisosSesion != null
                          && permisosSesion.contains("REGISTRAR_ASAMBLEISTAS");

    String mensajeExito = (request.getParameter("creado") != null)
        ? "Asambleista registrado correctamente."
        : null;

    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Asambleistas - SGL-AIR</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: 'Segoe UI', sans-serif; background: #ecf0f1; }

        .barra {
            background: #1f2d3d; color: #fff; padding: 1rem 2rem;
            display: flex; justify-content: space-between; align-items: center;
        }
        .barra a { color: #fff; text-decoration: none; font-size: 0.85rem; }
        .salir {
            background: #c0392b; padding: 0.5rem 1rem; border-radius: 4px;
        }

        .contenido { padding: 2rem; max-width: 1100px; margin: 0 auto; }

        .acciones { margin-bottom: 1rem; }
        .btn {
            display: inline-block; padding: 0.6rem 1.1rem;
            background: #2980b9; color: #fff; text-decoration: none;
            border-radius: 4px; font-size: 0.9rem; border: none; cursor: pointer;
        }
        .btn:hover { background: #2471a3; }

        table {
            width: 100%; border-collapse: collapse; background: #fff;
            box-shadow: 0 2px 8px rgba(0,0,0,0.05); border-radius: 6px;
            overflow: hidden;
        }
        th, td { padding: 0.75rem 1rem; text-align: left; font-size: 0.9rem; }
        th { background: #34495e; color: #fff; font-weight: 600; }
        tr:not(:last-child) td { border-bottom: 1px solid #ecf0f1; }
        tr:hover td { background: #f8f9fa; }

        .estado-vigente {
            display: inline-block; padding: 0.2rem 0.6rem;
            background: #d4edda; color: #155724; border-radius: 12px;
            font-size: 0.78rem; font-weight: 600;
        }
        .estado-inactivo {
            display: inline-block; padding: 0.2rem 0.6rem;
            background: #f8d7da; color: #721c24; border-radius: 12px;
            font-size: 0.78rem; font-weight: 600;
        }

        .alerta {
            padding: 0.8rem 1.1rem; border-radius: 4px;
            margin-bottom: 1rem; font-size: 0.9rem;
        }
        .alerta-ok    { background: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .alerta-error { background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }

        .titulo-seccion { color: #1f2d3d; margin: 1.5rem 0 1rem; font-size: 1.25rem; }
        .vacio { color: #7f8c8d; padding: 1rem; font-style: italic; }

        .detalle-info {
            background: #fff; padding: 1.25rem; border-radius: 6px;
            margin-bottom: 1.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.05);
        }
        .detalle-info p { margin: 0.3rem 0; font-size: 0.95rem; color: #34495e; }
        .detalle-info strong { color: #1f2d3d; }
    </style>
</head>
<body>

    <div class="barra">
        <span>Sistema de Gestion Legislativa AIR &mdash; Asambleistas</span>
        <div>
            <a href="<%= request.getContextPath() %>/inicio">Inicio</a>
            &nbsp;|&nbsp;
            <a class="salir" href="<%= request.getContextPath() %>/auth/logout">
                Cerrar sesion
            </a>
        </div>
    </div>

    <div class="contenido">

        <% if (mensajeExito != null) { %>
            <div class="alerta alerta-ok"><%= mensajeExito %></div>
        <% } %>

        <% if (error != null) { %>
            <div class="alerta alerta-error"><%= error %></div>
        <% } %>

        <%-- ============================================
             MODO DETALLE: muestra info del asambleista
             y su historial de nombramientos
             ============================================ --%>
        <% if (detalleAsambleista != null) { %>

            <h2 class="titulo-seccion">Detalle de asambleista</h2>

            <div class="detalle-info">
                <p><strong>Nombre:</strong> <%= detalleAsambleista.getNombre() %></p>
                <p><strong>Cedula:</strong> <%= detalleAsambleista.getCedula() %></p>
                <p><strong>Correo:</strong> <%= detalleAsambleista.getCorreoInstitucional() %></p>
                <p><strong>Estado actual:</strong>
                    <% if (vigente != null && vigente) { %>
                        <span class="estado-vigente">VIGENTE</span>
                    <% } else { %>
                        <span class="estado-inactivo">INACTIVO</span>
                    <% } %>
                </p>
            </div>

            <h3 class="titulo-seccion">Historial de nombramientos</h3>

            <% if (historial == null || historial.isEmpty()) { %>
                <p class="vacio">No tiene nombramientos registrados.</p>
            <% } else { %>
                <table>
                    <thead>
                        <tr>
                            <th>Sector</th>
                            <th>Puesto</th>
                            <th>Inicio</th>
                            <th>Fin</th>
                            <th>Estado</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (Asambleista.NombramientoVista n : historial) { %>
                            <tr>
                                <td><%= n.sector %></td>
                                <td><%= n.puesto == null ? "-" : n.puesto %></td>
                                <td><%= n.getFechaInicioFormateada() %></td>
                                <td><%= n.getFechaFinFormateada() %></td>
                                <td><%= n.estado %></td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
            <% } %>

            <p style="margin-top: 1.5rem;">
                <a class="btn" href="<%= request.getContextPath() %>/asambleistas">
                    Volver a la lista
                </a>
            </p>

        <% } else { %>

            <%-- ============================================
                 MODO LISTA: tabla completa de asambleistas
                 ============================================ --%>

            <h2 class="titulo-seccion">Padron de asambleistas</h2>

            <% if (puedeRegistrar) { %>
                <div class="acciones">
                    <a class="btn" href="<%= request.getContextPath() %>/asambleistas/nuevo">
                        + Registrar nuevo asambleista
                    </a>
                </div>
            <% } %>

            <% if (filas == null || filas.isEmpty()) { %>
                <p class="vacio">No hay asambleistas registrados.</p>
            <% } else { %>
                <table>
                    <thead>
                        <tr>
                            <th>Cedula</th>
                            <th>Nombre</th>
                            <th>Correo</th>
                            <th>Estado</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (FilaAsambleista f : filas) {
                            Asambleista a = f.asambleista;
                        %>
                            <tr>
                                <td><%= a.getCedula() %></td>
                                <td><%= a.getNombre() %></td>
                                <td><%= a.getCorreoInstitucional() %></td>
                                <td>
                                    <% if (f.vigente) { %>
                                        <span class="estado-vigente">Vigente</span>
                                    <% } else { %>
                                        <span class="estado-inactivo">Inactivo</span>
                                    <% } %>
                                </td>
                                <td>
                                    <a href="<%= request.getContextPath() %>/asambleistas/detalle?id=<%= a.getIdAsambleista() %>">
                                        Ver detalle
                                    </a>
                                </td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
            <% } %>

        <% } %>

    </div>

</body>
</html>
