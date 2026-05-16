package com.itcr.air.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * ConexionBD.java
 * Maneja la conexión a Azure SQL Server
 * 
 -- IMPORTANTE: Reemplaza los valores con tus credenciales de Azure
 */
public class ConexionBD {
    
    // REEMPLAZA ESTO CON TUS DATOS DE AZURE
    private static final String SERVIDOR = "tu-servidor.database.windows.net";
    private static final String PUERTO = "1433";
    private static final String BD = "proyecto_air";
    private static final String USUARIO = "admin_user";
    private static final String CONTRASENA = "TuContrasena123!";
    
    // URL de conexión JDBC para SQL Server
    private static final String URL = 
        "jdbc:sqlserver://" + SERVIDOR + ":" + PUERTO + 
        ";database=" + BD + 
        ";encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30";
    
    /**
     * Obtiene una conexión a la BD
     */
    public static Connection obtenerConexion() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection conexion = DriverManager.getConnection(URL, USUARIO, CONTRASENA);
            System.out.println("[INFO] Conexión a Azure SQL exitosa");
            return conexion;
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] Driver JDBC no encontrado");
            throw new SQLException("Driver no disponible", e);
        } catch (SQLException e) {
            System.err.println("[ERROR] No se pudo conectar: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Cierra una conexión
     */
    public static void cerrarConexion(Connection conexion) {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                System.err.println("[ERROR] Error al cerrar: " + e.getMessage());
            }
        }
    }
}