<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Anular Certificacion - AIR</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/estilos.css">
    <style>
        .contenedor-anular {
            max-width: 700px;
            margin: 40px auto;
            padding: 30px;
            background: #fff;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        .alerta-error {
            background: #f8d7da; color: #721c24;
            border-left: 4px solid #dc3545;
            padding: 12px; margin-bottom: 20px; border-radius: 4px;
        }
        .alerta-warning {
            background: #fff3cd; color: #856404;
            border-left: 4px solid #ffc107;
            padding: 12px; margin-bottom: 20px; border-radius: 4px;
        }
        .info-cert {
            background: #e7f3ff; padding: 15px;
            border-radius: 4px; margin-bottom: 20px;
        }
        .info-cert dt { font-weight: bold; }
        .info-cert dd { margin-left: 20px; margin-bottom: 8px; }
        .form-grupo { margin-bottom: 18px; }
        .form-grupo label { display: block; font-weight: bold; margin-bottom: 6px; }
        .form-grupo input, .form-grupo textarea {
            width: 100%; padding: 8px; border: 1px solid #ccc;
            border-radius: 4px; font-size: 14px;
        }
        .form-grupo textarea { min-height: 100px; resize: vertical; }
        .form-ayuda { font-size: 12px; color: #666; margin-top: 4px; }
        .botones { display: flex; gap: 10px; justify-content: flex-end; }
        .btn { padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; }
        .btn-peligro { background: #dc3545; color: white; }
        .btn-cancelar { background: #6c757d; color: white; text-decoration: none; }
    </style>
</head>
<body>
    <div class="contenedor-anular">
        <h1>Anular Certificacion</h1>
        <p style="color:#666">
            La anulacion de una certificacion es <strong>irreversible</strong>.
            El folio no se reutiliza y queda marcado como invalido en la
            pagina de verificacion publica.
        </p>

        <%-- Mensaje de error si lo hay --%>
        <c:if test="${not empty error}">
            <div class="alerta-error">
                <strong>Error:</strong> ${error}
            </div>
        </c:if>

        <%-- Info de la certificacion si fue precargada --%>
        <c:if test="${not empty certificacion}">
            <div class="info-cert">
                <h3 style="margin-top:0">Certificacion encontrada</h3>
                <dl>
                    <dt>Folio:</dt>
                    <dd>${certificacion.folioUnico}</dd>
                    <dt>Asambleista:</dt>
                    <dd>${certificacion.nombreAsambleista} (cedula ${certificacion.cedula})</dd>
                    <dt>Fecha de emision:</dt>
                    <dd>${certificacion.fechaEmision}</dd>
                    <dt>Estado actual:</dt>
                    <dd><strong>${certificacion.nombreEstado}</strong></dd>
                </dl>

                <c:if test="${not certificacion.estaActiva()}">
                    <div class="alerta-warning">
                        Esta certificacion no esta en estado Activa, no puede anularse.
                    </div>
                </c:if>
            </div>
        </c:if>

        <%-- Formulario --%>
        <form method="POST" action="${pageContext.request.contextPath}/certificaciones/anular">

            <div class="form-grupo">
                <label for="folio">Folio a anular:</label>
                <input type="text" id="folio" name="folio"
                       value="${empty folioPrev ? (certificacion.folioUnico) : folioPrev}"
                       placeholder="DAIR-001-2026"
                       pattern="^DAIR-[0-9]{1,4}-[0-9]{4}$"
                       required>
                <div class="form-ayuda">Formato: DAIR-NNN-AAAA</div>
            </div>

            <div class="form-grupo">
                <label for="motivo">Motivo de la anulacion:</label>
                <textarea id="motivo" name="motivo"
                          minlength="10" maxlength="500"
                          placeholder="Describa el motivo por el cual se anula la certificacion (minimo 10 caracteres)"
                          required>${motivoPrev}</textarea>
                <div class="form-ayuda">
                    Entre 10 y 500 caracteres. Este motivo queda registrado en la
                    bitacora de auditoria (Art. 301 LGAP).
                </div>
            </div>

            <div class="botones">
                <a href="${pageContext.request.contextPath}/certificaciones/historial"
                   class="btn btn-cancelar">Cancelar</a>
                <button type="submit" class="btn btn-peligro"
                        onclick="return confirm('Esta accion es irreversible. Confirma anular el folio?')">
                    Anular Certificacion
                </button>
            </div>
        </form>
    </div>
</body>
</html>
