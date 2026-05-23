<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Detalle Propuesta - AIR</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        h1, h2 { color: #2c3e50; }
        .card { background: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        table { width: 100%; border-collapse: collapse; }
        th, td { border-bottom: 1px solid #eee; padding: 10px; text-align: left; }
        th { background: #34495e; color: white; }
        .badge { display: inline-block; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: bold; }
        .alert-success { background: #d4edda; color: #155724; padding: 12px; border-radius: 4px; margin-bottom: 16px; }
        form { display: inline-block; margin-top: 12px; }
        select, button { padding: 6px 12px; border: 1px solid #ddd; border-radius: 4px; }
        button { background: #3498db; color: white; border: none; cursor: pointer; }
    </style>
</head>
<body>

<a href="${pageContext.request.contextPath}/propuestas">&larr; Volver al buscador</a>

<h1>${propuesta.codigoAir}</h1>

<c:if test="${param.estadoCambiado == '1'}">
    <div class="alert-success">Estado actualizado correctamente.</div>
</c:if>

<div class="card">
    <h2>Datos generales</h2>
    <p><strong>Titulo:</strong> ${propuesta.titulo}</p>
    <c:if test="${not empty propuesta.textoSustitutivo}">
        <p><strong>Texto sustitutivo:</strong></p>
        <p style="background: #f8f9fa; padding: 12px; border-left: 4px solid #3498db;">${propuesta.textoSustitutivo}</p>
    </c:if>
    <c:if test="${propuesta.conciliada}">
        <p><span class="badge" style="background:#d1ecf1; color:#0c5460;">PROPUESTA CONCILIADA</span></p>
    </c:if>
    <c:if test="${not empty propuesta.linkDocumentacion}">
        <p><strong>Documentacion:</strong> <a href="${propuesta.linkDocumentacion}" target="_blank">${propuesta.linkDocumentacion}</a></p>
    </c:if>

    <h3>Cambiar estado</h3>
    <form method="POST" action="${pageContext.request.contextPath}/propuestas/cambiar-estado">
        <input type="hidden" name="idPropuesta" value="${propuesta.idPropuesta}">
        <select name="idNuevoEstado" required>
            <option value="">-- Nuevo estado --</option>
            <c:forEach var="e" items="${estados}">
                <c:if test="${e.id != propuesta.idEstadoPropuesta}">
                    <option value="${e.id}">${e.nombre}</option>
                </c:if>
            </c:forEach>
        </select>
        <button type="submit">Cambiar</button>
    </form>
</div>

<div class="card">
    <h2>Proponentes</h2>
    <table>
        <thead><tr><th>Nombre</th><th>Cedula</th></tr></thead>
        <tbody>
            <c:choose>
                <c:when test="${empty proponentes}">
                    <tr><td colspan="2" style="text-align:center; color:#999;">Sin proponentes registrados.</td></tr>
                </c:when>
                <c:otherwise>
                    <c:forEach var="p" items="${proponentes}">
                        <tr><td>${p.nombre}</td><td>${p.cedula}</td></tr>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </tbody>
    </table>
</div>

<div class="card">
    <h2>Historial de estados</h2>
    <table>
        <thead><tr><th>Fecha</th><th>Etapa</th><th>Estado</th><th>Usuario</th></tr></thead>
        <tbody>
            <c:forEach var="h" items="${historial}">
                <tr>
                    <td>${h.fechaModificacion}</td>
                    <td>${h.etapa}</td>
                    <td>${h.estado}</td>
                    <td>${h.usuario}</td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</div>

<c:if test="${not empty conciliadas}">
    <div class="card">
        <h2>Propuestas conciliadas (derivadas de esta)</h2>
        <table>
            <thead><tr><th>Codigo</th><th>Titulo</th><th>Acciones</th></tr></thead>
            <tbody>
                <c:forEach var="c" items="${conciliadas}">
                    <tr>
                        <td>${c.codigoAir}</td>
                        <td>${c.titulo}</td>
                        <td><a href="${pageContext.request.contextPath}/propuestas/detalle?id=${c.idPropuesta}">Ver</a></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</c:if>

</body>
</html>