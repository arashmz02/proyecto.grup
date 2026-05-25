package controllers;

import config.Conexion;
import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

@WebServlet("/reportes/dashboard")
public class DashboardController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String tipo = req.getParameter("tipo");

        if ("json".equals(tipo)) {
            servirJson(req, resp);
        } else {
            cargarDashboard(req, resp);
        }
    }

    private void cargarDashboard(HttpServletRequest req,
                                  HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = Conexion.obtener()) {
            req.setAttribute("totalCertificaciones",
                contarTabla(conn, "certificacion_emitida"));
            req.setAttribute("totalAsambleistas",
                contarTabla(conn, "asambleista"));
        } catch (SQLException e) {
            req.setAttribute("errorBD",
                "Error cargando metricas: " + e.getMessage());
        }
        req.getRequestDispatcher("/views/reportes/dashboard.jsp")
           .forward(req, resp);
    }

    private void servirJson(HttpServletRequest req,
                             HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        Gson gson = new Gson();
        String metrica = req.getParameter("metrica");

        try (Connection conn = Conexion.obtener()) {
            Object datos;
            if ("porMes".equals(metrica)) {
                datos = certsPorMes(conn);
            } else if ("porSector".equals(metrica)) {
                datos = certsPorSector(conn);
            } else if ("porAnio".equals(metrica)) {
                datos = certsPorAnio(conn);
            } else {
                datos = Collections.singletonMap("error", "Metrica no reconocida");
            }
            out.print(gson.toJson(datos));
        } catch (SQLException e) {
            resp.setStatus(500);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private Map<String, Object> certsPorMes(Connection conn)
            throws SQLException {
        String sql =
            "SELECT FORMAT(fecha_emision, 'yyyy-MM') AS mes, " +
            "       COUNT(*) AS total " +
            "FROM certificacion_emitida " +
            "WHERE fecha_emision >= DATEADD(MONTH, -11, GETDATE()) " +
            "GROUP BY FORMAT(fecha_emision, 'yyyy-MM') " +
            "ORDER BY mes";
        return ejecutarMetrica(conn, sql);
    }

    private Map<String, Object> certsPorSector(Connection conn)
            throws SQLException {
        String sql =
            "SELECT ISNULL(cm.nombre, 'Sin sector') AS sector, " +
            "       COUNT(ce.id_certificacion) AS total " +
            "FROM certificacion_emitida ce " +
            "JOIN asambleista a ON a.id_asambleista = ce.id_asambleista " +
            "LEFT JOIN nombramiento n " +
            "    ON n.id_asambleista = a.id_asambleista " +
            "    AND n.estado = 'Vigente' " +
            "LEFT JOIN catalogo_maestro cm ON cm.id_item = n.id_sector " +
            "GROUP BY cm.nombre ORDER BY total DESC";
        return ejecutarMetrica(conn, sql);
    }

    private Map<String, Object> certsPorAnio(Connection conn)
            throws SQLException {
        String sql =
            "SELECT CAST(YEAR(fecha_emision) AS VARCHAR) AS anio, " +
            "       COUNT(*) AS total " +
            "FROM certificacion_emitida " +
            "GROUP BY YEAR(fecha_emision) " +
            "ORDER BY anio DESC";
        return ejecutarMetrica(conn, sql);
    }

    private Map<String, Object> ejecutarMetrica(Connection conn, String sql)
            throws SQLException {
        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                labels.add(rs.getString(1));
                values.add(rs.getInt(2));
            }
        }
        Map<String, Object> m = new HashMap<>();
        m.put("labels", labels);
        m.put("values", values);
        return m;
    }

    private int contarTabla(Connection conn, String tabla)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM " + tabla);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}