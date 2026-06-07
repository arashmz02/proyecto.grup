<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>AIR - Dashboard Administrativo</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/resources/css/certificacion.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js">
    </script>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/air-unificado.css">
</head>
<body>
<div class="air-bar"><span class="marca">SGL-AIR &middot; Asamblea Institucional Representativa</span><nav><a href="${pageContext.request.contextPath}/inicio">Inicio</a><a href="${pageContext.request.contextPath}/auth/logout" class="salir">Salir</a></nav></div>
<div class="page-container">

    <c:if test="${not empty errorBD}">
        <div style="background:#fff3cd;border:1px solid #ffc107;
                    padding:10px 16px;border-radius:6px;
                    margin-bottom:16px;color:#856404;">
            <strong>Aviso:</strong> <c:out value="${errorBD}"/>
        </div>
    </c:if>

    <%-- Metricas resumen --%>
    <div class="metricas-row">
        <div class="metrica-card">
            <div class="numero">
                <c:out value="${totalCertificaciones}" default="-"/>
            </div>
            <div class="etiqueta">Total certificaciones emitidas</div>
        </div>
        <div class="metrica-card">
            <div class="numero">
                <c:out value="${totalAsambleistas}" default="-"/>
            </div>
            <div class="etiqueta">Asambleistas registrados</div>
        </div>
        <div class="metrica-card" style="background:#003d7a;color:#fff;">
            <div class="numero" style="color:#fff;">AIR</div>
            <div class="etiqueta" style="color:#adc8f0;">
                Sistema de Gestion Normativa
            </div>
        </div>
    </div>

    <%-- Graficos --%>
    <div class="graficos-row">
        <div class="grafico-card">
            <canvas id="graficoPorMes" height="200"></canvas>
        </div>
        <div class="grafico-card">
            <canvas id="graficoPorSector" height="200"></canvas>
        </div>
    </div>

    <div class="graficos-row">
        <div class="grafico-card" style="flex:0.5;">
            <canvas id="graficoPorAnio" height="180"></canvas>
        </div>
    </div>

    <%-- Exportacion --%>
    <div class="export-section">
        <h3>Exportar datos</h3>

        <h4 style="color:#495057;margin-bottom:12px;font-size:0.95rem;">
            Historial completo de certificaciones (.xlsx)
        </h4>
        <form method="GET"
              action="${pageContext.request.contextPath}/api/export/certificaciones">
            <div class="export-form">
                <div>
                    <label style="display:block;font-size:0.85rem;
                                  color:#003d7a;font-weight:bold;
                                  margin-bottom:4px;">Desde:</label>
                    <input type="date" name="fechaDesde"
                           style="padding:7px 12px;border:1px solid #ced4da;
                                  border-radius:6px;"/>
                </div>
                <div>
                    <label style="display:block;font-size:0.85rem;
                                  color:#003d7a;font-weight:bold;
                                  margin-bottom:4px;">Hasta:</label>
                    <input type="date" name="fechaHasta"
                           style="padding:7px 12px;border:1px solid #ced4da;
                                  border-radius:6px;"/>
                </div>
                <button type="submit" class="btn-primary-tec">
                    Descargar Excel
                </button>
            </div>
        </form>

        <hr style="border:none;border-top:1px solid #e0e0e0;margin:20px 0;"/>

        <h4 style="color:#495057;margin-bottom:12px;font-size:0.95rem;">
            Aportes de un asambleista individual (.xlsx)
        </h4>
        <div style="display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap;">
            <div>
                <label style="display:block;font-size:0.85rem;
                               color:#003d7a;font-weight:bold;
                               margin-bottom:4px;">
                    Buscar asambleista:
                </label>
                <div class="search-input-wrapper" style="width:280px;">
                    <input type="text" id="inputExport"
                           placeholder="Nombre o cedula"
                           autocomplete="off"/>
                    <div class="autocomplete-list" id="listaExport"></div>
                </div>
                <input type="hidden" id="exportId"/>
            </div>
            <button type="button" id="btnExport"
                    class="btn-primary-tec" disabled>
                Descargar Excel individual
            </button>
        </div>
    </div>

</div>

<script src="${pageContext.request.contextPath}/resources/js/autocomplete-dashboard.js">
</script>
<script src="${pageContext.request.contextPath}/resources/js/charts.js"></script>
<script>
    document.addEventListener('DOMContentLoaded', function () {
        if (typeof inicializarDashboard === 'function') {
            inicializarDashboard('${pageContext.request.contextPath}');
        }

        inicializarBuscador({
            contextPath: '${pageContext.request.contextPath}',
            inputId: 'inputExport',
            listaId: 'listaExport',
            hiddenId: 'exportId',
            btnConsultarId: 'btnExport'
        });

        document.getElementById('btnExport').addEventListener('click', function () {
            const id = document.getElementById('exportId').value;
            if (!id) { alert('Seleccione un asambleista primero.'); return; }
            window.location.href = '${pageContext.request.contextPath}' +
                '/api/export/asambleista?asambleistaId=' + encodeURIComponent(id);
        });
    });
</script>

</body>
</html>
