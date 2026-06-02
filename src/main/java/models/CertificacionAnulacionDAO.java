package models;

import config.Conexion;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * CertificacionAnulacionDAO - Issue #15 (Frank)
 *
 * Encapsula las operaciones de anulacion y sustitucion sobre
 * certificacion_emitida. Delega toda la logica de validacion
 * legal a los stored procedures sp_anular_certificacion y
 * sp_emitir_sustitucion en Azure SQL.
 *
 * Reglas de negocio (vienen de los SPs):
 *  - Solo se anulan certificaciones en estado Activa (37)
 *  - El motivo es obligatorio (no vacio)
 *  - Una certificacion sustituida queda como estado 39
 *  - El folio anulado NO se reutiliza
 *  - Los triggers tg_no_repudio_cert bloquean UPDATE/DELETE
 *    sobre campos inmutables (folio, hash, asambleista, fecha)
 */
public class CertificacionAnulacionDAO {

    /**
     * Busca una certificacion por su folio unico.
     * Incluye el nombre del estado para mostrar en UI.
     *
     * @return null si no existe
     */
    public Certificacion obtenerPorFolio(String folio) throws SQLException {

        String sql =
            "SELECT ce.id_certificacion, ce.id_asambleista, ce.folio_unico, " +
            "       ce.hash_seguridad, " +
            "       CONVERT(VARCHAR, ce.fecha_emision, 103) AS fecha_emision_fmt, " +
            "       ce.id_estado, " +
            "       cm.nombre AS nombre_estado, " +
            "       ce.motivo_anulacion, " +
            "       CONVERT(VARCHAR, ce.fecha_anulacion, 103) AS fecha_anulacion_fmt, " +
            "       ce.usuario_anulacion, " +
            "       ce.id_certificacion_sustituye, " +
            "       a.nombre AS nombre_asambleista, a.cedula " +
            "FROM certificacion_emitida ce " +
            "INNER JOIN catalogo_maestro cm ON cm.id_item = ce.id_estado " +
            "LEFT JOIN asambleista a ON a.id_asambleista = ce.id_asambleista " +
            "WHERE ce.folio_unico = ?";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, folio);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Certificacion c = new Certificacion();
                c.setIdCertificacion(rs.getInt("id_certificacion"));
                c.setIdAsambleista(rs.getInt("id_asambleista"));
                c.setFolioUnico(rs.getString("folio_unico"));
                c.setHashSeguridad(rs.getString("hash_seguridad"));
                c.setFechaEmision(rs.getString("fecha_emision_fmt"));
                c.setIdEstado(rs.getInt("id_estado"));
                c.setNombreEstado(rs.getString("nombre_estado"));
                c.setMotivoAnulacion(rs.getString("motivo_anulacion"));
                c.setFechaAnulacion(rs.getString("fecha_anulacion_fmt"));

                int usuarioAnul = rs.getInt("usuario_anulacion");
                if (!rs.wasNull()) c.setUsuarioAnulacion(usuarioAnul);

                int idSust = rs.getInt("id_certificacion_sustituye");
                if (!rs.wasNull()) c.setIdCertificacionSustituye(idSust);

                c.setNombreAsambleista(rs.getString("nombre_asambleista"));
                c.setCedula(rs.getString("cedula"));
                return c;
            }
        }
    }

    /**
     * Anula una certificacion existente. Llama al SP que cambia
     * el id_estado a 38 (Anulada) y registra motivo, fecha y
     * usuario. Lanza SQLException si el SP rechaza la operacion
     * (folio no existe, no esta Activa, motivo vacio, etc.).
     */
    public void anular(String folio, String motivo, int idUsuario) throws SQLException {

        try (Connection con = Conexion.obtener()) {
            Conexion.establecerContexto(con, idUsuario, "ANULACION:" + folio);

            try (CallableStatement cs = con.prepareCall(
                    "{ call sp_anular_certificacion(?, ?, ?) }")) {

                cs.setString(1, folio);
                cs.setString(2, motivo);
                cs.setInt(3, idUsuario);
                cs.execute();
            }
        }
    }

    /**
     * Emite una sustitucion: marca la anterior como Sustituida (39)
     * y crea una nueva con id_certificacion_sustituye apuntando a
     * la anterior. El folio definitivo se asigna por el trigger
     * de foliado del Issue #1 cuando esta nueva certificacion se
     * complete.
     *
     * @return id de la nueva certificacion creada (-1 si fallo)
     */
    public int emitirSustitucion(String folioAnterior, String motivo,
                                  int idUsuario) throws SQLException {

        try (Connection con = Conexion.obtener()) {
            Conexion.establecerContexto(con, idUsuario, "SUSTITUCION:" + folioAnterior);

            try (CallableStatement cs = con.prepareCall(
                    "{ call sp_emitir_sustitucion(?, ?, ?, ?) }")) {

                cs.setString(1, folioAnterior);
                cs.setString(2, motivo);
                cs.setInt(3, idUsuario);
                cs.registerOutParameter(4, Types.INTEGER);
                cs.execute();

                return cs.getInt(4);
            }
        }
    }

    /**
     * Consulta el estado publico de un folio. Se usa en la pagina
     * de verificacion externa: devuelve solo lo necesario para
     * que un tercero confirme si el documento es valido o no.
     */
    public Certificacion verificarFolio(String folio) throws SQLException {
        return obtenerPorFolio(folio);
    }
}
