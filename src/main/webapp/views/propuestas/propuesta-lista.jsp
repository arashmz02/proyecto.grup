<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Propuestas - AIR</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        h1 { color: #2c3e50; }
        table { width: 100%; border-collapse: collapse; background: white; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        th, td { border-bottom: 1px solid #eee; padding: 12px; text-align: left; }
        th { background: #34495e; color: white; }
        .badge { display: inline-block; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: bold; }
        .badge-aprobada { background: #d4edda; color: #155724; }
        .badge-rechazada { background: #f8d7da; color: #721c24; }
        .badge-pendiente { background: #fff3cd; color: #856404; }
        .badge-discusion { background: #d1ecf1; color: #0c5460; }
        .filtros { background: white; padding: 16px; border-radius: 8px; margin-bottom: 20px; }
        .btn { padding: 8px 16px; background: #3498db; color: white; border: none; border-radius: 4px; text-decoration: none; display: inline-block; }
        .alert-success { background: #d4edda; color: #155724; padding: 12px; border-radius: 4px; margin-bottom: 16px; }
    </style>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/air-unificado.css">
</head>
<body>
<div class="air-bar"><span class="marca">SGL-AIR &middot; Asamblea Institucional Representativa</span><nav><a href="${pageContext.request.contextPath}/inicio">Inicio</a><a href="${pageContext.request.contextPath}/auth/logout" class="salir">Salir</a></nav></div>

<h1>Buscador de Propuestas</h1>

<c:if test="${param.creado == '1'}">
    <div class="alert-success">Propuesta registrada exitosamente.</div>
</c:if>

<div class="filtros">
    <form method="GET" action="${pageContext.request.contextPath}/propuestas">
        <label>Filtrar por estado:</label>
        <select name="estado" onchange="this.form.submit()">
            <option value="">-- Todos --</option>
            <c:forEach var="e" items="${estados}">
                <option value="${e.id}" <c:if test="${filtroEstadoActivo == e.id}">selected</c:if>>${e.nombre}</option>
            </c:forEach>
        </select>
    </form>
</div>

<p>
    <a href="${pageContext.request.contextPath}/propuestas/nueva" class="btn">+ Nueva Propuesta</a>
</p>

<table>
    <thead>
        <tr>
            <th>Codigo AIR</th>
            <th>Titulo</th>
            <th>Estado</th>
            <th>Acciones</th>
        </tr>
    </thead>
    <tbody>
        <c:choose>
            <c:when test="${empty propuestas}">
                <tr><td colspan="4" style="text-align: center; color: #999;">No hay propuestas que mostrar.</td></tr>
            </c:when>
            <c:otherwise>
                <c:forEach var="p" items="${propuestas}">
                    <tr>
                        <td><strong>${p.codigoAir}</strong>
                            <c:if test="${p.esConciliada()}"><span class="badge badge-discusion">CONCILIADA</span></c:if>
                        </td>
                        <td>${p.titulo}</td>
                        <td>
                            <c:forEach var="e" items="${estados}">
                                <c:if test="${e.id == p.idEstadoPropuesta}">
                                    <c:choose>
                                        <c:when test="${e.nombre == 'Aprobada'}"><span class="badge badge-aprobada">${e.nombre}</span></c:when>
                                        <c:when test="${e.nombre == 'Rechazada'}"><span class="badge badge-rechazada">${e.nombre}</span></c:when>
                                        <c:when test="${e.nombre == 'En Discusion'}"><span class="badge badge-discusion">${e.nombre}</span></c:when>
                                        <c:otherwise><span class="badge badge-pendiente">${e.nombre}</span></c:otherwise>
                                    </c:choose>
                                </c:if>
                            </c:forEach>
                        </td>
                        <td><a href="${pageContext.request.contextPath}/propuestas/detalle?id=${p.idPropuesta}">Ver detalle</a></td>
                    </tr>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </tbody>
</table>

</body>
</html>