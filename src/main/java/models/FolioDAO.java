package models;

import config.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * FolioDAO - Issue #1 (Arash)
 *
 * Genera folios atomicos para certificaciones.
 * Aprovecha el trigger tg_folio_secuencial de la BD, que se
 * dispara al INSERT en certificacion_emitida y genera el folio
 * unico (formato DAIR-NNN-AAAA) bajo control de concurrencia.
 *
 * Tablas:
 *   - certificacion_emitida: donde se inserta la certificacion
 *   - control_folio: contador anual gestionado por el trigger
 */
public class FolioDAO {

    /**
     * Genera un folio nuevo insertando en certificacion_emitida.
     * El trigger tg_folio_secuencial calcula el folio.
     *
     * @param tipoDocumento  texto descriptivo (se guarda en hash_seguridad
     *                       como referencia simple por ahora)
     * @param anio           anio del folio
     * @param observacion    observacion libre
     * @param idUsuario      id del usuario que genera (para auditoria)
     * @return el folio generado (ej. DAIR-001-2026), o null si fallo
     */
    public String generarFolio(String tipoDocumento, int anio,
                               String observacion, int idUsuario) throws SQLException {

        // Insertar dejando folio_unico vacio -> el trigger lo genera
        // El parametro id_asambleista se pone null porque este uso
        // es para folios generales, no asociados a un asambleista
        String sqlInsert =
            "INSERT INTO certificacion_emitida " +
            "(id_asambleista, folio_unico, hash_seguridad, usuario_secretaria) " +
            "VALUES (NULL, NULL, ?, ?); " +
            "SELECT TOP 1 folio_unico FROM certificacion_emitida " +
            "WHERE usuario_secretaria = ? ORDER BY fecha_emision DESC;";

        try (Connection con = Conexion.obtener()) {

            Conexion.establecerContexto(con, idUsuario, null);

            try (PreparedStatement ps = con.prepareStatement(sqlInsert)) {
                String hash = "TIPO:" + tipoDocumento + "|OBS:" +
                              (observacion == null ? "" : observacion);
                ps.setString(1, hash);
                ps.setInt(2, idUsuario);
                ps.setInt(3, idUsuario);

                boolean tieneResultados = ps.execute();
                while (!tieneResultados && ps.getUpdateCount() != -1) {
                    tieneResultados = ps.getMoreResults();
                }

                if (tieneResultados) {
                    try (ResultSet rs = ps.getResultSet()) {
                        if (rs.next()) {
                            return rs.getString("folio_unico");
                        }
                    }
                }
            }
        }
        return null;
    }
}