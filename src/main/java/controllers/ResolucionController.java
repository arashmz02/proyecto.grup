package controllers;
 
import config.Conexion;
import models.Resolucion;
 
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
 
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
 
/*Controlador del modulo de Resoluciones (Issue #10 Parte II)
Responsable: Josue 
NOTA DE INTEGRACION (post-merge):
- La tabla 'resolucion_propuesta' (no 'resolucion') es la que
  pertenece a este modulo. La tabla 'resolucion' pertenece al
  motor de votaciones de Frank (Issue #12).
- Las URLs (/resoluciones, /resoluciones/detalle, etc.) se mantienen.
 
NOTA: aplicarReforma() DISPARA el trigger tg_vigencia_normativa
del Sprint 2 que archiva automaticamente la version anterior.*/
@WebServlet(name = "ResolucionController", urlPatterns = {
    "/resoluciones",
    "/resoluciones/detalle",
    "/resoluciones/emitir",
    "/agenda/agregar",
    "/reformas/aplicar"
})
public class ResolucionController extends HttpServlet {
 
    //Formato 'AIR-RES-NNN-AAAA'
    private static final Pattern PATRON_NUMERO_RESOLUCION =
        Pattern.compile("^AIR-RES-[0-9]{1,4}-[0-9]{4}$");
 
 
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
 
        if (!AuthController.middlewareAuth(req, resp)) return;
 
        String ruta = req.getServletPath();
 
