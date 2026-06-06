package controllers;

import config.Conexion;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BitacoraController - Issue #13 (Frank)
 *
 * Expone los eventos de la bitacora de certificaciones para
 * que el dashboard de auditoria los consulte. La bitacora es
 * append-only en BD: aqui solo se LEE, nunca se modifica.
 *
 * Rutas:
 *   GET /bitacora/certificaciones  -> Lista eventos con filtros opcionales
 *
 * Filtros (todos opcionales):
 *   folio       - filtra por folio exacto
 *   accion      - EMISION, REIMPRESION, ANULACION, SUSTITUCION, CONSULTA
 *   fechaDesde  - YYYY-MM-DD
 *   fechaHasta  - YYYY-MM-DD
 *   idUsuario   - id del usuario que ejecuto la accion
 *   limit       - cuantos resultados (default 50, max 200)
 *
 * Seguridad:
 *   Requiere autenticacion. La bitacora contiene info sensible.
 */
@WebServlet(name = "BitacoraController", urlPatterns = {
    "/bitacora/certificaciones"
})
public class BitacoraController extends HttpServlet {

    private static final Gson gson = new Gson();
    private static final int LIMIT_DEFAULT = 50;
    private static final int LIMIT_MAX = 200;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!AuthController.middlewareAuth(req, resp)) return;

        // Leer filtros
        String folio       = trim(req.getParameter("folio"));
        String accion      = trim(req.getParameter("accion"));
        String fechaDesde  = trim(req.getParameter("fechaDesde"));
        String fechaHasta  = trim(req.getParameter("fechaHasta"));
        String idUsuarioS  = trim(req.getParameter("idUsuario"));
        String limitS      = trim(req.getParameter("limit"));

        int limit = LIMIT_DEFAULT;
        if (limitS != null && !limitS.isBlank()) {
            try {
                limit = Math.min(LIMIT_MAX, Math.max(1, Integer.parseInt(limitS)));
            } catch (NumberFormatException e) {
                limit = LIMIT_DEFAULT;
            }
        }

        Integer idUsuario = null;
        if (idUsuarioS != null && !idUsuarioS.isBlank()) {
            try {
                idUsuario = Integer.parseInt(idUsuarioS);
            } catch (NumberFormatException e) {
                enviarError(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "El parametro idUsuario debe ser numerico.");
                return;
            }
        }

        try {
            List<Map<String, Object>> eventos = consultarBitacora(
                folio, accion, fechaDesde, fechaHasta, idUsuario, limit);

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("total", eventos.size());
            respuesta.put("limit", limit);
            respuesta.put("eventos", eventos);

            enviarJson(resp, respuesta);

        } catch (SQLException e) {
            e.printStackTrace();
            enviarError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error al consultar la bitacora.");
        }
    }


    /* ====================================================
       LOGICA: consulta la bitacora con filtros dinamicos
       ==================================================== */
    private List<Map<String, Object>> consultarBitacora(
            String folio, String accion, String fechaDesde,
            String fechaHasta, Integer idUsuario, int limit) throws SQLException {

        StringBuilder sql = new StringBuilder(
            "SELECT TOP (" + limit + ") " +
            "  l.id_log, " +
            "  l.id_certificacion, " +
            "  ce.folio_unico, " +
            "  cm.nombre AS accion, " +
            "  l.id_usuario, " +
            "  ISNULL(u.username, 'N/D') AS usuario, " +
            "  l.hash_documento, " +
            "  CONVERT(VARCHAR(19), l.fecha_evento, 121) AS fecha_evento, " +
            "  l.ip_origen, " +
            "  l.snapshot_json " +
            "FROM log_certificacion_emitida l " +
            "LEFT JOIN certificacion_emitida ce " +
            "       ON ce.id_certificacion = l.id_certificacion " +
            "LEFT JOIN catalogo_maestro cm " +
            "       ON cm.id_item = l.id_accion " +
            "LEFT JOIN sys_usuario u " +
            "       ON u.id_usuario = l.id_usuario " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (folio != null && !folio.isBlank()) {
            sql.append("AND ce.folio_unico = ? ");
            params.add(folio);
        }

        if (accion != null && !accion.isBlank()) {
            sql.append("AND cm.nombre = ? AND cm.grupo_catalogo = 'ACCION_LOG_CERT' ");
            params.add(accion);
        }

        if (fechaDesde != null && !fechaDesde.isBlank()) {
            sql.append("AND l.fecha_evento >= ? ");
            params.add(fechaDesde + " 00:00:00");
        }

        if (fechaHasta != null && !fechaHasta.isBlank()) {
            sql.append("AND l.fecha_evento <= ? ");
            params.add(fechaHasta + " 23:59:59");
        }

        if (idUsuario != null) {
            sql.append("AND l.id_usuario = ? ");
            params.add(idUsuario);
        }

        sql.append("ORDER BY l.fecha_evento DESC, l.id_log DESC");

        List<Map<String, Object>> eventos = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> ev = new HashMap<>();
                    ev.put("idLog",          rs.getInt("id_log"));
                    ev.put("idCertificacion",rs.getInt("id_certificacion_emitida"));
                    ev.put("folio",          rs.getString("folio_unico"));
                    ev.put("accion",         rs.getString("accion"));
                    ev.put("idUsuario",      rs.getInt("id_usuario"));
                    ev.put("usuario",        rs.getString("usuario"));
                    ev.put("hashDocumento",  rs.getString("hash_documento"));
                    ev.put("fechaEvento",    rs.getString("fecha_evento"));
                    ev.put("ipOrigen",       rs.getString("ip_origen"));
                    ev.put("snapshotJson",   rs.getString("snapshot_json"));
                    eventos.add(ev);
                }
            }
        }

        return eventos;
    }


    /* ============== HELPERS ============== */

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private void enviarJson(HttpServletResponse resp, Object obj) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(obj));
        }
    }

    private void enviarError(HttpServletResponse resp, int status, String mensaje)
            throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json; charset=UTF-8");
        Map<String, String> err = new HashMap<>();
        err.put("error", mensaje);
        try (PrintWriter out = resp.getWriter()) {
            out.print(gson.toJson(err));
        }
    }
}