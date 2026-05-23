<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Nueva Sesion - AIR</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        h1 { color: #2c3e50; }
        form { background: white; padding: 24px; border-radius: 8px; max-width: 600px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        label { display: block; margin-top: 14px; font-weight: bold; color: #34495e; }
        input, select { width: 100%; padding: 8px; margin-top: 4px; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; }
        .btn { padding: 10px 20px; background: #3498db; color: white; border: none; border-radius: 4px; cursor: pointer; margin-top: 20px; }
        .btn:hover { background: #2980b9; }
        .btn-secondary { background: #95a5a6; }
        .alert-error { background: #f8d7da; color: #721c24; padding: 12px; border-radius: 4px; margin-bottom: 16px; }
        .help { font-size: 12px; color: #7f8c8d; margin-top: 2px; }
    </style>
</head>
<body>

<h1>Registrar Nueva Sesion</h1>

<c:if test="${not empty error}">
    <div class="alert-error">${error}</div>
</c:if>

<form method="POST" action="${pageContext.request.contextPath}/sesiones/nueva">

    <label>Numero de sesion:</label>
    <input type="text" name="numeroSesion" value="${numeroSesionPrev}" placeholder="AIR-110-2024" required>
    <div class="help">Formato: AIR-NNN-AAAA</div>

    <label>Fecha:</label>
    <input type="date" name="fecha" value="${fechaPrev}" required>

    <label>Tipo de sesion:</label>
    <select name="idTipoSesion" required>
        <option value="">-- Seleccione --</option>
        <c:forEach var="t" items="${tiposSesion}">
            <option value="${t.id}" <c:if test="${idTipoSesionPrev == t.id}">selected</c:if>>${t.nombre}</option>
        </c:forEach>
    </select>

    <label>Modalidad:</label>
    <select name="idTipoModalidad" required>
        <option value="">-- Seleccione --</option>
        <c:forEach var="m" items="${modalidades}">
            <option value="${m.id}" <c:if test="${idTipoModalidadPrev == m.id}">selected</c:if>>${m.nombre}</option>
        </c:forEach>
    </select>

    <label>Quorum requerido:</label>
    <input type="number" name="quorumRequerido" value="${quorumPrev}" min="0" required>

    <button type="submit" class="btn">Guardar</button>
    <a href="${pageContext.request.contextPath}/sesiones" class="btn btn-secondary" style="text-decoration: none; display: inline-block;">Cancelar</a>
</form>

</body>
</html>