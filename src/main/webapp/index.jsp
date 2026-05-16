<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
    Pagina raiz de la aplicacion.
    Su unica funcion es redirigir al login. La logica real de
    autenticacion vive en el AuthController.
--%>
<% response.sendRedirect(request.getContextPath() + "/auth/login"); %>
