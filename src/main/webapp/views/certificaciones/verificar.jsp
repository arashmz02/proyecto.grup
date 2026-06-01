<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Verificar Certificacion - AIR</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/estilos.css">
    <style>
        .contenedor-verificar {
            max-width: 800px;
            margin: 40px auto;
            padding: 30px;
            background: #fff;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        .formulario-busqueda {
            display: flex;
            gap: 10px;
            margin-bottom: 30px;
        }
        .formulario-busqueda input {
            flex: 1;
            padding: 10px;
            border: 1px solid #ccc;
            border-radius: 4px;
            font-size: 16px;
        }
        .btn-verificar {
            padding: 10px 25px;
            background: #007bff;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 16px;
        }
        .resultado {
            padding: 25px;
            border-radius: 8px;
            margin-bottom: 20px;
        }
        .resultado-valido {
            background: #d4edda;
            border-left: 6px solid #28a745;
            color: #155724;
        }
        .resultado-invalido {
            background: #f8d7da;
            border-left: 6px solid #dc3545;
            color: #721c24;
        }
        .resultado-sustituida {
            background: #fff3cd;
            border-left: 6px solid #ffc107;
            color: #856404;
        }
        .resultado-no-encontrado {
            background: #e2e3e5;
            border-left: 6px solid #6c757d;
            color: #383d41;
        }
        .icono-estado {
            font-size: 48px;
            margin-bottom: 10px;
        }
        .datos-cert dt {
            font-weight: bold;
            margin-top: 12px;
        }
        .datos-cert dd {
            margin-left: 0;
            margin-bottom: 4px;
        }
        .mensaje-exito {
            background: #cce5ff;
            border-left: 4px solid #007bff;
            padding: 12px;
            margin-bottom: 20px;
            border-radius: 4px;
        }
        .nota-legal {
            font-size: 12px;
            color: #666;
            margin-top: 30px;
            padding-top: 20px;
            border-top: 1px solid #ddd;
        }
    </style>
</head>
<body>
    <div class="contenedor-verificar">
        <h1>Verificacion de Certificacion</h1>
        <p style="color:#666">
            Ingrese el folio impreso en la certificacion para confirmar su autenticidad.
            Esta consulta es publica y no requiere autenticacion.
        </p>

        <%-- Formulario de busqueda --%>
        <form method="GET" action="${pageContext.request.contextPath}/certificaciones/verificar"
              class="formulario-busqueda">
            <input type="text" name="folio"
                   value="${folioBusqueda}"
                   placeholder="Ingrese el folio (ej. DAIR-001-2026)"
                   pattern="^DAIR-[0-9]{1,4}-[0-9]{4}$"
                   required>
            <button type="submit" class="btn-verificar">Verificar</button>
        </form>

        <%-- Mensaje si veniamos de una anulacion exitosa --%>
        <c:if test="${not empty mensajeExito}">
            <div class="mensaje-exito">${mensajeExito}</div>
        </c:if>

        <%-- Caso: folio no encontrado --%>
        <c:if test="${not empty error}">
            <div class="resultado resultado-no-encontrado">
                <div class="icono-estado">?</div>
                <h2>Folio no encontrado</h2>
                <p>${error}</p>
                <p>Verifique que el folio este escrito correctamente o contacte
                   a la Secretaria de la AIR.</p>
            </div>
        </c:if>

        <%-- Caso: certificacion ACTIVA --%>
        <c:if test="${not empty certificacion and certificacion.estaActiva()}">
            <div class="resultado resultado-valido">
                <div class="icono-estado">VALIDO</div>
                <h2>Certificacion vigente</h2>
                <p>Este documento es <strong>autentico y vigente</strong> segun los
                   registros de la Asamblea Institucional Representativa.</p>
                <dl class="datos-cert">
                    <dt>Folio:</dt>
                    <dd>${certificacion.folioUnico}</dd>
                    <dt>Emitida a nombre de:</dt>
                    <dd>${certificacion.nombreAsambleista}</dd>
                    <dt>Cedula:</dt>
                    <dd>${certificacion.cedula}</dd>
                    <dt>Fecha de emision:</dt>
                    <dd>${certificacion.fechaEmision}</dd>
                </dl>
            </div>
        </c:if>

        <%-- Caso: certificacion ANULADA --%>
        <c:if test="${not empty certificacion and certificacion.estaAnulada()}">
            <div class="resultado resultado-invalido">
                <div class="icono-estado">INVALIDO</div>
                <h2>Certificacion ANULADA</h2>
                <p>Este folio fue <strong>anulado</strong> y NO debe considerarse
                   valido. Cualquier documento impreso con este folio carece
                   de fe publica.</p>
                <dl class="datos-cert">
                    <dt>Folio:</dt>
                    <dd>${certificacion.folioUnico}</dd>
                    <dt>Fecha de anulacion:</dt>
                    <dd>${certificacion.fechaAnulacion}</dd>
                    <dt>Motivo:</dt>
                    <dd>${certificacion.motivoAnulacion}</dd>
                </dl>
            </div>
        </c:if>

        <%-- Caso: certificacion SUSTITUIDA --%>
        <c:if test="${not empty certificacion and certificacion.estaSustituida()}">
            <div class="resultado resultado-sustituida">
                <div class="icono-estado">SUSTITUIDA</div>
                <h2>Certificacion sustituida</h2>
                <p>Este folio fue <strong>sustituido</strong> por una version
                   corregida. Solicite el folio actualizado a la Secretaria
                   de la AIR.</p>
                <dl class="datos-cert">
                    <dt>Folio anterior:</dt>
                    <dd>${certificacion.folioUnico}</dd>
                    <dt>Fecha de sustitucion:</dt>
                    <dd>${certificacion.fechaAnulacion}</dd>
                    <dt>Motivo:</dt>
                    <dd>${certificacion.motivoAnulacion}</dd>
                </dl>
            </div>
        </c:if>

        <div class="nota-legal">
            <p>Esta verificacion se basa en los registros de la base de datos
               oficial del sistema AIR. La autenticidad del documento queda
               respaldada por el Hash SHA-256 generado al momento de emision
               (Art. 301 de la Ley General de la Administracion Publica).</p>
            <p><strong>Instituto Tecnologico de Costa Rica</strong> -
               Secretaria de la Asamblea Institucional Representativa</p>
        </div>
    </div>
</body>
</html>
