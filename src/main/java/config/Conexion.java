package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestiona la conexion a la base de datos Azure SQL.
 *
 * REGLA DE SEGURIDAD (Issue #0): las credenciales NUNCA van
 * escritas en el codigo ni en el repositorio. Se leen de
 * variables de entorno. En desarrollo local se definen en un
 * archivo .env (que esta en .gitignore); en GitHub Actions y en
 * el servidor de despliegue se configuran como secrets/env vars.
 *
 * Variables de entorno esperadas:
 *   DB_SERVER    -> sglairtec.database.windows.net
 *   DB_NAME      -> sgl_air
 *   DB_USER      -> usuario SQL
 *   DB_PASSWORD  -> contrasena del usuario SQL
 *
 * Si alguna no esta definida, la aplicacion falla de inmediato
 * con un mensaje claro, en lugar de intentar conectarse con
 * valores nulos.
 */
public class Conexion {

    private static final String SERVER;
    private static final String DATABASE;
    private static final String USER;
    private static final String PASSWORD;

    // Bloque estatico: se ejecuta una sola vez al cargar la clase.
    // Valida que todas las variables de entorno existan.
    static {
        SERVER   = requerirVariable("DB_SERVER");
        DATABASE = requerirVariable("DB_NAME");
        USER     = requerirVariable("DB_USER");
        PASSWORD = requerirVariable("DB_PASSWORD");

        // Carga explicita del driver JDBC de SQL Server.
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                "No se encontro el driver JDBC de SQL Server. " +
                "Verifique que la dependencia mssql-jdbc este en el pom.xml.");
        }
    }

    /**
     * Lee una variable de entorno y lanza error si no existe.
     */
    private static String requerirVariable(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) {
            throw new ExceptionInInitializerError(
                "Falta la variable de entorno '" + nombre + "'. " +
                "Definala en su archivo .env o en las variables del sistema.");
        }
        return valor;
    }

    /**
     * Devuelve una nueva conexion a la base de datos.
     * Quien la llama es responsable de cerrarla (idealmente con
     * try-with-resources).
     *
     * La cadena incluye 'encrypt=true' porque Azure SQL exige
     * conexiones cifradas (TLS).
     */
    public static Connection obtener() throws SQLException {
        String url = String.format(
            "jdbc:sqlserver://%s:1433;database=%s;encrypt=true;" +
            "trustServerCertificate=false;loginTimeout=60;",
            SERVER, DATABASE);

        return DriverManager.getConnection(url, USER, PASSWORD);
    }

    /**
     * Establece el contexto de sesion para los triggers de
     * auditoria. Debe llamarse despues de abrir la conexion y
     * antes de cualquier INSERT/UPDATE/DELETE que se quiera
     * auditar con el id del usuario responsable.
     *
     * Los triggers tg_auditoria_* y tg_cambio_identidad leen
     * estos valores con SESSION_CONTEXT(). Si no se establecen,
     * la auditoria registra NULL en id_usuario.
     *
     * @param con        conexion activa
     * @param idUsuario  id del usuario autenticado (puede ser null)
     * @param razonCambio razon del cambio de identidad (puede ser null)
     */
    public static void establecerContexto(Connection con,
                                          Integer idUsuario,
                                          String razonCambio) throws SQLException {
        // sp_set_session_context fija valores accesibles durante
        // toda la sesion de la conexion.
        try (var ps = con.prepareStatement(
                "EXEC sp_set_session_context @key = ?, @value = ?")) {

            ps.setString(1, "usuario_id");
            if (idUsuario != null) {
                ps.setInt(2, idUsuario);
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.execute();

            ps.setString(1, "razon_cambio");
            if (razonCambio != null) {
                ps.setString(2, razonCambio);
            } else {
                ps.setNull(2, java.sql.Types.NVARCHAR);
            }
            ps.execute();
        }
    }

    // Constructor privado: esta clase no se instancia, solo
    // expone metodos estaticos.
    private Conexion() { }
}
