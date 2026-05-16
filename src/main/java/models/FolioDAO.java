package models;

import config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FolioDAO {

    public String generarFolio(String tipoDocumento, int anio, String observacion) {

        String sql = "SELECT fn_generar_folio(?, ?, NULL, ?)";

        try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            stmt.setString(1, tipoDocumento);
            stmt.setInt(2, anio);
            stmt.setString(3, observacion);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}

