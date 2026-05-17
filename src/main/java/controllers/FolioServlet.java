package controllers;

import models.FolioDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * FolioServlet - Issue #1 (Arash)
 *
 * Atiende dos rutas:
 *   GET  /folios -> muestra el formulario
 *   POST /folios -> genera el folio y vuelve a mostrar el formulario
 *                   con el folio generado abajo
 *
 * Requiere autenticacion. Requiere permiso EMITIR_CERTIFICACION
 * para usar el formulario.
 */
@WebServlet(name = "FolioServlet", urlPatterns = {"/folios"})
public class FolioServlet extends HttpServlet {

    private FolioDAO folioDAO;

    @Override
    public void init() {
        folioDAO = new FolioDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!AuthController.middlewareAuth(req, resp)) return;
        if (!AuthController.middlewarePermiso(req, resp, "EMITIR_CERTIFICACION")) return;

        req.getRequestDispatcher("/views/folios.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!AuthController.middlewareAuth(req, resp)) return;
        if (!AuthController.middlewarePermiso(req, resp, "EMITIR_CERTIFICACION")) return;

        String tipoDocumento = req.getParameter("tipoDocumento");
        String anioStr       = req.getParameter("anio");
        String observacion   = req.getParameter("observacion");

        if (tipoDocumento == null || tipoDocumento.isBlank() ||
            anioStr == null || anioStr.isBlank()) {
            req.setAttribute("error", "Tipo de documento y anio son obligatorios.");
            req.getRequestDispatcher("/views/folios.jsp").forward(req, resp);
            return;
        }

        int anio;
        try {
            anio = Integer.parseInt(anioStr);
        } catch (NumberFormatException e) {
            req.setAttribute("error", "El anio debe ser un numero.");
            req.getRequestDispatcher("/views/folios.jsp").forward(req, resp);
            return;
        }

        HttpSession sesion = req.getSession(false);
        int idUsuario = (Integer) sesion.getAttribute("idUsuario");

        try {
            String folio = folioDAO.generarFolio(tipoDocumento, anio, observacion, idUsuario);

            if (folio == null) {
                req.setAttribute("error", "No se pudo generar el folio.");
            } else {
                req.setAttribute("folioGenerado", folio);
                req.setAttribute("tipoUsado",     tipoDocumento);
                req.setAttribute("anioUsado",     anio);
                req.setAttribute("obsUsada",      observacion);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            req.setAttribute("error", "Error al conectar con la base de datos.");
        }

        req.getRequestDispatcher("/views/folios.jsp").forward(req, resp);
    }
}