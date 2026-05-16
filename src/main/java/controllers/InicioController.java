package controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Servlet TEMPORAL de la pantalla de inicio.
 *
 * Su proposito en el Sprint 2 es doble:
 *  1. Dar un destino al que redirigir tras un login exitoso.
 *  2. Demostrar que el middlewareAuth protege rutas internas:
 *     si se entra a /inicio sin sesion, redirige al login.
 *
 * En sprints posteriores esta pantalla se reemplaza por el
 * panel real del sistema. Por ahora solo muestra quien inicio
 * sesion, sus roles y permisos, y un boton de logout.
 */
@WebServlet(name = "InicioController", urlPatterns = {"/inicio"})
public class InicioController extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // PROTECCION DE RUTA INTERNA (criterio de aceptacion del
        // Issue #0). Si no hay sesion activa, middlewareAuth
        // redirige al login y este metodo se detiene aqui.
        if (!AuthController.middlewareAuth(req, resp)) {
            return;
        }

        HttpSession sesion = req.getSession(false);
        String username    = (String) sesion.getAttribute("username");
        List<String> roles    = (List<String>) sesion.getAttribute("roles");
        List<String> permisos = (List<String>) sesion.getAttribute("permisos");

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html lang='es'><head><meta charset='UTF-8'>");
        out.println("<title>Inicio - SGL-AIR</title>");
        out.println("<style>");
        out.println("body{font-family:'Segoe UI',sans-serif;margin:0;background:#ecf0f1;}");
        out.println(".barra{background:#1f2d3d;color:#fff;padding:1rem 2rem;");
        out.println("display:flex;justify-content:space-between;align-items:center;}");
        out.println(".contenido{padding:2rem;}");
        out.println(".tarjeta{background:#fff;padding:1.5rem;border-radius:6px;");
        out.println("max-width:600px;box-shadow:0 2px 8px rgba(0,0,0,0.1);}");
        out.println(".salir{color:#fff;background:#c0392b;padding:0.5rem 1rem;");
        out.println("text-decoration:none;border-radius:4px;font-size:0.85rem;}");
        out.println("h2{margin-top:0;color:#1f2d3d;}");
        out.println("ul{color:#34495e;}");
        out.println("</style></head><body>");

        out.println("<div class='barra'>");
        out.println("<span>Sistema de Gestion Legislativa AIR</span>");
        out.println("<a class='salir' href='" + req.getContextPath() +
                     "/auth/logout'>Cerrar sesion</a>");
        out.println("</div>");

        out.println("<div class='contenido'><div class='tarjeta'>");
        out.println("<h2>Sesion iniciada correctamente</h2>");
        out.println("<p><strong>Usuario:</strong> " + username + "</p>");

        out.println("<p><strong>Roles:</strong></p><ul>");
        if (roles != null) {
            for (String r : roles) out.println("<li>" + r + "</li>");
        }
        out.println("</ul>");

        out.println("<p><strong>Permisos:</strong></p><ul>");
        if (permisos != null) {
            for (String p : permisos) out.println("<li>" + p + "</li>");
        }
        out.println("</ul>");

        out.println("</div></div>");
        out.println("</body></html>");
    }
}
