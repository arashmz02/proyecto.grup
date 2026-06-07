<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Sesiones - AIR</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        h1 { color: #2c3e50; }
        table { width: 100%; border-collapse: collapse; background: white; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        th, td { border-bottom: 1px solid #eee; padding: 12px; text-align: left; }
        th { background: #34495e; color: white; }
        tr:hover { background: #f9f9f9; }
        .btn { display: inline-block; padding: 8px 16px; background: #3498db; color: white; text-decoration: none; border-radius: 4px; }
        .btn:hover { background: #2980b9; }
        .alert-success { background: #d4edda; color: #155724; padding: 12px; border-radius: 4px; margin-bottom: 16px; }
        .alert-error { background: #f8d7da; color: #721c24; padding: 12px; border-radius: 4px; margin-bottom: 16px; }
    </style>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/air-unificado.css">
</head>
<body>
<div class="air-bar"><span class="marca">SGL-AIR &middot; Asamblea Institucional Representativa</span><nav><a href="${pageContext.request.contextPath}/inicio">Inicio</a><a href="${pageContext.request.contextPath}/auth/logout" class="salir">Salir</a></nav></div>
<h1>Sesiones de la AIR</h1>
<c:if test="${param.creado == '1'}">
    <div class="alert-success">Sesion registrada exitosamente.</div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert-error">${error}</div>
</c:if>
<p>
    <a href="${pageContext.request.contextPath}/sesiones/nueva" class="btn">+ Nueva Sesion</a>
</p>
<table>
    <thead>
        <tr>
            <th>Numero</th>
            <th>Fecha</th>
            <th>Quorum</th>
            <th>Convocados</th>
            <th>Acciones</th>
        </tr>
    </thead>
    <tbody>
        <c:choose>
            <c:when test="${empty sesiones}">
                <tr><td colspan="5" style="text-align: center; color: #999;">No hay sesiones registradas.</td></tr>
            </c:when>
            <c:otherwise>
                <c:forEach var="s" items="${sesiones}">
                    <tr>
                        <td><strong>${s.numeroSesion}</strong></td>
                        <td>${s.fechaSesion}</td>
                        <td>${s.quorumRequerido}</td>
                        <td>${s.totalConvocados}</td>
                        <td>
                            <a href="${pageContext.request.contextPath}/sesiones/detalle?id=${s.idSesion}">Ver detalle</a>
                        </td>
                    </tr>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </tbody>
</table>
</body>
</html>