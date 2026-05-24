<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Detalle de Sesion - AIR</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        h1, h2 { color: #2c3e50; }
        .card { background: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        table { width: 100%; border-collapse: collapse; }
        th, td { border-bottom: 1px solid #eee; padding: 10px; text-align: left; }
        th { background: #34495e; color: white; }
        .badge { display: inline-block; padding: 4px 10px; border-radius: 12px; font-size: 12px; }
        .badge-presente { background: #d4edda; color: #155724; }
        .badge-ausente  { background: #f8d7da; color: #721c24; }
        .badge-just     { background: #fff3cd; color: #856404; }
        .stat { font-size: 24px; color: #3498db; font-weight: bold; }
        .alert-success { background: #d4edda; color: #155724; padding: 12px; border-radius: 4px; margin-bottom: 16px; }
        .btn { padding: 6px 12px; background: #3498db; color: white; border: none; border-radius: 4px; cursor: pointer; }
    </style>
</head>
<body>
<a href="${pageContext.request.contextPath}/sesiones">&larr; Volver al listado</a>
<h1>Sesion ${sesion.numeroSesion}</h1>
<c:if test="${param.asistencia == '1'}">
    <div class="alert-success">Asistencia registrada.</div>
</c:if>
<c:if test="${param.agendaAgregada == '1'}">
    <div class="alert-success">Propuesta agregada a la agenda.</div>
</c:if>
<div class="card">
    <h2>Datos generales</h2>
    <p><strong>Fecha:</strong> ${sesion.fechaSesion}</p>
    <p><strong>Quorum requerido:</strong> ${sesion.quorumRequerido}</p>
    <p><strong>Total convocados:</strong> ${sesion.totalConvocados}</p>
    <p><strong>Presentes registrados:</strong> <span class="stat">${presentes}</span></p>
    <c:choose>
        <c:when test="${presentes >= sesion.quorumRequerido}">
            <span class="badge badge-presente">QUORUM ALCANZADO</span>
        </c:when>
        <c:otherwise>
            <span class="badge badge-ausente">QUORUM PENDIENTE</span>
        </c:otherwise>
    </c:choose>
</div>
<div class="card">
    <h2>Asistencia</h2>
    <table>
        <thead>
            <tr><th>Nombre</th><th>Cedula</th><th>Estado</th></tr>
        </thead>
        <tbody>
            <c:choose>
                <c:when test="${empty asistencia}">
                    <tr><td colspan="3" style="text-align: center; color: #999;">No hay asistencia registrada para esta sesion.</td></tr>
                </c:when>
                <c:otherwise>
                    <c:forEach var="a" items="${asistencia}">
                        <tr>
                            <td>${a.nombreAsambleista}</td>
                            <td>${a.cedula}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${a.estado == 'Presente'}">
                                        <span class="badge badge-presente">${a.estado}</span>
                                    </c:when>
                                    <c:when test="${a.estado == 'Ausente'}">
                                        <span class="badge badge-ausente">${a.estado}</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge badge-just">${a.estado}</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </tbody>
    </table>
</div>
</body>
</html>