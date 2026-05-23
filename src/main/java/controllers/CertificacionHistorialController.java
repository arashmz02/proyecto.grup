package controllers;

import config.Conexion;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/certificaciones/historial")
public class CertificacionHistorialController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String fechaDesde  = req.getParameter("fechaDesde");
        String fechaHasta  = req.getParameter("fechaHasta");
        String consecutivo = req.getParameter("consecutivo");
        String buscaNombre = req.getParameter("buscaNombre");

        List<Map<String, Object>> registros = new ArrayList<>();
        String mensajeError = null;

        try (Connection conn = Conexion.obtener()) {
            registros = consultarHistorial(conn, fechaDesde, fechaHasta,
                                           consecutivo, buscaNombre);
        } catch (SQLException e) {
            mensajeError = "No se pudo cargar el historial: " + e.getMessage();
        }

        req.setAttribute("registros", registros);
        req.setAttribute("mensajeError", mensajeError);
        req.setAttribute("filtroFechaDesde", fechaDesde);
        req.setAttribute("filtroFechaHasta", fechaHasta);
        req.setAttribute("filtroConsecutivo", consecutivo);
        req.setAttribute("filtroBuscaNombre", buscaNombre);

        req.getRequestDispatcher("/views/certificaciones/historial.jsp")
           .forward(req, resp);
    }

    private List<Map<String, Object>> consultarHistorial(
            Connection conn, String fechaDesde, String fechaHasta,
            String consecutivo, String buscaNombre) throws SQLException {

        List<Map<String, Object>> lista = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT ce.id_certificacion, ce.folio_unico, " +
            "       a.nombre AS nombre_asambleista, a.cedula, " +
            "       CONVERT(VARCHAR, ce.fecha_emision, 103) AS fecha_emision_fmt, " +
            "       ISNULL(u.username, 'N/D') AS usuario_secretaria, " +
            "       ISNULL(ce.hash_seguridad, '') AS hash_seguridad, " +
            "       'Activo' AS estado, " +
            "       NULL AS motivo_anulacion " +
            "FROM certificacion_emitida ce " +
            "JOIN asambleista a ON a.id_asambleista = ce.id_asambleista " +
            "LEFT JOIN sys_usuario u ON u.id_usuario = ce.usuario_secretaria " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (fechaDesde != null && !fechaDesde.isEmpty()) {
            sql.append("AND ce.fecha_emision >= ? ");
            params.add(fechaDesde);
        }
        if (fechaHasta != null && !fechaHasta.isEmpty()) {
            sql.append("AND ce.fecha_emision <= ? ");
            params.add(fechaHasta + " 23:59:59");
        }
        if (consecutivo != null && !consecutivo.isEmpty()) {
            sql.append("AND ce.folio_unico LIKE ? ");
            params.add("%" + consecutivo + "%");
        }
        if (buscaNombre != null && !buscaNombre.isEmpty()) {
            sql.append("AND a.nombre LIKE ? ");
            params.add("%" + buscaNombre + "%");
        }

        sql.append("ORDER BY ce.fecha_emision DESC");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id_certificacion"));
                row.put("folio", rs.getString("folio_unico"));
                row.put("nombreAsambleista", rs.getString("nombre_asambleista"));
                row.put("cedula", rs.getString("cedula"));
                row.put("fechaEmision", rs.getString("fecha_emision_fmt"));
                row.put("usuarioSecretaria", rs.getString("usuario_secretaria"));
                row.put("estado", rs.getString("estado"));
                row.put("motivoAnulacion", rs.getString("motivo_anulacion"));
                lista.add(row);
            }
        }
        return lista;
    }
}