package controllers;

import com.google.gson.Gson;
import config.Conexion;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/asambleistas/buscar")
public class AsambleistaAjaxServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        Gson gson = new Gson();

        String q = req.getParameter("q");
        String idStr = req.getParameter("id");

        try (Connection conn = Conexion.obtener()) {
            if (idStr != null && !idStr.isEmpty()) {
                int id = Integer.parseInt(idStr);
                Map<String, Object> detalle = obtenerDetalle(conn, id);
                out.print(gson.toJson(detalle));
            } else if (q != null && !q.isEmpty()) {
                List<Map<String, Object>> resultados = buscarAsambleistas(conn, q);
                out.print(gson.toJson(resultados));
            } else {
                out.print("[]");
            }
        } catch (SQLException e) {
            resp.setStatus(500);
            out.print("{\"error\": \"Error de base de datos\"}");
        }
    }

    private List<Map<String, Object>> buscarAsambleistas(Connection conn, String q)
            throws SQLException {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT TOP 10 id_asambleista, nombre, cedula " +
                     "FROM asambleista " +
                     "WHERE nombre LIKE ? OR cedula LIKE ? " +
                     "ORDER BY nombre";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + q + "%");
            ps.setString(2, "%" + q + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getInt("id_asambleista"));
                m.put("nombre", rs.getString("nombre"));
                m.put("cedula", rs.getString("cedula"));
                lista.add(m);
            }
        }
        return lista;
    }

    private Map<String, Object> obtenerDetalle(Connection conn, int id)
            throws SQLException {
        Map<String, Object> detalle = new HashMap<>();
        String sql = "SELECT a.nombre, a.cedula, " +
                     "ISNULL(cm.nombre, 'Sin sector') AS sector_nombre, " +
                     "CONVERT(VARCHAR, n.fecha_inicio, 103) AS fecha_inicio, " +
                     "ISNULL(n.estado, 'Sin nombramiento') AS estado_nombramiento " +
                     "FROM asambleista a " +
                     "LEFT JOIN nombramiento n ON n.id_asambleista = a.id_asambleista " +
                     "    AND n.estado = 'Vigente' " +
                     "LEFT JOIN catalogo_maestro cm ON cm.id_item = n.id_sector " +
                     "WHERE a.id_asambleista = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                detalle.put("nombre", rs.getString("nombre"));
                detalle.put("cedula", rs.getString("cedula"));
                detalle.put("sector", rs.getString("sector_nombre"));
                detalle.put("fechaInicio", rs.getString("fecha_inicio") != null
                        ? rs.getString("fecha_inicio") : "N/D");
                detalle.put("estadoNombramiento", rs.getString("estado_nombramiento"));
                detalle.put("encontrado", true);
            } else {
                detalle.put("encontrado", false);
            }
        }
        return detalle;
    }
}