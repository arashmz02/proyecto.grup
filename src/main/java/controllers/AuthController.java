package controllers;

import config.Conexion;
import models.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Controlador de la capa CONTROLADOR (MVC) para autenticacion.
 * Corresponde al Issue #0 (Gestion de Seguridad y Roles).
 *
 * Responsabilidades:
 *  - login:  valida credenciales y crea la sesion
 *  - logout: destruye la sesion de forma segura
 *  - middlewareAuth:    verifica que haya sesion activa
 *  - middlewarePermiso: verifica que el usuario tenga un permiso
 *
 * Rutas atendidas por este servlet:
 *  POST /auth/login   -> procesa el formulario de login
 *  GET  /auth/logout  -> cierra la sesion
 *  GET  /auth/login   -> muestra el formulario de login
 *
 * Los middlewares son metodos estaticos para que otros
 * controladores (ej. AsambleistaController) los reutilicen.
 */
@WebServlet(name = "AuthController", urlPatterns = {"/auth/login", "/auth/logout"})
public class AuthController extends HttpServlet {

    /* ============================================================
       GET: mostrar formulario de login o procesar logout
       ============================================================ */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String ruta = req.getServletPath();

        if ("/auth/logout".equals(ruta)) {
            logout(req, resp);
        } else {
            // Mostrar la vista de login
            req.getRequestDispatcher("/views/auth/login.jsp").forward(req, resp);
        }
    }

    /* ============================================================
       POST: procesar el formulario de login
       ============================================================ */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if ("/auth/login".equals(req.getServletPath())) {
            login(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }


    /* ============================================================
       LOGIN
       Recibe username y password, valida contra la BD y, si son
       correctos, crea la sesion con los datos del usuario.
       ============================================================ */
    private void login(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validacion minima de entrada
        if (username == null || username.isBlank() ||
            password == null || password.isBlank()) {
            reenviarConError(req, resp, "Debe ingresar usuario y contrasena.");
            return;
        }

        try {
            Usuario usuario = Usuario.obtenerPorUsername(username.trim());

            // Mensaje generico a proposito: no se le dice al
            // atacante si fallo el usuario o la contrasena.
            if (usuario == null || !usuario.validarPassword(password)) {
                reenviarConError(req, resp, "Usuario o contrasena incorrectos.");
                return;
            }

            if (!usuario.isActivo()) {
                reenviarConError(req, resp, "La cuenta esta desactivada.");
                return;
            }

            // Credenciales validas: crear sesion.
            // Se invalida cualquier sesion previa para evitar
            // ataques de fijacion de sesion (session fixation).
            HttpSession sesionPrevia = req.getSession(false);
            if (sesionPrevia != null) {
                sesionPrevia.invalidate();
            }
            HttpSession sesion = req.getSession(true);

            List<String> roles    = usuario.obtenerRoles();
            List<String> permisos = usuario.obtenerPermisos();

            sesion.setAttribute("idUsuario", usuario.getIdUsuario());
            sesion.setAttribute("username",  usuario.getUsername());
            sesion.setAttribute("roles",     roles);
            sesion.setAttribute("permisos",  permisos);

            // Tiempo de vida de la sesion: 30 minutos de inactividad
            sesion.setMaxInactiveInterval(30 * 60);

            // Redirige al inicio del sistema. Ajustar esta ruta
            // cuando exista la pantalla principal.
            resp.sendRedirect(req.getContextPath() + "/inicio");

        } catch (SQLException e) {
            // No se expone el detalle tecnico al usuario final.
            e.printStackTrace();
            reenviarConError(req, resp,
                "Error al conectar con la base de datos. Intente mas tarde.");
        }
    }


    /* ============================================================
       LOGOUT
       Destruye la sesion por completo. Tras esto, cualquier
       intento de acceso vuelve a requerir login.
       ============================================================ */
    private void logout(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        HttpSession sesion = req.getSession(false);
        if (sesion != null) {
            sesion.invalidate();   // elimina todos los atributos y el token
        }
        resp.sendRedirect(req.getContextPath() + "/auth/login");
    }


    /* ============================================================
       MIDDLEWARE: AUTENTICACION
       Verifica que exista una sesion activa con un usuario.
       Devuelve true si puede continuar; si no, redirige al login
       y devuelve false.

       Uso desde otro servlet:
         if (!AuthController.middlewareAuth(req, resp)) return;
       ============================================================ */
    public static boolean middlewareAuth(HttpServletRequest req,
                                         HttpServletResponse resp)
            throws IOException {

        HttpSession sesion = req.getSession(false);

        boolean autenticado = sesion != null
                && sesion.getAttribute("idUsuario") != null;

        if (!autenticado) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return false;
        }
        return true;
    }


    /* ============================================================
       MIDDLEWARE: PERMISO
       Verifica que el usuario autenticado tenga un permiso
       especifico. Asume que middlewareAuth ya se llamo antes.

       Uso desde otro servlet:
         if (!AuthController.middlewarePermiso(req, resp,
                 "REGISTRAR_ASAMBLEISTAS")) return;
       ============================================================ */
    @SuppressWarnings("unchecked")
    public static boolean middlewarePermiso(HttpServletRequest req,
                                            HttpServletResponse resp,
                                            String permisoRequerido)
            throws IOException {

        HttpSession sesion = req.getSession(false);

        if (sesion == null || sesion.getAttribute("permisos") == null) {
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return false;
        }

        List<String> permisos =
                (List<String>) sesion.getAttribute("permisos");

        if (permisos == null || !permisos.contains(permisoRequerido)) {
            // 403 Forbidden: esta autenticado pero no autorizado
            resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "No tiene permiso para realizar esta accion.");
            return false;
        }
        return true;
    }


    /* ------------------------------------------------------------
       Utilidad interna: reenvia al formulario de login con un
       mensaje de error para mostrar al usuario.
       ------------------------------------------------------------ */
    private void reenviarConError(HttpServletRequest req,
                                  HttpServletResponse resp,
                                  String mensaje)
            throws ServletException, IOException {

        req.setAttribute("error", mensaje);
        req.getRequestDispatcher("/views/auth/login.jsp").forward(req, resp);
    }
}
