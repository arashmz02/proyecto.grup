package controllers;

import services.ExportService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/export/*")
public class ExportController extends HttpServlet {

    private final ExportService exportService = new ExportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo();

        if ("/certificaciones".equals(pathInfo)) {
            exportarCertificaciones(req, resp);
        } else if ("/asambleista".equals(pathInfo)) {
            exportarAsambleista(req, resp);
        } else {
            resp.sendError(404, "Tipo de exportacion no reconocido.");
        }
    }

    private void exportarCertificaciones(HttpServletRequest req,
                                          HttpServletResponse resp)
            throws IOException {
        String desde = req.getParameter("fechaDesde");
        String hasta = req.getParameter("fechaHasta");
        try {
            byte[] archivo = exportService.exportarCertificaciones(desde, hasta);
            enviarExcel(resp, archivo, "certificaciones_AIR.xlsx");
        } catch (Exception e) {
            resp.sendError(500, "Error generando Excel: " + e.getMessage());
        }
    }

    private void exportarAsambleista(HttpServletRequest req,
                                      HttpServletResponse resp)
            throws IOException {
        String idStr = req.getParameter("asambleistaId");
        if (idStr == null || idStr.isEmpty()) {
            resp.sendError(400, "Parametro asambleistaId requerido.");
            return;
        }
        try {
            int id = Integer.parseInt(idStr);
            byte[] archivo = exportService.exportarAportesPorAsambleista(id);
            enviarExcel(resp, archivo, "aportes_" + id + ".xlsx");
        } catch (NumberFormatException e) {
            resp.sendError(400, "asambleistaId debe ser un numero entero.");
        } catch (Exception e) {
            resp.sendError(500, "Error generando Excel: " + e.getMessage());
        }
    }

    private void enviarExcel(HttpServletResponse resp, byte[] datos,
                              String nombreArchivo) throws IOException {
        resp.setContentType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition",
            "attachment; filename=\"" + nombreArchivo + "\"");
        resp.setContentLength(datos.length);
        resp.getOutputStream().write(datos);
    }
}