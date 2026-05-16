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
 * Servlet de la pantalla de inicio (post-login).
 *
 * Responsabilidades:
 *  1. Dar un destino al que redirigir tras un login exitoso.
 *  2. Demostrar que el middlewareAuth protege rutas internas:
 *     si se entra a /inicio sin sesion, redirige al login.
 *  3. Mostrar al usuario las acciones disponibles segun los
 *     permisos que tenga asignados (defensa en profundidad: el
 *     enlace solo aparece si la sesion tiene el permiso, y
 *     ademas cada controlador valida el permiso en su middleware).
 */
@WebServlet(name = "InicioController", urlPatterns = {"/inicio"})
public class InicioController extends HttpServlet {

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // PROTECCION DE RUTA INTERNA (criterio de aceptacion del Issue #0).
        // Si no hay sesion activa, middlewareAuth redirige al login.
        if (!AuthController.middlewareAuth(req, resp)) {
            return;
        }

        HttpSession sesion = req.getSession(false);
        String username       = (String) sesion.getAttribute("username");
        List<String> roles    = (List<String>) sesion.getAttribute("roles");
        List<String> permisos = (List<String>) sesion.getAttribute("permisos");

        // Calcular que modulos puede ver
        boolean puedeVerAsambleistas =
            permisos != null && (permisos.contains("REGISTRAR_ASAMBLEISTAS")
                              || permisos.contains("CONSULTAR_NORMATIVA"));
        // (Cuando existan modulos de Josue y Arash, se agregan aqui:
        //  puedeVerNormativa, puedeVerCertificaciones, puedeGestionarUsuarios, etc.)

        String ctx = req.getContextPath();

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html lang='es'><head><meta charset='UTF-8'>");
        out.println("<title>Inicio - SGL-AIR</title>");
        out.println("<style>");
        out.println("*{box-sizing:border-box;margin:0;padding:0;}");
        out.println("body{font-family:'Segoe UI',sans-serif;background:#ecf0f1;}");
        out.println(".barra{background:#1f2d3d;color:#fff;padding:1rem 2rem;");
        out.println("display:flex;justify-content:space-between;align-items:center;}");
        out.println(".contenido{padding:2rem;max-width:900px;margin:0 auto;}");
        out.println(".tarjeta{background:#fff;padding:1.5rem;border-radius:6px;");
        out.println("margin-bottom:1.25rem;box-shadow:0 2px 8px rgba(0,0,0,0.05);}");
        out.println(".salir{color:#fff;background:#c0392b;padding:0.5rem 1rem;");
        out.println("text-decoration:none;border-radius:4px;font-size:0.85rem;}");
        out.println("h2{margin-bottom:0.75rem;color:#1f2d3d;font-size:1.2rem;}");
        out.println("ul{color:#34495e;margin-left:1.5rem;margin-top:0.5rem;}");
        out.println(".modulos{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));");
        out.println("gap:1rem;margin-top:1rem;}");
        out.println(".modulo{display:block;padding:1.1rem 1.25rem;background:#2980b9;");
        out.println("color:#fff;text-decoration:none;border-radius:6px;transition:background 0.15s;}");
        out.println(".modulo:hover{background:#2471a3;}");
        out.println(".modulo .titulo{font-weight:600;font-size:0.98rem;margin-bottom:0.25rem;}");
        out.println(".modulo .descripcion{font-size:0.82rem;opacity:0.9;}");
        out.println(".vacio{color:#7f8c8d;font-style:italic;font-size:0.9rem;}");
        out.println("</style></head><body>");

        // --- Barra superior ---
        out.println("<div class='barra'>");
        out.println("<span>Sistema de Gestion Legislativa AIR</span>");
        out.println("<a class='salir' href='" + ctx + "/auth/logout'>Cerrar sesion</a>");
        out.println("</div>");

        out.println("<div class='contenido'>");

        // --- Tarjeta de identidad ---
        out.println("<div class='tarjeta'>");
        out.println("<h2>Bienvenido, " + escapar(username) + "</h2>");

        out.println("<p><strong>Roles:</strong></p><ul>");
        if (roles != null) {
            for (String r : roles) out.println("<li>" + escapar(r) + "</li>");
        }
        out.println("</ul>");

        out.println("<p style='margin-top:0.75rem;'><strong>Permisos:</strong></p><ul>");
        if (permisos != null) {
            for (String p : permisos) out.println("<li>" + escapar(p) + "</li>");
        }
        out.println("</ul>");
        out.println("</div>");

        // --- Tarjeta de modulos disponibles ---
        out.println("<div class='tarjeta'>");
        out.println("<h2>Acciones disponibles</h2>");

        boolean hayAlgo = puedeVerAsambleistas;

        if (!hayAlgo) {
            out.println("<p class='vacio'>");
            out.println("Tu rol actual no tiene modulos asociados a\u00fan.");
            out.println("</p>");
        } else {
            out.println("<div class='modulos'>");

            if (puedeVerAsambleistas) {
                out.println("<a class='modulo' href='" + ctx + "/asambleistas'>");
                out.println("<div class='titulo'>Padron de asambleistas</div>");
                out.println("<div class='descripcion'>");
                out.println("Consultar y gestionar el registro de asambleistas y sus nombramientos.");
                out.println("</div></a>");
            }

            // Aqui se agregan los modulos de Josue y Arash cuando esten listos:
            //   if (puedeVerNormativa)        ... /reglamentos
            //   if (puedeVerCertificaciones)  ... /certificaciones
            //   if (puedeGestionarUsuarios)   ... /usuarios

            out.println("</div>");
        }

        out.println("</div>");

        out.println("</div></body></html>");
    }


    /**
     * Escapa caracteres HTML basicos para evitar XSS si el
     * username o rol contuviera caracteres especiales.
     */
    private static String escapar(String s) {
        if (s == null) return "";
        return s.replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#39;");
    }
}
