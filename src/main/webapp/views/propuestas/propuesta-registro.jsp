<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Nueva Propuesta - AIR</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        h1 { color: #2c3e50; }
        form { background: white; padding: 24px; border-radius: 8px; max-width: 700px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        label { display: block; margin-top: 14px; font-weight: bold; color: #34495e; }
        input, select, textarea { width: 100%; padding: 8px; margin-top: 4px; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; }
        textarea { resize: vertical; min-height: 80px; }
        select[multiple] { min-height: 140px; }
        .btn { padding: 10px 20px; background: #3498db; color: white; border: none; border-radius: 4px; cursor: pointer; margin-top: 20px; }
        .btn-secondary { background: #95a5a6; text-decoration: none; display: inline-block; }
        .alert-error { background: #f8d7da; color: #721c24; padding: 12px; border-radius: 4px; margin-bottom: 16px; }
        .help { font-size: 12px; color: #7f8c8d; margin-top: 2px; }
    </style>
</head>
<body>

<h1>Registrar Nueva Propuesta</h1>

<c:if test="${not empty error}">
    <div class="alert-error">${error}</div>
</c:if>

<form method="POST" action="${pageContext.request.contextPath}/propuestas/nueva">

    <label>Codigo AIR:</label>
    <input type="text" name="codigoAir" value="${codigoAirPrev}" placeholder="AIR-99-2024" required>
    <div class="help">Formato: AIR-NN-AAAA o AIR-NN-CONC-AAAA</div>

    <label>Titulo:</label>
    <input type="text" name="titulo" value="${tituloPrev}" maxlength="300" required>

    <label>Texto sustitutivo:</label>
    <textarea name="textoSustitutivo">${textoSustitutivoPrev}</textarea>

    <label>Reglamento base:</label>
    <select name="idReglamentoBase">
        <option value="">-- Ninguno --</option>
        <c:forEach var="r" items="${reglamentos}">
            <option value="${r.id}" <c:if test="${idReglamentoBasePrev == r.id}">selected</c:if>>${r.nombre}</option>
        </c:forEach>
    </select>

    <label>Etapa:</label>
    <select name="idEtapaPropuesta" required>
        <option value="">-- Seleccione --</option>
        <c:forEach var="e" items="${etapas}">
            <option value="${e.id}" <c:if test="${idEtapaPrev == e.id}">selected</c:if>>${e.nombre}</option>
        </c:forEach>
    </select>

    <label>Estado inicial:</label>
    <select name="idEstadoPropuesta" required>
        <option value="">-- Seleccione --</option>
        <c:forEach var="e" items="${estados}">
            <option value="${e.id}" <c:if test="${idEstadoPrev == e.id}">selected</c:if>>${e.nombre}</option>
        </c:forEach>
    </select>

    <label>Tipo de mayoria requerida:</label>
    <select name="idTipoMayoria" required>
        <option value="">-- Seleccione --</option>
        <c:forEach var="m" items="${mayorias}">
            <option value="${m.id}" <c:if test="${idMayoriaPrev == m.id}">selected</c:if>>${m.nombre}</option>
        </c:forEach>
    </select>

    <label>Link de documentacion:</label>
    <input type="url" name="linkDocumentacion" value="${linkDocPrev}" placeholder="https://...">

    <label>Proponentes (mantenga Ctrl para seleccionar varios):</label>
    <select name="idProponente" multiple required>
        <c:forEach var="a" items="${asambleistas}">
            <option value="${a.id}">${a.nombre}</option>
        </c:forEach>
    </select>
    <div class="help">Debe seleccionar al menos uno.</div>

    <button type="submit" class="btn">Guardar Propuesta</button>
    <a href="${pageContext.request.contextPath}/propuestas" class="btn btn-secondary">Cancelar</a>
</form>

</body>
</html>