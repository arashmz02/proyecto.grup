<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>AIR — Previsualización de Certificación</title>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/resources/css/certificacion.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/air-unificado.css">
</head>
<body>
<div class="air-bar"><span class="marca">SGL-AIR &middot; Asamblea Institucional Representativa</span><nav><a href="${pageContext.request.contextPath}/inicio">Inicio</a><a href="${pageContext.request.contextPath}/auth/logout" class="salir">Salir</a></nav></div>
<div class="page-container">

    <%-- Barra de acciones --%>
    <div class="no-print" style="display:flex;justify-content:space-between;
                                 align-items:center;margin-bottom:20px;">
        <a href="${pageContext.request.contextPath}/views/certificaciones/buscador.jsp"
           class="btn-secondary-tec" style="text-decoration:none;">
            ← Volver al buscador
        </a>
        <div style="display:flex;gap:12px;">
            <button onclick="window.print()" class="btn-secondary-tec">
                Imprimir vista previa
            </button>
            <c:if test="${sessionScope.rolNombre == 'Secretaria'
                          or sessionScope.rolNombre == 'Administrador'}">
                <form method="POST"
                      action="${pageContext.request.contextPath}/api/certificaciones/emitir"
                      style="display:inline;">
                    <input type="hidden" name="asambleistaId"
                           value="${param.asambleistaId}"/>
                    <input type="hidden" name="fechaDesde"
                           value="${param.fechaDesde}"/>
                    <input type="hidden" name="fechaHasta"
                           value="${param.fechaHasta}"/>
                    <button type="submit" class="btn-success-tec">
                        ✓ Generar Certificación Oficial
                    </button>
                </form>
            </c:if>
        </div>
    </div>

    <%-- Aviso de previsualización --%>
    <div class="no-print"
         style="background:#fff3cd;border:1px solid #ffc107;padding:10px 16px;
                border-radius:6px;margin-bottom:20px;color:#856404;">
        <strong>Vista previa</strong> — Este documento NO tiene validez legal aún.
        El folio único (DAIR-XXX-AAAA) y el hash de seguridad se asignan
        al presionar "Generar Certificación Oficial".
    </div>

    <%-- Cuerpo del certificado --%>
    <div class="cert-preview-wrapper">

        <%-- Encabezado institucional --%>
        <div class="cert-header">
            <div style="display:flex;gap:16px;align-items:center;">
                <div class="logo-text">AIR</div>
                <div style="font-size:0.85rem;color:#555;">
                    Asamblea Institucional<br>Representativa
                </div>
            </div>
            <div style="text-align:right;">
                <div style="font-weight:bold;color:#003d7a;">TEC</div>
                <div style="font-size:0.8rem;color:#555;">
                    Tecnológico de Costa Rica
                </div>
            </div>
        </div>

        <%-- Título --%>
        <div class="cert-title-block">
            <div class="org-name">
                Directorio de la Asamblea Institucional Representativa
            </div>
            <div class="doc-type" style="margin-top:6px;">Constancia</div>
            <div class="cert-folio-placeholder">
                FOLIO: [ se asignará al emitir oficialmente — DAIR-XXX-2026 ]
            </div>
        </div>

        <%-- Cuerpo --%>
        <div class="cert-body">

            <div class="cert-section">
                El/La
                <strong>
                    <c:out value="${sessionScope.usuarioNombre}"
                           default="Presidente(a) del Directorio"/>
                </strong>,
                en su calidad de presidente(a) del Directorio de la Asamblea
                Institucional Representativa, hace constar que:
            </div>

            <div class="cert-nombre-destacado">
                <c:choose>
                    <c:when test="${not empty requestScope.nombreAsambleista}">
                        <c:out value="${requestScope.nombreAsambleista}"/>
                    </c:when>
                    <c:otherwise>
                        [ Asambleísta ID: <c:out value="${param.asambleistaId}"/> ]
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="cert-cedula">
                Cédula de identidad N°
                <c:out value="${requestScope.cedulaAsambleista}"
                       default="[ pendiente ]"/>
            </div>

            <div class="cert-section">
                De acuerdo con los registros de la Secretaría de la AIR,
                asume la siguiente representación ante la Asamblea Institucional
                Representativa:
                <ul style="margin-top:8px;">
                    <li>
                        <em>
                            Sector:
                            <c:out value="${requestScope.sectorAsambleista}"
                                   default="[ sector ]"/>,
                            a partir del año
                            <c:out value="${requestScope.anioInicio}"
                                   default="[ año ]"/>
                            y su nombramiento
                            <c:choose>
                                <c:when test="${requestScope.esVigente == true}">
                                    se encuentra vigente.
                                </c:when>
                                <c:otherwise>
                                    finalizó el
                                    <c:out value="${requestScope.fechaFinNombramiento}"
                                           default="[ fecha ]"/>.
                                </c:otherwise>
                            </c:choose>
                        </em>
                    </li>
                </ul>
            </div>

            <div class="cert-section">
                Según consta en nuestros registros de asistencia, se reporta
                su participación en
                <strong>
                    <c:out value="${requestScope.totalSesionesAsistidas}"
                           default="[ N ]"/>
                </strong>
                sesiones convocadas para este período
                <c:if test="${not empty requestScope.totalSesionesConvocadas}">
                    de un total de
                    <strong>
                        <c:out value="${requestScope.totalSesionesConvocadas}"/>
                    </strong>
                </c:if>.
            </div>

            <c:if test="${not empty requestScope.participaciones}">
                <div class="cert-section">
                    También participó activamente en el trabajo de las
                    siguientes comisiones:
                    <c:forEach var="p" items="${requestScope.participaciones}"
                               varStatus="st">
                        <div style="margin-top:14px;border-left:3px solid #003d7a;
                                    padding-left:12px;">
                            Propuesta base <c:out value="${st.index + 1}"/>:
                            <em>"<c:out value="${p.tituloPropuesta}"/>"</em>,
                            aprobada en la sesión
                            <c:out value="${p.numeroSesion}"/>
                            del <c:out value="${p.fechaSesion}"/>.
                            <c:if test="${not empty p.integrantesComision}">
                                <br>La comisión fue integrada por:
                                <c:out value="${p.integrantesComision}"/>.
                            </c:if>
                            <c:if test="${not empty p.notaCondicional}">
                                <br>
                                <small style="color:#666;font-style:italic;">
                                    Nota: <c:out value="${p.notaCondicional}"/>
                                </small>
                            </c:if>
                        </div>
                    </c:forEach>
                </div>
            </c:if>

            <c:if test="${empty requestScope.participaciones}">
                <div class="cert-section no-print"
                     style="color:#856404;background:#fff3cd;
                            padding:10px;border-radius:4px;font-style:italic;">
                    [ Las participaciones serán cargadas por el
                    ReporteController — Issue #17 de Arash ]
                </div>
            </c:if>

        </div>

        <%-- Cierre legal Art. 301 LGAP --%>
        <div class="cert-footer">
            <p>
                Se extiende la presente certificación a solicitud de parte
                interesada, en el ejercicio de las potestades conferidas al
                Directorio de la Asamblea Institucional Representativa del
                Instituto Tecnológico de Costa Rica.
            </p>
            <p>
                Lo anterior bajo juramento, consciente de las penas establecidas
                en el artículo 301 de la Ley General de la Administración Pública
                y sus reformas.
            </p>
            <div style="margin-top:50px;">
                <div class="cert-firma-space">
                    <c:out value="${sessionScope.usuarioNombre}"
                           default="Presidente(a) del Directorio AIR"/>
                    <br>Directorio de la Asamblea Institucional Representativa
                </div>
            </div>
        </div>

    </div>
</div>

</body>
</html>