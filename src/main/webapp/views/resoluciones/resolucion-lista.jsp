<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Resoluciones - AIR</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        h1 { color: #2c3e50; }
        table { width: 100%; border-collapse: collapse; background: white; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        th, td { border-bottom: 1px solid #eee; padding: 12px; text-align: left; }
        th { background: #34495e; color: white; }
        .alert-success { background: #d4edda; color: #155724; padding: 12px; border-radius: 4px; margin-bottom: 16px; }
    </style>
</head>
<body>

<h1>Resoluciones Emitidas</h1>

<c:if test="${param.emitida == '1'}">
    <div class="alert-success">Resolucion emitida correctamente.</div>
</c:if>

<table>
    <thead>
        <tr>
            <th>Numero</th>
            <th>Fecha emision</th>
            <th>Acciones</th>
        </tr>
    </thead>
    <tbody>
        <c:choose>
            <c:when test="${empty resoluciones}">
                <tr><td colspan="3" style="text-align: center; color: #999;">No hay resoluciones emitidas.</td></tr>
            </c:when>
            <c:otherwise>
                <c:forEach var="r" items="${resoluciones}">
                    <tr>
                        <td><strong>${r.numeroResolucion}</strong></td>
                        <td>${r.fechaEmision}</td>
                        <td><a href="${pageContext.request.contextPath}/resoluciones/detalle?id=${r.idResolucion}">Ver detalle</a></td>
                    </tr>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </tbody>
</table>

</body>
</html>