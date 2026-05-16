<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html>
<head>
    <title>Generación de Folios</title>
</head>
<body>

<h1>Generar Folio</h1>

<form action="folios" method="post">

    <label>Tipo de Documento:</label>
    <input type="text" name="tipoDocumento" required>

    <br><br>

    <label>Año:</label>
    <input type="number" name="anio" required>

    <br><br>

    <label>Observación:</label>
    <input type="text" name="observacion">

    <br><br>

    <button type="submit">
        Generar Folio
    </button>

</form>

<%
    String folioGenerado =
            (String) request.getAttribute("folioGenerado");

    if (folioGenerado != null) {
%>

    <h2>
        Folio generado:
        <%= folioGenerado %>
    </h2>

<%
    }
%>

</body>
</html>