        switch (ruta) {
            case "/resoluciones":
                listar(req, resp);
                break;
            case "/resoluciones/detalle":
                mostrarDetalle(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
 
 
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
 
        if (!AuthController.middlewareAuth(req, resp)) return;
 
        String ruta = req.getServletPath();
 
        switch (ruta) {
            case "/agenda/agregar":
                procesarAgregarAgenda(req, resp);
                break;
            case "/resoluciones/emitir":
                procesarEmitirResolucion(req, resp);
                break;
            case "/reformas/aplicar":
                procesarAplicarReforma(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
 
 
    /*ACCION: listar todas las resoluciones */
    private void listar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
 
        try {
            List<Resolucion> resoluciones = Resolucion.listarTodas();
            req.setAttribute("resoluciones", resoluciones);
 
            req.getRequestDispatcher("/views/resoluciones/resolucion-lista.jsp")
               .forward(req, resp);
 
        } catch (SQLException e) {
            e.printStackTrace();
            req.setAttribute("error",
                "No se pudo cargar la lista de resoluciones.");
            req.getRequestDispatcher("/views/resoluciones/resolucion-lista.jsp")
               .forward(req, resp);
        }
    }
 
 
    /*ACCION: detalle de la resolucion + reformas aplicadas */
    private void mostrarDetalle(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
 
        String idStr = req.getParameter("id");
        if (idStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Falta el parametro 'id'.");
            return;
        }
 
        try {
            int id = Integer.parseInt(idStr);
            Resolucion r = Resolucion.obtenerPorId(id);
 
            if (r == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Resolucion no encontrada.");
                return;
            }
 
            req.setAttribute("resolucion", r);
            req.setAttribute("reformas",   Resolucion.obtenerReformasDeResolucion(id));
            req.setAttribute("tiposReforma", cargarOpciones("TIPO_REFORMA"));
 
            req.getRequestDispatcher("/views/resoluciones/resolucion-detalle.jsp")
               .forward(req, resp);
 
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Id invalido.");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error al cargar el detalle.");
        }
    }
 
 
    /*ACCION: agregar una propuesta como punto de agenda de una sesion */
    private void procesarAgregarAgenda(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
 
        if (!AuthController.middlewarePermiso(req, resp,
                "REGISTRAR_ASAMBLEISTAS")) {
            return;
        }
 
        String idSesionStr     = req.getParameter("idSesion");
        String idPropuestaStr  = req.getParameter("idPropuesta");
        String ordenStr        = req.getParameter("orden");
        String descripcion     = req.getParameter("descripcion");
 
        if (idSesionStr == null || idPropuestaStr == null || ordenStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Faltan parametros (sesion, propuesta y orden son obligatorios).");
            return;
        }
 
        try {
            int idSesion    = Integer.parseInt(idSesionStr);
            int idPropuesta = Integer.parseInt(idPropuestaStr);
            int orden       = Integer.parseInt(ordenStr);
 
            if (orden < 1) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "El orden debe ser mayor o igual a 1.");
                return;
            }
 
            Resolucion.crearPuntoAgenda(idSesion, idPropuesta, orden, descripcion);
 
            resp.sendRedirect(req.getContextPath() +
                "/sesiones/detalle?id=" + idSesion + "&agendaAgregada=1");
 
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Los identificadores y el orden deben ser numericos.");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error al agregar a la agenda. Verifique que la propuesta no este " +
                "ya en esta sesion o que el orden no este ocupado.");
        }
    }
 
 
    /*ACCION: emitir una resolucion oficial sobre un punto de agenda */
    private void procesarEmitirResolucion(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
 
        if (!AuthController.middlewarePermiso(req, resp,
                "EMITIR_CERTIFICACION")) {
            return;
        }
 
        String idPuntoStr        = req.getParameter("idPuntoAgenda");
        String numeroResolucion  = trim(req.getParameter("numeroResolucion"));
 
        if (idPuntoStr == null || numeroResolucion == null || numeroResolucion.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Falta el punto de agenda o el numero de resolucion.");
            return;
        }
 
        if (!PATRON_NUMERO_RESOLUCION.matcher(numeroResolucion).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "El numero debe tener formato AIR-RES-NNN-AAAA (ej. AIR-RES-001-2024).");
            return;
        }
 
        try {
            if (Resolucion.obtenerPorNumero(numeroResolucion) != null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Ya existe una resolucion con ese numero.");
                return;
            }
 
            int idPunto = Integer.parseInt(idPuntoStr);
 
            HttpSession sesion = req.getSession(false);
            int idUsuario = (Integer) sesion.getAttribute("idUsuario");
 
            int idResolucion = Resolucion.emitirResolucion(idPunto, numeroResolucion, idUsuario);
 
            resp.sendRedirect(req.getContextPath() +
                "/resoluciones/detalle?id=" + idResolucion + "&emitida=1");
 
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "El identificador del punto de agenda debe ser numerico.");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error al emitir la resolucion. Verifique que el punto de agenda " +
                "no tenga ya una resolucion asociada.");
        }
    }
 
 
    /*ACCION: aplicar una reforma normativa derivada de una resolucion */
    private void procesarAplicarReforma(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
 
        if (!AuthController.middlewarePermiso(req, resp,
                "EMITIR_CERTIFICACION")) {
            return;
        }
 
        String idResolucionStr  = req.getParameter("idResolucion");
        String idElementoStr    = req.getParameter("idElementoNormativo");
        String idTipoStr        = req.getParameter("idTipoReforma");
        String textoAnterior    = req.getParameter("textoAnterior");
        String textoNuevo       = req.getParameter("textoNuevo");
        String fechaVigenciaStr = req.getParameter("fechaInicioVigencia");
 
        if (idResolucionStr == null || idElementoStr == null || idTipoStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Faltan datos obligatorios para aplicar la reforma.");
            return;
        }
 
        if (textoNuevo == null || textoNuevo.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "El texto nuevo de la reforma no puede estar vacio.");
            return;
        }
 
        try {
            int idResolucion = Integer.parseInt(idResolucionStr);
            int idElemento   = Integer.parseInt(idElementoStr);
            int idTipo       = Integer.parseInt(idTipoStr);
 
            LocalDate fechaVigencia = (fechaVigenciaStr == null || fechaVigenciaStr.isBlank())
                                      ? LocalDate.now() : LocalDate.parse(fechaVigenciaStr);
 
            HttpSession sesion = req.getSession(false);
            int idUsuario = (Integer) sesion.getAttribute("idUsuario");
 
            Resolucion.aplicarReforma(idResolucion, idElemento, idTipo,
                                      textoAnterior, textoNuevo, fechaVigencia, idUsuario);
 
            resp.sendRedirect(req.getContextPath() +
                "/resoluciones/detalle?id=" + idResolucion + "&reformaAplicada=1");
 
        } catch (DateTimeParseException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "La fecha de vigencia debe tener formato YYYY-MM-DD.");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Los identificadores deben ser numericos.");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error al aplicar la reforma. Verifique los datos.");
        }
    }
 
 
    /*HELPERS*/
 
    private List<OpcionCatalogo> cargarOpciones(String grupo) throws SQLException {
        String sql = "SELECT id_item, nombre FROM catalogo_maestro " +
                     "WHERE grupo_catalogo = ? AND activo = 1 ORDER BY nombre";
 
        List<OpcionCatalogo> opciones = new ArrayList<>();
 
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
 
            ps.setString(1, grupo);
 
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OpcionCatalogo o = new OpcionCatalogo();
                    o.id     = rs.getInt("id_item");
                    o.nombre = rs.getString("nombre");
                    opciones.add(o);
                }
            }
        }
        return opciones;
    }
 
 
    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
 
 
    public static class OpcionCatalogo {
        public int id;
        public String nombre;
 
        public int getId()        { return id; }
        public String getNombre() { return nombre; }
    }
}