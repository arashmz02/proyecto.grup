package com.itcr.air.dao;

import java.sql.*;
import java.util.*;
import com.google.gson.*;
import com.itcr.air.util.ConexionBD;

/**
 * NormativaDAO.java
 * Data Access Object para la jerarquía normativa
 * Contiene TODAS las queries SQL con JDBC puro
 */
public class NormativaDAO {
    
    /**
     * Obtiene el árbol COMPLETO de un reglamento (CTE recursiva)
     */
    public static JsonArray obtenerArbolReglamento(int idReglamento) throws SQLException {
        String sql = "WITH RECURSIVE arbol AS (" +
            "  SELECT " +
            "    e.id_elemento, e.id_elemento_padre, e.numero_etiqueta, " +
            "    e.contenido_texto, e.orden, n.nombre AS nivel, " +
            "    1 AS profundidad " +
            "  FROM elemento_normativo e " +
            "  JOIN catalogo_nivel_reglamento n ON e.id_nivel_reglamento = n.id_nivel_reglamento " +
            "  JOIN catalogo_estado_vigencia v ON e.id_estado_vigencia = v.id_estado_vigencia " +
            "  WHERE e.id_reglamento = ? AND e.id_elemento_padre IS NULL AND v.nombre = 'Vigente' " +
            "  UNION ALL " +
            "  SELECT " +
            "    e.id_elemento, e.id_elemento_padre, e.numero_etiqueta, " +
            "    e.contenido_texto, e.orden, n.nombre AS nivel, " +
            "    a.profundidad + 1 " +
            "  FROM elemento_normativo e " +
            "  JOIN catalogo_nivel_reglamento n ON e.id_nivel_reglamento = n.id_nivel_reglamento " +
            "  JOIN catalogo_estado_vigencia v ON e.id_estado_vigencia = v.id_estado_vigencia " +
            "  JOIN arbol a ON e.id_elemento_padre = a.id_elemento " +
            "  WHERE v.nombre = 'Vigente' " +
            ") SELECT * FROM arbol ORDER BY profundidad, orden";
        
        JsonArray resultado = new JsonArray();
        
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idReglamento);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                JsonObject fila = new JsonObject();
                fila.addProperty("id_elemento", rs.getInt("id_elemento"));
                fila.addProperty("id_elemento_padre", rs.getInt("id_elemento_padre"));
                fila.addProperty("numero_etiqueta", rs.getString("numero_etiqueta"));
                fila.addProperty("contenido_texto", rs.getString("contenido_texto"));
                fila.addProperty("nivel", rs.getString("nivel"));
                fila.addProperty("orden", rs.getInt("orden"));
                resultado.add(fila);
            }
        }
        
        return resultado;
    }
    
    /**
     * Obtiene los hijos directos de un elemento
     */
    public static JsonArray obtenerHijos(int idElementoPadre) throws SQLException {
        String sql = "SELECT * FROM elemento_normativo " +
            "WHERE id_elemento_padre = ? AND id_estado_vigencia = " +
            "(SELECT id_estado_vigencia FROM catalogo_estado_vigencia WHERE nombre = 'Vigente') " +
            "ORDER BY orden ASC";
        
        JsonArray resultado = new JsonArray();
        
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idElementoPadre);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                JsonObject fila = new JsonObject();
                fila.addProperty("id_elemento", rs.getInt("id_elemento"));
                fila.addProperty("numero_etiqueta", rs.getString("numero_etiqueta"));
                fila.addProperty("contenido_texto", rs.getString("contenido_texto"));
                resultado.add(fila);
            }
        }
        
        return resultado;
    }
    
    /**
     * Obtiene qué versión estaba vigente en una fecha específica
     */
    public static JsonObject obtenerArticuloEnFecha(int idElemento, String fecha) throws SQLException {
        String sql = "SELECT * FROM elemento_normativo " +
            "WHERE id_elemento = ? " +
            "AND fecha_inicio_vigencia <= CAST(? AS DATE) " +
            "AND (fecha_fin_vigencia IS NULL OR fecha_fin_vigencia > CAST(? AS DATE))";
        
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idElemento);
            stmt.setString(2, fecha);
            stmt.setString(3, fecha);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                JsonObject fila = new JsonObject();
                fila.addProperty("id_elemento", rs.getInt("id_elemento"));
                fila.addProperty("numero_etiqueta", rs.getString("numero_etiqueta"));
                fila.addProperty("contenido_texto", rs.getString("contenido_texto"));
                return fila;
            }
        }
        
        return new JsonObject();
    }
    
    /**
     * CREA un nuevo elemento (reforma, artículo, etc.)
     * El TRIGGER tg_vigencia_normativa se encarga de marcar versiones anteriores como Histórico
     */
    public static JsonObject crearElemento(int idReglamento, Integer idElementoPadre, 
                                          int idNivel, String numeroEtiqueta, 
                                          String contenido, int orden) throws SQLException {
        
        String sql = "INSERT INTO elemento_normativo " +
            "(id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia) " +
            "VALUES (?, ?, ?, ?, ?, ?, " +
            "(SELECT id_estado_vigencia FROM catalogo_estado_vigencia WHERE nombre = 'Vigente'))";
        
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, idReglamento);
            if (idElementoPadre != null) {
                stmt.setInt(2, idElementoPadre);
            } else {
                stmt.setNull(2, java.sql.Types.INTEGER);
            }
            stmt.setInt(3, idNivel);
            stmt.setString(4, numeroEtiqueta);
            stmt.setString(5, contenido);
            stmt.setInt(6, orden);
            
            int filasAfectadas = stmt.executeUpdate();
            
            if (filasAfectadas > 0) {
                // El trigger ya insertó el elemento, recupera el ID
                String sqlSelect = "SELECT TOP 1 * FROM elemento_normativo " +
                    "WHERE id_reglamento = ? AND numero_etiqueta = ? ORDER BY id_elemento DESC";
                
                try (PreparedStatement stmtSelect = conn.prepareStatement(sqlSelect)) {
                    stmtSelect.setInt(1, idReglamento);
                    stmtSelect.setString(2, numeroEtiqueta);
                    ResultSet rs = stmtSelect.executeQuery();
                    
                    if (rs.next()) {
                        JsonObject resultado = new JsonObject();
                        resultado.addProperty("id_elemento", rs.getInt("id_elemento"));
                        resultado.addProperty("numero_etiqueta", rs.getString("numero_etiqueta"));
                        resultado.addProperty("contenido_texto", rs.getString("contenido_texto"));
                        return resultado;
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("uq_etiqueta_vigente")) {
                throw new SQLException("Ya existe un elemento \"" + numeroEtiqueta + "\" vigente en este nivel");
            }
            throw e;
        }
        
        return new JsonObject();
    }
    
    /**
     * Obtiene el HISTORIAL COMPLETO de un elemento (todas sus versiones)
     */
    public static JsonArray obtenerHistorial(int idElemento) throws SQLException {
        String sql = "SELECT e.*, v.nombre AS estado_nombre " +
            "FROM elemento_normativo e " +
            "JOIN catalogo_estado_vigencia v ON e.id_estado_vigencia = v.id_estado_vigencia " +
            "WHERE e.id_elemento = ? " +
            "ORDER BY e.fecha_inicio_vigencia DESC";
        
        JsonArray resultado = new JsonArray();
        
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idElemento);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                JsonObject fila = new JsonObject();
                fila.addProperty("id_elemento", rs.getInt("id_elemento"));
                fila.addProperty("numero_etiqueta", rs.getString("numero_etiqueta"));
                fila.addProperty("contenido_texto", rs.getString("contenido_texto"));
                fila.addProperty("fecha_inicio_vigencia", rs.getDate("fecha_inicio_vigencia").toString());
                fila.addProperty("estado_nombre", rs.getString("estado_nombre"));
                
                if (rs.getDate("fecha_fin_vigencia") != null) {
                    fila.addProperty("fecha_fin_vigencia", rs.getDate("fecha_fin_vigencia").toString());
                }
                
                resultado.add(fila);
            }
        }
        
        return resultado;
    }
    
    /**
     * Obtiene la información COMPLETA de un elemento específico
     */
    public static JsonObject obtenerElemento(int idElemento) throws SQLException {
        String sql = "SELECT e.*, r.nombre_normativa, r.sigla, n.nombre AS nivel_nombre, " +
            "v.nombre AS estado_nombre " +
            "FROM elemento_normativo e " +
            "JOIN reglamento r ON e.id_reglamento = r.id_reglamento " +
            "JOIN catalogo_nivel_reglamento n ON e.id_nivel_reglamento = n.id_nivel_reglamento " +
            "JOIN catalogo_estado_vigencia v ON e.id_estado_vigencia = v.id_estado_vigencia " +
            "WHERE e.id_elemento = ?";
        
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idElemento);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                JsonObject fila = new JsonObject();
                fila.addProperty("id_elemento", rs.getInt("id_elemento"));
                fila.addProperty("numero_etiqueta", rs.getString("numero_etiqueta"));
                fila.addProperty("contenido_texto", rs.getString("contenido_texto"));
                fila.addProperty("nombre_normativa", rs.getString("nombre_normativa"));
                fila.addProperty("sigla", rs.getString("sigla"));
                fila.addProperty("nivel_nombre", rs.getString("nivel_nombre"));
                fila.addProperty("estado_nombre", rs.getString("estado_nombre"));
                fila.addProperty("fecha_inicio_vigencia", rs.getDate("fecha_inicio_vigencia").toString());
                
                if (rs.getDate("fecha_fin_vigencia") != null) {
                    fila.addProperty("fecha_fin_vigencia", rs.getDate("fecha_fin_vigencia").toString());
                }
                
                return fila;
            }
        }
        
        return new JsonObject();
    }
}