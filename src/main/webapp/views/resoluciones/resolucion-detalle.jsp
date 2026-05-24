<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Detalle Resolucion - AIR</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        h1, h2 { color: #2c3e50; }
        .card { background: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        table { width: 100%; border-collapse: collapse; }
        th, td { border-bottom: 1px solid #eee; padding: 10px; text-align: left; }
        th { background: #34495e; color: white; }
        .badge { display: inline-block; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: bold; background: #d1ecf1; color: #0c5460; }
        .texto-comparativo { background: #f8f9fa; padding: 12px; border-left: 4px solid #3498db; margin-top: 8px; }
        .texto-anterior { border-left-color: #e74c3c; }
        .alert-success { background: #d4edda; color: #155724; padding: 12px; border-radius: 4px; margin-bottom: 16px; }
    </style>
</head>
<body>

<a href="${pageContext.request.contextPath}/resoluciones">&larr; Volver al listado</a>

<h1>Resolucion ${resolucion.numeroResolucion}</h1>

<c:if test="${param.reformaAplicada == '1'}">
    <div class="alert-success">Reforma aplicada correctamente. El reglamento ha sido versionado.</div>
</c:if>

<div class="card">
    <h2>Datos generales</h2>
    <p><strong>Numero oficial:</strong> ${resolucion.numeroResolucion}</p>
    <p><strong>Fecha emision:</strong> ${resolucion.fechaEmision}</p>
    <p><strong>ID Punto de agenda:</strong> ${resolucion.idPuntoAgenda}</p>
</div>

<div class="card">
    <h2>Reformas aplicadas al reglamento</h2>
    <c:choose>
        <c:when test="${empty reformas}">
            <p style="color:#999;">Esta resolucion no ha aplicado reformas todavia.</p>
        </c:when>
        <c:otherwise>
            <c:forEach var="r" items="${reformas}">
                <div style="border-bottom: 2px solid #eee; padding: 16px 0;">
                    <p>
                        <strong>Elemento normativo:</strong> Articulo/Inciso ${r.numeroEtiqueta}
                        <span class="badge">${r.tipoReforma}</span>
                    </p>
                    <p><strong>Vigente desde:</strong> ${r.fechaInicioVigencia}</p>

                    <c:if test="${not empty r.textoAnterior}">
                        <p><strong>Texto anterior:</strong></p>
                        <div class="texto-comparativo texto-anterior">${r.textoAnterior}</div>
                    </c:if>

                    <p><strong>Texto nuevo:</strong></p>
                    <div class="texto-comparativo">${r.textoNuevo}</div>
                </div>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</div>

</body>
</html>