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
 * Muestra al usuario sus roles, permisos y todos los modulos del
 * sistema agrupados por area. La seguridad efectiva la aplica cada
 * controlador destino con middlewareAuth; este menu solo navega.
 */
@WebServlet(name = "InicioController", urlPatterns = {"/inicio"})
public class InicioController extends HttpServlet {
 
    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
 
        if (!AuthController.middlewareAuth(req, resp)) {
            return;
        }
 
        HttpSession sesion = req.getSession(false);
        String username       = (String) sesion.getAttribute("username");
        List<String> roles    = (List<String>) sesion.getAttribute("roles");
        List<String> permisos = (List<String>) sesion.getAttribute("permisos");
 
        String ctx = req.getContextPath();
 
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();
 
        out.println("<!DOCTYPE html>");
        out.println("<html lang='es'><head><meta charset='UTF-8'>");
        out.println("<meta name='viewport' content='width=device-width, initial-scale=1'>");
        out.println("<title>Inicio - SGL-AIR</title>");
        out.println("<style>");
        out.println("*{box-sizing:border-box;margin:0;padding:0;}");
        out.println("body{font-family:'Segoe UI',sans-serif;background:#ecf0f1;}");
        out.println(".barra{background:#1f2d3d;color:#fff;padding:1rem 2rem;");
        out.println("display:flex;justify-content:space-between;align-items:center;}");
        out.println(".contenido{padding:2rem;max-width:1000px;margin:0 auto;}");
        out.println(".tarjeta{background:#fff;padding:1.5rem;border-radius:6px;");
        out.println("margin-bottom:1.25rem;box-shadow:0 2px 8px rgba(0,0,0,0.05);}");
        out.println(".salir{color:#fff;background:#c0392b;padding:0.5rem 1rem;");
        out.println("text-decoration:none;border-radius:4px;font-size:0.85rem;}");
        out.println("h2{margin-bottom:0.75rem;color:#1f2d3d;font-size:1.2rem;}");
        out.println("ul{color:#34495e;margin-left:1.5rem;margin-top:0.5rem;}");
        out.println(".modulos{display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));");
        out.println("gap:1rem;margin-top:1rem;}");
        out.println(".modulo{display:block;padding:1.1rem 1.25rem;background:#2980b9;");
        out.println("color:#fff;text-decoration:none;border-radius:6px;transition:background 0.15s;}");
        out.println(".modulo:hover{background:#2471a3;}");
        out.println(".modulo.verde{background:#27ae60;}.modulo.verde:hover{background:#1e8449;}");
        out.println(".modulo.gris{background:#566573;}.modulo.gris:hover{background:#46505c;}");
        out.println(".modulo .titulo{font-weight:600;font-size:0.98rem;margin-bottom:0.25rem;}");
        out.println(".modulo .descripcion{font-size:0.82rem;opacity:0.9;}");
        out.println("</style></head><body>");
 
        out.println("<div class='barra'>");
        out.println("<span>Sistema de Gestion Legislativa AIR</span>");
        out.println("<a class='salir' href='" + ctx + "/auth/logout'>Cerrar sesion</a>");
        out.println("</div>");
 
        out.println("<div class='contenido'>");
 
        // ---- Tarjeta de identidad ----
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
 
        // ---- Asamblea y Sesiones ----
        out.println("<div class='tarjeta'>");
        out.println("<h2>Asamblea y Sesiones</h2>");
        out.println("<div class='modulos'>");
        modulo(out, ctx, "/asambleistas", "", "Padron de Asambleistas",
               "Registro de asambleistas y sus nombramientos.");
        modulo(out, ctx, "/sesiones", "", "Sesiones Plenarias",
               "Convocatorias, agenda y registro de sesiones.");
        modulo(out, ctx, "/views/sesiones/quorum.jsp", "", "Control de Quorum",
               "Verificacion de quorum y resumen de asistencia (Issue #11).");
        modulo(out, ctx, "/propuestas", "", "Propuestas",
               "Gestion de propuestas de reforma normativa.");
        modulo(out, ctx, "/resoluciones", "", "Resoluciones",
               "Resoluciones emitidas por la asamblea.");
        out.println("</div></div>");
 
        // ---- Normativa y Certificaciones ----
        out.println("<div class='tarjeta'>");
        out.println("<h2>Normativa y Certificaciones</h2>");
        out.println("<div class='modulos'>");
        modulo(out, ctx, "/views/normativa/arbol.jsp", "", "Normativa Institucional",
               "Arbol jerarquico del Estatuto Organico y sus reformas.");
        modulo(out, ctx, "/folios", "", "Generacion de Folios",
               "Folios unicos para certificaciones con numeracion atomica.");
        modulo(out, ctx, "/certificaciones/historial", "", "Historial de Certificaciones",
               "Certificaciones emitidas y su estado actual.");
        modulo(out, ctx, "/certificaciones/anular", "verde", "Anular Certificacion",
               "Anulacion con justificacion y sustitucion de folios (Issue #15).");
        modulo(out, ctx, "/certificaciones/verificar", "verde", "Verificacion Publica",
               "Validar la autenticidad de un folio emitido (Issue #15).");
        out.println("</div></div>");
 
        // ---- Auditoria y Reportes ----
        out.println("<div class='tarjeta'>");
        out.println("<h2>Auditoria y Reportes</h2>");
        out.println("<div class='modulos'>");
        modulo(out, ctx, "/bitacora/certificaciones", "gris", "Bitacora de Auditoria",
               "Trazabilidad de cambios con sello de tiempo y hash (Issue #13).");
        modulo(out, ctx, "/reportes/dashboard", "gris", "Dashboard de Reportes",
               "Indicadores y estadisticas del sistema.");
        out.println("</div></div>");
 
        out.println("</div></body></html>");
    }
 
    /** Renderiza una tarjeta-enlace de modulo. clase puede ser "", "verde" o "gris". */
    private static void modulo(PrintWriter out, String ctx, String ruta,
                               String clase, String titulo, String descripcion) {
        String cls = clase.isEmpty() ? "modulo" : "modulo " + clase;
        out.println("<a class='" + cls + "' href='" + ctx + ruta + "'>");
        out.println("<div class='titulo'>" + escapar(titulo) + "</div>");
        out.println("<div class='descripcion'>" + escapar(descripcion) + "</div></a>");
    }
 
    private static String escapar(String s) {
        if (s == null) return "";
        return s.replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#39;");
    }
}
 