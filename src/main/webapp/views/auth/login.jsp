<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
    Vista de login - Issue #0
    Sistema de Gestion Legislativa AIR

    REGLA DE ORO DEL MVC (seccion 2.3 del plan):
    Esta vista NO contiene logica de negocio. No valida
    credenciales, no consulta la base de datos, no calcula nada.
    Solo muestra el formulario y envia los datos por POST al
    AuthController, que es quien decide que hacer.

    El unico atributo que lee es 'error', que el controlador
    coloca en el request cuando el login falla.
--%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Iniciar sesion - SGL-AIR</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #1f2d3d;
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            color: #2c3e50;
        }

        .tarjeta-login {
            background: #ffffff;
            width: 100%;
            max-width: 380px;
            padding: 2.5rem 2rem;
            border-radius: 6px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
        }

        .tarjeta-login h1 {
            font-size: 1.25rem;
            text-align: center;
            margin-bottom: 0.25rem;
            color: #1f2d3d;
        }

        .tarjeta-login .subtitulo {
            text-align: center;
            font-size: 0.8rem;
            color: #7f8c8d;
            margin-bottom: 1.75rem;
        }

        .campo {
            margin-bottom: 1.1rem;
        }

        .campo label {
            display: block;
            font-size: 0.8rem;
            margin-bottom: 0.35rem;
            color: #34495e;
        }

        .campo input {
            width: 100%;
            padding: 0.6rem 0.75rem;
            border: 1px solid #cfd8dc;
            border-radius: 4px;
            font-size: 0.95rem;
        }

        .campo input:focus {
            outline: none;
            border-color: #2980b9;
        }

        .boton-login {
            width: 100%;
            padding: 0.7rem;
            background: #2980b9;
            color: #ffffff;
            border: none;
            border-radius: 4px;
            font-size: 0.95rem;
            cursor: pointer;
            margin-top: 0.5rem;
        }

        .boton-login:hover {
            background: #2471a3;
        }

        .mensaje-error {
            background: #fdecea;
            border: 1px solid #f5c6cb;
            color: #c0392b;
            font-size: 0.82rem;
            padding: 0.6rem 0.75rem;
            border-radius: 4px;
            margin-bottom: 1.1rem;
        }
    </style>
</head>
<body>

    <div class="tarjeta-login">
        <h1>Sistema de Gestion Legislativa AIR</h1>
        <p class="subtitulo">Asamblea Institucional Representativa - ITCR</p>

        <%-- Mensaje de error: solo aparece si el controlador lo coloco --%>
        <% if (request.getAttribute("error") != null) { %>
            <div class="mensaje-error">
                <%= request.getAttribute("error") %>
            </div>
        <% } %>

        <%-- El form envia por POST a /auth/login.
             El action usa el context path para funcionar sin
             importar como se despliegue la aplicacion. --%>
        <form method="post" action="<%= request.getContextPath() %>/auth/login">

            <div class="campo">
                <label for="username">Usuario</label>
                <input type="text" id="username" name="username"
                       autocomplete="username" required autofocus>
            </div>

            <div class="campo">
                <label for="password">Contrasena</label>
                <input type="password" id="password" name="password"
                       autocomplete="current-password" required>
            </div>

            <button type="submit" class="boton-login">Iniciar sesion</button>
        </form>
    </div>

</body>
</html>
