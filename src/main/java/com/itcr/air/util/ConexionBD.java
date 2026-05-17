package com.itcr.air.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * ConexionBD.java
 * Maneja la conexion a Azure SQL Server.
 * Lee credenciales desde variables de entorno (Issue #0).
 */
public class ConexionBD {

    private static final String SERVIDOR = getEnv("DB_SERVER");
    private static final String BD       = getEnv("DB_NAME");
    private static final String USUARIO  = getEnv("DB_USER");
    private static final String CONTRASENA = getEnv("DB_PASSWORD");

    private static final String URL =
        "jdbc:sqlserver://" + SERVIDOR + ":1433" +
        ";database=" + BD +
        ";encrypt=true;trustServerCertificate=false;loginTimeout=60";

    private static String getEnv(String nombre) {
        String v = System.getenv(nombre);
        if (v == null || v.isBlank()) {
            throw new RuntimeException("Falta la variable de entorno " + nombre);
        }
        return v;
    }

    public static Connection obtenerConexion() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            return DriverManager.getConnection(URL, USUARIO, CONTRASENA);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver no disponible", e);
        }
    }

    public static void cerrarConexion(Connection conexion) {
        if (conexion != null) {
            try { conexion.close(); } catch (SQLException e) { /* ignore */ }
        }
    }
}