package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {

    private static final String SERVER   = "sglairtec.database.windows.net";
    private static final String DATABASE = "sgl_air";
    private static final String USER     = "sql_sgl_air_tec_admin";
    private static final String PASSWORD = "FrankArashJosue123";

    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("No se encontro el driver JDBC.");
        }
    }

    public static Connection obtener() throws SQLException {
        String url = String.format("jdbc:sqlserver://%s:1433;database=%s;encrypt=true;trustServerCertificate=false;loginTimeout=60;", SERVER, DATABASE);
        return DriverManager.getConnection(url, USER, PASSWORD);
    }

    public static void establecerContexto(Connection con, Integer idUsuario, String razonCambio) throws SQLException {
        try (var ps = con.prepareStatement("EXEC sp_set_session_context @key = ?, @value = ?")) {
            ps.setString(1, "usuario_id");
            if (idUsuario != null) { ps.setInt(2, idUsuario); } else { ps.setNull(2, java.sql.Types.INTEGER); }
            ps.execute();
            ps.setString(1, "razon_cambio");
            if (razonCambio != null) { ps.setString(2, razonCambio); } else { ps.setNull(2, java.sql.Types.NVARCHAR); }
            ps.execute();
        }
    }

    private Conexion() { }
}