package com.itcr.air.servlets;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import com.google.gson.*;
import com.itcr.air.dao.NormativaDAO;

/**
 * NormativaServlet.java
 * Controlador para la jerarquÃ­a normativa
 * Maneja las solicitudes HTTP y llamadas al DAO
 */
@WebServlet("/api/normativa/*")
public class NormativaServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private Gson gson = new Gson();
    
    /**
     * GET: /api/normativa/arbol/1 â†’ Ã¡rbol del reglamento 1
     * GET: /api/normativa/articulo/5 â†’ detalle del elemento 5
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            String pathInfo = request.getPathInfo();
            String[] parts = pathInfo.split("/");
            
            if (parts.length < 3) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println(gson.toJson(crearError("Ruta invÃ¡lida")));
                return;
            }
            
            String accion = parts[1];
            int id = Integer.parseInt(parts[2]);
            
            if ("arbol".equals(accion)) {
                verArbol(id, response, out);
            } else if ("articulo".equals(accion)) {
                verArticulo(id, response, out);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.println(gson.toJson(crearError("AcciÃ³n no encontrada")));
            }
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println(gson.toJson(crearError("ID invÃ¡lido")));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(gson.toJson(crearError("Error: " + e.getMessage())));
            e.printStackTrace();
        }
    }
    
    /**
     * POST: /api/normativa/reforma â†’ crear nuevo elemento
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            //Leer el JSON del body
            BufferedReader reader = request.getReader();
            JsonElement jsonElement = JsonParser.parseReader(reader);
            JsonObject jsonData = jsonElement.getAsJsonObject();
            
            String pathInfo = request.getPathInfo();
            
            if ("/reforma".equals(pathInfo)) {
                crearReforma(jsonData, response, out);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.println(gson.toJson(crearError("AcciÃ³n no encontrada")));
            }
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(gson.toJson(crearError("Error: " + e.getMessage())));
            e.printStackTrace();
        }
    }
    
    /**
     * Ver Ã¡rbol completo de un reglamento
     */
    private void verArbol(int idReglamento, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        
        JsonArray arbol = NormativaDAO.obtenerArbolReglamento(idReglamento);
        
        JsonObject resultado = new JsonObject();
        resultado.addProperty("success", true);
        resultado.add("datos", arbol);
        
        out.println(gson.toJson(resultado));
    }
    
    /**
     * Ver un artÃ­culo especÃ­fico con su historial
     */
    private void verArticulo(int idElemento, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        
        JsonObject elemento = NormativaDAO.obtenerElemento(idElemento);
        
        if (elemento.keySet().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.println(gson.toJson(crearError("Elemento no encontrado")));
            return;
        }
        
        JsonArray historial = NormativaDAO.obtenerHistorial(idElemento);
        
        JsonObject resultado = new JsonObject();
        resultado.addProperty("success", true);
        resultado.add("elemento", elemento);
        resultado.add("historial", historial);
        
        out.println(gson.toJson(resultado));
    }
    
    /**
     * Crear una reforma (nuevo elemento)
     */
    private void crearReforma(JsonObject datos, HttpServletResponse response, PrintWriter out) 
            throws Exception {
        
        //Validaciones bÃ¡sicas
        if (!datos.has("id_reglamento") || !datos.has("id_nivel_reglamento") || 
            !datos.has("numero_etiqueta") || !datos.has("contenido_texto")) {
            
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println(gson.toJson(crearError("Faltan campos obligatorios")));
            return;
        }
        
        int idReglamento = datos.get("id_reglamento").getAsInt();
        int idNivel = datos.get("id_nivel_reglamento").getAsInt();
        String numeroEtiqueta = datos.get("numero_etiqueta").getAsString();
        String contenido = datos.get("contenido_texto").getAsString();
        int orden = datos.has("orden") ? datos.get("orden").getAsInt() : 1;
        Integer idElementoPadre = datos.has("id_elemento_padre") ? 
            datos.get("id_elemento_padre").getAsInt() : null;
        
        //ValidaciÃ³n de orden
        if (orden < 1) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println(gson.toJson(crearError("Orden debe ser mayor a 0")));
            return;
        }
        
        try {
            JsonObject elemento = NormativaDAO.crearElemento(idReglamento, idElementoPadre, 
                                                            idNivel, numeroEtiqueta, contenido, orden);
            
            response.setStatus(HttpServletResponse.SC_CREATED);
            JsonObject resultado = new JsonObject();
            resultado.addProperty("success", true);
            resultado.addProperty("mensaje", "Elemento creado. VersiÃ³n anterior marcada como histÃ³rica.");
            resultado.add("elemento", elemento);
            
            out.println(gson.toJson(resultado));
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println(gson.toJson(crearError(e.getMessage())));
        }
    }
    
    /**
     * Crea un objeto JSON de error
     */
    private JsonObject crearError(String mensaje) {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", mensaje);
        return error;
    }
}
