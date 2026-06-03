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
import java.util.HashMap;
import java.util.Map;

/**
 * QuorumController - Issue #11 (Frank)
 *
 * Endpoints REST que exponen el estado de quorum y asistencia
 * de una sesion. Pensado para ser consumido por el frontend de
 * Josue (vistas de sesion) mediante AJAX, devolviendo JSON.
 *
 * Rutas:
 *   GET /sesiones/quorum?id=X   -> Estado de quorum de la sesion X
 *   GET /sesiones/resumen?id=X  -> Conteos por estado de asistencia
 *
 * Seguridad:
 *   Ambos endpoints requieren autenticacion. No exponen datos
 *   personales, solo conteos agregados.
 */
@WebServlet(name = "QuorumController", urlPatterns = {
    "/sesiones/quorum",
    "/sesiones/resumen"
})
public class QuorumController extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!AuthController.middlewareAuth(req, resp)) return;

        String ruta = req.getServletPath();
        String idStr = req.getParameter("id");

        if (idStr == null || idStr.isBlank()) {
            enviarError(resp, HttpServletResponse.SC_BAD_REQUEST,
                "Falta el parametro 'id' de sesion.");
            return;
        }

        int idSesion;
        try {
            idSesion = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            enviarError(resp, HttpServletResponse.SC_BAD_REQUEST,
                "El id de sesion debe ser numerico.");
            return;
        }

        try {
            if ("/sesiones/quorum".equals(ruta)) {
                enviarJson(resp, calcularQuorum(idSesion));
            } else if ("/sesiones/resumen".equals(ruta)) {
                enviarJson(resp, calcularResumen(idSesion));
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            enviarError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error al consultar la base de datos.");
        }
    }


    /* ====================================================
       LOGICA: calcular quorum de una sesion
       Devuelve: { id, numeroSesion, presentes, requerido,
                   totalConvocados, tieneQuorum }
       ==================================================== */
    private Map<String, Object> calcularQuorum(int idSesion) throws SQLException {

        // 1) Obtener datos basicos de la sesion
        String sqlSesion =
            "SELECT s.id_sesion, s.numero_sesion, s.quorum_requerido, " +
            "       s.total_convocados, s.cerrada " +
            "FROM sesion s WHERE s.id_sesion = ?";

        // 2) Contar presentes (busca el id_item de 'Presente' por nombre)
        String sqlPresentes =
            "SELECT COUNT(*) AS presentes " +
            "FROM asistencia_sesion_plenaria a " +
            "INNER JOIN catalogo_maestro cm ON cm.id_item = a.id_estado_asistencia " +
            "WHERE a.id_sesion = ? " +
            "  AND cm.grupo_catalogo = 'ESTADO_ASISTENCIA' " +
            "  AND cm.nombre = 'Presente'";

        Map<String, Object> resultado = new HashMap<>();

        try (Connection con = Conexion.obtener()) {

            // Sesion
            try (PreparedStatement ps = con.prepareStatement(sqlSesion)) {
                ps.setInt(1, idSesion);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        resultado.put("error", "Sesion no encontrada");
                        return resultado;
                    }
                    resultado.put("idSesion",         rs.getInt("id_sesion"));
                    resultado.put("numeroSesion",     rs.getString("numero_sesion"));
                    resultado.put("quorumRequerido",  rs.getInt("quorum_requerido"));
                    resultado.put("totalConvocados",  rs.getInt("total_convocados"));
                    resultado.put("cerrada",          rs.getBoolean("cerrada"));
                }
            }

            // Presentes
            int presentes = 0;
            try (PreparedStatement ps = con.prepareStatement(sqlPresentes)) {
                ps.setInt(1, idSesion);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) presentes = rs.getInt("presentes");
                }
            }
            resultado.put("presentes", presentes);

            // Calculo de quorum
            int requerido = (int) resultado.getOrDefault("quorumRequerido", 0);
            boolean tieneQuorum = presentes >= requerido;
            resultado.put("tieneQuorum", tieneQuorum);

            // Porcentaje de asistencia
            int total = (int) resultado.getOrDefault("totalConvocados", 0);
            double porcentaje = total > 0 ? (presentes * 100.0 / total) : 0;
            resultado.put("porcentajeAsistencia", Math.round(porcentaje * 10.0) / 10.0);
        }

        return resultado;
    }


    /* ====================================================
       LOGICA: resumen de asistencia por estado
       Devuelve: { idSesion, presentes, ausentes, justificados,
                   sinRegistrar, total }
       ==================================================== */
    private Map<String, Object> calcularResumen(int idSesion) throws SQLException {

        String sql =
            "SELECT cm.nombre AS estado, COUNT(*) AS cantidad " +
            "FROM asistencia_sesion_plenaria a " +
            "INNER JOIN catalogo_maestro cm ON cm.id_item = a.id_estado_asistencia " +
            "WHERE a.id_sesion = ? " +
            "  AND cm.grupo_catalogo = 'ESTADO_ASISTENCIA' " +
            "GROUP BY cm.nombre";

        String sqlTotalConvocados =
            "SELECT total_convocados FROM sesion WHERE id_sesion = ?";

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("idSesion", idSesion);
        resumen.put("presentes", 0);
        resumen.put("ausentes", 0);
        resumen.put("justificados", 0);

        int totalRegistrados = 0;

        try (Connection con = Conexion.obtener()) {

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idSesion);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String estado = rs.getString("estado");
                        int cantidad  = rs.getInt("cantidad");
                        totalRegistrados += cantidad;

                        switch (estado) {
                            case "Presente":    resumen.put("presentes", cantidad);    break;
                            case "Ausente":     resumen.put("ausentes", cantidad);     break;
                            case "Justificado": resumen.put("justificados", cantidad); break;
                        }
                    }
                }
            }

            // Total convocados para calcular sin registrar
            int totalConvocados = 0;
            try (PreparedStatement ps = con.prepareStatement(sqlTotalConvocados)) {
                ps.setInt(1, idSesion);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) totalConvocados = rs.getInt("total_convocados");
                }
            }
            resumen.put("totalConvocados", totalConvocados);
            resumen.put("totalRegistrados", totalRegistrados);
            resumen.put("sinRegistrar", Math.max(0, totalConvocados - totalRegistrados));
        }

        return resumen;
    }


    /* ============== HELPERS ============== */

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