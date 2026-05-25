<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>AIR — Buscador de Certificaciones</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/resources/css/certificacion.css">
</head>
<body>

<nav style="background:#003d7a;padding:12px 28px;color:#fff;
            font-weight:bold;font-size:1.1rem;">
    AIR — Sistema de Gestión Normativa
    <span style="float:right;font-weight:normal;font-size:0.9rem;">
        <c:out value="${sessionScope.usuarioNombre}" default="Usuario"/>
    </span>
</nav>

<div class="page-container">
    <div class="card-panel">
        <h2>Módulo de Certificaciones — Búsqueda</h2>

        <c:if test="${not empty param.error}">
            <div style="background:#f8d7da;border:1px solid #f5c6cb;
                        padding:10px 16px;border-radius:6px;
                        margin-bottom:16px;color:#721c24;">
                <c:out value="${param.error}"/>
            </div>
        </c:if>

        <div style="margin-bottom:24px;">
            <label style="display:block;font-weight:bold;color:#003d7a;
                          margin-bottom:8px;">
                Buscar asambleísta (nombre o cédula):
            </label>
            <div class="search-input-wrapper">
                <input type="text" id="inputBusqueda"
                       placeholder="Ej: Ana Rosa Ruiz o 3-0248-0440"
                       autocomplete="off"/>
                <div class="autocomplete-list" id="listaAutocompletado"></div>
            </div>
            <input type="hidden" id="asambleistaId" value=""/>
        </div>

        <div class="info-card-asambleista" id="infoAsambleista">
            <div class="info-row">
                <div class="info-item">
                    <label>Nombre</label>
                    <span id="infoNombre">—</span>
                </div>
                <div class="info-item">
                    <label>Cédula</label>
                    <span id="infoCedula">—</span>
                </div>
                <div class="info-item">
                    <label>Sector</label>
                    <span id="infoSector">—</span>
                </div>
                <div class="info-item">
                    <label>Desde</label>
                    <span id="infoFechaInicio">—</span>
                </div>
                <div class="info-item">
                    <label>Estado</label>
                    <span id="infoEstado">—</span>
                </div>
            </div>
        </div>

        <div style="margin-top:28px;margin-bottom:24px;">
            <p style="font-weight:bold;color:#003d7a;margin-bottom:12px;">
                Rango de fechas:
            </p>
            <div class="date-range-group">
                <div class="form-group">
                    <label for="fechaDesde">Desde:</label>
                    <input type="date" id="fechaDesde"/>
                </div>
                <div class="form-group">
                    <label for="fechaHasta">Hasta:</label>
                    <input type="date" id="fechaHasta"/>
                </div>
                <div class="form-group">
                    <label>&nbsp;</label>
                    <button type="button" id="btnTodoHistorial"
                            class="btn-secondary-tec" style="width:100%;">
                        Todo el historial
                    </button>
                </div>
            </div>
        </div>

        <div style="text-align:right;margin-top:20px;">
            <button type="button" id="btnConsultar"
                    class="btn-primary-tec" disabled>
                Consultar historial →
            </button>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/resources/js/autocomplete.js"></script>
<script>
    inicializarBuscador({
        contextPath: '${pageContext.request.contextPath}',
        inputId: 'inputBusqueda',
        listaId: 'listaAutocompletado',
        hiddenId: 'asambleistaId',
        btnConsultarId: 'btnConsultar',
        infoCardId: 'infoAsambleista',
        infoNombreId: 'infoNombre',
        infoCedulaId: 'infoCedula',
        infoSectorId: 'infoSector',
        infoFechaInicioId: 'infoFechaInicio',
        infoEstadoId: 'infoEstado',
        fechaDesdeId: 'fechaDesde',
        fechaHastaId: 'fechaHasta',
        urlConsultar: '${pageContext.request.contextPath}/views/certificaciones/preview.jsp'
    });

    document.getElementById('btnTodoHistorial').addEventListener('click', function () {
        document.getElementById('fechaDesde').value = '';
        document.getElementById('fechaHasta').value = '';
    });
</script>

</body>
</html>