<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>AIR — Historial de Certificaciones</title>
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
        <h2>Historial de Certificaciones Emitidas</h2>

        <c:if test="${not empty mensajeError}">
            <div style="background:#fff3cd;border:1px solid #ffc107;
                        padding:10px 16px;border-radius:6px;
                        margin-bottom:16px;color:#856404;">
                <strong>Aviso:</strong> <c:out value="${mensajeError}"/>
            </div>
        </c:if>

        <form method="GET"
              action="${pageContext.request.contextPath}/certificaciones/historial">
            <div style="display:flex;gap:16px;flex-wrap:wrap;
                        align-items:flex-end;margin-bottom:20px;">
                <div>
                    <label style="display:block;font-weight:bold;color:#003d7a;
                                  margin-bottom:4px;font-size:0.85rem;">
                        Fecha desde:
                    </label>
                    <input type="date" name="fechaDesde"
                           value="${filtroFechaDesde}"
                           style="padding:7px 12px;border:1px solid #ced4da;
                                  border-radius:6px;"/>
                </div>
                <div>
                    <label style="display:block;font-weight:bold;color:#003d7a;
                                  margin-bottom:4px;font-size:0.85rem;">
                        Fecha hasta:
                    </label>
                    <input type="date" name="fechaHasta"
                           value="${filtroFechaHasta}"
                           style="padding:7px 12px;border:1px solid #ced4da;
                                  border-radius:6px;"/>
                </div>
                <div>
                    <label style="display:block;font-weight:bold;color:#003d7a;
                                  margin-bottom:4px;font-size:0.85rem;">
                        Folio:
                    </label>
                    <input type="text" name="consecutivo"
                           value="${filtroConsecutivo}"
                           placeholder="Ej: DAIR-009-2025"
                           style="padding:7px 12px;border:1px solid #ced4da;
                                  border-radius:6px;width:180px;"/>
                </div>
                <div>
                    <label style="display:block;font-weight:bold;color:#003d7a;
                                  margin-bottom:4px;font-size:0.85rem;">
                        Nombre del funcionario:
                    </label>
                    <input type="text" name="buscaNombre"
                           value="${filtroBuscaNombre}"
                           placeholder="Ej: Ana Rosa Ruiz"
                           style="padding:7px 12px;border:1px solid #ced4da;
                                  border-radius:6px;width:200px;"/>
                </div>
                <div style="display:flex;gap:8px;">
                    <button type="submit" class="btn-primary-tec">
                        Filtrar
                    </button>
                    <a href="${pageContext.request.contextPath}/certificaciones/historial"
                       class="btn-secondary-tec"
                       style="text-decoration:none;display:inline-block;">
                        Limpiar
                    </a>
                </div>
            </div>
        </form>

        <c:choose>
            <c:when test="${empty registros and empty mensajeError}">
                <div style="text-align:center;padding:30px;color:#6c757d;">
                    No se encontraron certificaciones con los filtros aplicados.
                </div>
            </c:when>
            <c:when test="${not empty registros}">
                <div style="margin-bottom:10px;color:#6c757d;font-size:0.9rem;">
                    <strong>${registros.size()}</strong> registro(s) encontrado(s).
                </div>
                <div style="overflow-x:auto;">
                    <table class="table-historial">
                        <thead>
                            <tr>
                                <th>Folio</th>
                                <th>Funcionario</th>
                                <th>Cédula</th>
                                <th>Fecha emisión</th>
                                <th>Emitido por</th>
                                <th>Estado</th>
                                <th>Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="cert" items="${registros}">
                                <tr>
                                    <td>
                                        <strong>
                                            <c:out value="${cert.folio}"/>
                                        </strong>
                                    </td>
                                    <td>
                                        <c:out value="${cert.nombreAsambleista}"/>
                                    </td>
                                    <td>
                                        <c:out value="${cert.cedula}"/>
                                    </td>
                                    <td>
                                        <c:out value="${cert.fechaEmision}"/>
                                    </td>
                                    <td>
                                        <c:out value="${cert.usuarioSecretaria}"/>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${cert.estado == 'Activo'}">
                                                <span class="badge-activo">
                                                    ACTIVO
                                                </span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge-anulado">
                                                    ANULADO
                                                </span>
                                                <c:if test="${not empty cert.motivoAnulacion}">
                                                    <br>
                                                    <small style="color:#6c757d;">
                                                        <c:out value="${cert.motivoAnulacion}"/>
                                                    </small>
                                                </c:if>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:if test="${cert.estado == 'Activo'}">
                                            <a href="${pageContext.request.contextPath}/api/certificaciones/reimprimir?id=${cert.id}"
                                               target="_blank"
                                               style="color:#003d7a;font-size:0.85rem;">
                                                Re-imprimir
                                            </a>
                                        </c:if>
                                        <c:if test="${cert.estado == 'Anulado'}">
                                            <span style="color:#aaa;font-size:0.85rem;">
                                                Anulado
                                            </span>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
        </c:choose>

    </div>
</div>

</body>
</html>