package controllers;

import models.Certificacion;
import models.CertificacionAnulacionDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * AnulacionController - Issue #15 (Frank)
 *
 * Expone los endpoints HTTP para anular certificaciones y para
 * la verificacion publica de folios.
 *
 * Rutas:
 *   GET  /certificaciones/anular     -> formulario de anulacion
 *   POST /certificaciones/anular     -> procesa la anulacion
 *   GET  /certificaciones/verificar  -> verificacion publica
 *
 * Seguridad:
 *   - Anular requiere autenticacion y permiso ANULAR_CERTIFICACION
 *   - Verificar es publica (cualquier persona puede consultar un
 *     folio para confirmar autenticidad)
 *
 * La logica de validacion legal vive en los SPs en BD. Aqui solo
 * hacemos validaciones de forma para devolver errores claros al
 * usuario antes de llamar a los SPs.
 */
@WebServlet(name = "AnulacionController", urlPatterns = {
    "/certificaciones/anular",
    "/certificaciones/verificar"
})
public class AnulacionController extends HttpServlet {

    private final CertificacionAnulacionDAO dao = new CertificacionAnulacionDAO();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String ruta = req.getServletPath();

        if ("/certificaciones/verificar".equals(ruta)) {
            // Verificacion publica: NO requiere login
            verificarFolio(req, resp);
            return;
        }

        if ("/certificaciones/anular".equals(ruta)) {
            if (!AuthController.middlewareAuth(req, resp)) return;
            mostrarFormulario(req, resp);
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!AuthController.middlewareAuth(req, resp)) return;

        String ruta = req.getServletPath();

        if ("/certificaciones/anular".equals(ruta)) {
            procesarAnulacion(req, resp);
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }


    /* ====================================================
       ACCION: mostrar formulario de anulacion
       ==================================================== */
    private void mostrarFormulario(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Si el usuario llego con un folio en la URL, lo precargamos
        String folioPrecargado = req.getParameter("folio");

        if (folioPrecargado != null && !folioPrecargado.isBlank()) {
            try {
                Certificacion c = dao.obtenerPorFolio(folioPrecargado.trim());
                if (c != null) {
                    req.setAttribute("certificacion", c);
                } else {
                    req.setAttribute("error",
                        "No existe una certificacion con el folio " + folioPrecargado);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                req.setAttribute("error", "Error al consultar el folio.");
            }
        }

        req.getRequestDispatcher("/views/certificaciones/anular.jsp")
           .forward(req, resp);
    }


    /* ====================================================
       ACCION: procesar la anulacion (POST)
       ==================================================== */
    private void procesarAnulacion(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String folio  = trim(req.getParameter("folio"));
        String motivo = trim(req.getParameter("motivo"));

        // Validaciones de forma
        String error = validarCampos(folio, motivo);
        if (error != null) {
            volverConError(req, resp, folio, motivo, error);
            return;
        }

        // Obtener id del usuario logueado
        HttpSession httpSesion = req.getSession(false);
        Integer idUsuario = (Integer) httpSesion.getAttribute("idUsuario");

        if (idUsuario == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                "Sesion expirada. Vuelva a iniciar sesion.");
            return;
        }

        try {
            // Verificar que existe y esta Activa antes de llamar al SP
            Certificacion c = dao.obtenerPorFolio(folio);
            if (c == null) {
                volverConError(req, resp, folio, motivo,
                    "No existe una certificacion con el folio " + folio + ".");
                return;
            }
            if (!c.estaActiva()) {
                volverConError(req, resp, folio, motivo,
                    "La certificacion " + folio + " no esta en estado Activa. " +
                    "Estado actual: " + c.getNombreEstado());
                return;
            }

            // Llamar al SP
            dao.anular(folio, motivo, idUsuario);

            // Redireccion con mensaje de exito
            resp.sendRedirect(req.getContextPath() +
                "/certificaciones/verificar?folio=" + folio + "&anulado=1");

        } catch (SQLException e) {
            e.printStackTrace();
            volverConError(req, resp, folio, motivo,
                "Error al anular la certificacion: " + e.getMessage());
        }
    }


    /* ====================================================
       ACCION: verificacion publica de folio
       ==================================================== */
    private void verificarFolio(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String folio = trim(req.getParameter("folio"));

        if (folio == null || folio.isBlank()) {
            // Mostrar pagina vacia con el formulario de busqueda
            req.getRequestDispatcher("/views/certificaciones/verificar.jsp")
               .forward(req, resp);
            return;
        }

        try {
            Certificacion c = dao.verificarFolio(folio);

            if (c == null) {
                req.setAttribute("error",
                    "No se encontro ninguna certificacion con el folio " + folio);
            } else {
                req.setAttribute("certificacion", c);
            }

            // Mostrar mensaje si veniamos de una anulacion exitosa
            if ("1".equals(req.getParameter("anulado"))) {
                req.setAttribute("mensajeExito",
                    "Certificacion anulada correctamente.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            req.setAttribute("error", "Error al consultar el folio.");
        }

        req.setAttribute("folioBusqueda", folio);
        req.getRequestDispatcher("/views/certificaciones/verificar.jsp")
           .forward(req, resp);
    }


    /* ============== HELPERS ============== */

    private String validarCampos(String folio, String motivo) {
        if (folio == null || folio.isBlank())
            return "El folio es obligatorio.";
        if (!folio.matches("^DAIR-[0-9]{1,4}-[0-9]{4}$"))
            return "El folio debe tener formato DAIR-NNN-AAAA (ej. DAIR-001-2026).";
        if (motivo == null || motivo.isBlank())
            return "El motivo de anulacion es obligatorio.";
        if (motivo.length() < 10)
            return "El motivo debe tener al menos 10 caracteres para justificar la anulacion.";
        if (motivo.length() > 500)
            return "El motivo no puede exceder 500 caracteres.";
        return null;
    }


    private void volverConError(HttpServletRequest req, HttpServletResponse resp,
                                String folio, String motivo, String mensaje)
            throws ServletException, IOException {

        req.setAttribute("error", mensaje);
        req.setAttribute("folioPrev", folio);
        req.setAttribute("motivoPrev", motivo);

        req.getRequestDispatcher("/views/certificaciones/anular.jsp")
           .forward(req, resp);
    }


    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
