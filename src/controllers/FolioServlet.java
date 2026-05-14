package controllers;

import models.FolioDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/folios")
public class FolioServlet extends HttpServlet {

    private FolioDAO folioDAO;

    @Override
    public void init() {
        folioDAO = new FolioDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String tipoDocumento = request.getParameter("tipoDocumento");
        int anio = Integer.parseInt(request.getParameter("anio"));
        String observacion = request.getParameter("observacion");

        String folioGenerado = folioDAO.generarFolio(
                tipoDocumento,
                anio,
                observacion
        );

        request.setAttribute("folioGenerado", folioGenerado);

        request.getRequestDispatcher("/views/folios.jsp")
                .forward(request, response);
    }
}