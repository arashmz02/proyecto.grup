package controllers;

import config.Conexion;
import models.Asambleista;

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

/**
 * Controlador del modulo de asambleistas (Issue #9).
 *
 * Rutas:
 *   GET  /asambleistas            -> lista de asambleistas
 *   GET  /asambleistas/nuevo      -> formulario de alta
 *   POST /asambleistas/nuevo      -> procesa el alta
 *   GET  /asambleistas/detalle    -> historial de nombramientos
 *
 * Todas las rutas requieren autenticacion. Las acciones de
 * escritura requieren el permiso REGISTRAR_ASAMBLEISTAS.
 *
 * VALIDACIONES (criterios de aceptacion del Issue #9):
 *   - Cedula: formato X-XXXX-XXXX (un digito, guion, cuatro
 *     digitos, guion, cuatro digitos).
 *   - Correo: debe terminar en @itcr.ac.cr o @estudiantec.cr.
 *   - Cedula unica: se rechaza el alta si ya existe.
 */
@WebServlet(name = "AsambleistaController", urlPatterns = {
    "/asambleistas",
    "/asambleistas/nuevo",
    "/asambleistas/detalle"
})
public class AsambleistaController extends HttpServlet {

    // Patrones de validacion compilados una sola vez (mas eficiente)
    private static final Pattern PATRON_CEDULA =
        Pattern.compile("^[0-9]-[0-9]{4}-[0-9]{4}$");

    private static final Pattern PATRON_CORREO_INSTITUCIONAL =
        Pattern.compile("^[A-Za-z0-9._%+-]+@(itcr\\.ac\\.cr|estudiantec\\.cr)$");


    /* ============================================================
       GET: enrutado por ruta solicitada
       ============================================================ */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Todas las rutas exigen sesion activa
        if (!AuthController.middlewareAuth(req, resp)) return;

        String ruta = req.getServletPath();

        switch (ruta) {
            case "/asambleistas":
                listar(req, resp);
                break;
            case "/asambleistas/nuevo":
                mostrarFormulario(req, resp);
                break;
            case "/asambleistas/detalle":
                mostrarDetalle(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }


    /* ============================================================
       POST: solo /asambleistas/nuevo (alta)
       ============================================================ */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!AuthController.middlewareAuth(req, resp)) return;

        if ("/asambleistas/nuevo".equals(req.getServletPath())) {
            procesarAlta(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }


    /* ============================================================
       ACCION: listar asambleistas
       ============================================================ */
    private void listar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            List<Asambleista> asambleistas = Asambleista.listarTodos();

            // Marcar cuales estan vigentes para mostrarlo en la vista
            List<FilaAsambleista> filas = new ArrayList<>();
            for (Asambleista a : asambleistas) {
                FilaAsambleista f = new FilaAsambleista();
                f.asambleista = a;
                f.vigente     = Asambleista.estaVigente(a.getIdAsambleista());
                filas.add(f);
            }

            req.setAttribute("filas", filas);
            req.getRequestDispatcher("/views/asambleistas/asambleista-lista.jsp")
               .forward(req, resp);

        } catch (SQLException e) {
            e.printStackTrace();
            req.setAttribute("error",
                "No se pudo cargar la lista de asambleistas. Intente mas tarde.");
            req.getRequestDispatcher("/views/asambleistas/asambleista-lista.jsp")
               .forward(req, resp);
        }
    }


    /* ============================================================
       ACCION: mostrar formulario de alta
       Requiere permiso REGISTRAR_ASAMBLEISTAS.
       ============================================================ */
    private void mostrarFormulario(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!AuthController.middlewarePermiso(req, resp,
                "REGISTRAR_ASAMBLEISTAS")) {
            return;
        }

        try {
            // Cargar sectores y puestos del catalogo_maestro
            req.setAttribute("sectores", cargarOpciones("SECTOR"));
            req.setAttribute("puestos",  cargarOpciones("PUESTO"));

            req.getRequestDispatcher("/views/asambleistas/asambleista-registro.jsp")
               .forward(req, resp);

        } catch (SQLException e) {
            e.printStackTrace();
            req.setAttribute("error", "No se pudieron cargar los catalogos.");
            req.getRequestDispatcher("/views/asambleistas/asambleista-registro.jsp")
               .forward(req, resp);
        }
    }


    /* ============================================================
       ACCION: procesar alta del formulario
       ============================================================ */
    private void procesarAlta(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!AuthController.middlewarePermiso(req, resp,
                "REGISTRAR_ASAMBLEISTAS")) {
            return;
        }

        // ---- 1. Leer parametros ----
        String cedula = trim(req.getParameter("cedula"));
        String nombre = trim(req.getParameter("nombre"));
        String correo = trim(req.getParameter("correo"));
        String idSectorStr     = req.getParameter("idSector");
        String idPuestoStr     = req.getParameter("idPuesto");   // opcional
        String fechaInicioStr  = req.getParameter("fechaInicio");
        String fechaFinStr     = req.getParameter("fechaFin");   // opcional

        // ---- 2. Validaciones ----
        String error = validarCampos(cedula, nombre, correo,
                                     idSectorStr, fechaInicioStr);
        if (error != null) {
            volverAlFormularioConError(req, resp, error);
            return;
        }

        // ---- 3. Parsear y verificar duplicado ----
        try {
            if (Asambleista.obtenerPorCedula(cedula) != null) {
                volverAlFormularioConError(req, resp,
                    "Ya existe un asambleista con la cedula " + cedula + ".");
                return;
            }

            int idSector  = Integer.parseInt(idSectorStr);
            Integer idPuesto = (idPuestoStr == null || idPuestoStr.isBlank())
                               ? null : Integer.parseInt(idPuestoStr);

            LocalDate fechaInicio = LocalDate.parse(fechaInicioStr);
            LocalDate fechaFin    = (fechaFinStr == null || fechaFinStr.isBlank())
                                    ? null : LocalDate.parse(fechaFinStr);

            if (fechaFin != null && fechaFin.isBefore(fechaInicio)) {
                volverAlFormularioConError(req, resp,
                    "La fecha de fin no puede ser anterior a la de inicio.");
                return;
            }

            // ---- 4. Crear ----
            HttpSession sesion = req.getSession(false);
            int idUsuario = (Integer) sesion.getAttribute("idUsuario");

            int idAsambleista = Asambleista.crear(cedula, nombre, correo, idUsuario);
            Asambleista.agregarNombramiento(
                idAsambleista, idSector, idPuesto, fechaInicio, fechaFin, idUsuario);

            // Redirigir a la lista. El parametro 'creado' permite a
            // la vista mostrar un toast de exito.
            resp.sendRedirect(req.getContextPath() + "/asambleistas?creado=1");

        } catch (DateTimeParseException e) {
            volverAlFormularioConError(req, resp,
                "Las fechas deben tener formato YYYY-MM-DD.");

        } catch (NumberFormatException e) {
            volverAlFormularioConError(req, resp,
                "Los identificadores de sector y puesto deben ser numericos.");

        } catch (SQLException e) {
            // El trigger tg_traslape_sector lanza error 50001 si hay
            // traslape de fechas. Lo mostramos como mensaje amigable.
            if (e.getErrorCode() == 50001 ||
                (e.getMessage() != null && e.getMessage().contains("Traslape"))) {
                volverAlFormularioConError(req, resp,
                    "El nombramiento se traslapa con otro existente del asambleista.");
            } else {
                e.printStackTrace();
                volverAlFormularioConError(req, resp,
                    "Error al guardar. Intente de nuevo.");
            }
        }
    }


    /* ============================================================
       ACCION: detalle (historial de nombramientos)
       ============================================================ */
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
            Asambleista a = Asambleista.obtenerPorId(id);

            if (a == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Asambleista no encontrado.");
                return;
            }

            req.setAttribute("asambleista", a);
            req.setAttribute("vigente", Asambleista.estaVigente(id));
            req.setAttribute("historial",
                Asambleista.obtenerHistorialNombramientos(id));

            req.getRequestDispatcher("/views/asambleistas/asambleista-lista.jsp")
               .forward(req, resp);

        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Id invalido.");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error al cargar el detalle.");
        }
    }


    /* ============================================================
       HELPERS
       ============================================================ */

    /**
     * Valida los campos del formulario. Devuelve null si todo OK,
     * o un mensaje de error apto para mostrar al usuario.
     */
    private String validarCampos(String cedula, String nombre, String correo,
                                 String idSector, String fechaInicio) {

        if (cedula == null || cedula.isBlank()) return "La cedula es obligatoria.";
        if (!PATRON_CEDULA.matcher(cedula).matches())
            return "La cedula debe tener formato X-XXXX-XXXX (ej. 1-1234-5678).";

        if (nombre == null || nombre.isBlank()) return "El nombre es obligatorio.";
        if (nombre.length() > 150) return "El nombre no puede exceder 150 caracteres.";

        if (correo == null || correo.isBlank()) return "El correo es obligatorio.";
        if (!PATRON_CORREO_INSTITUCIONAL.matcher(correo).matches())
            return "El correo debe ser institucional (@itcr.ac.cr o @estudiantec.cr).";

        if (idSector == null || idSector.isBlank())
            return "Debe seleccionar un sector.";

        if (fechaInicio == null || fechaInicio.isBlank())
            return "La fecha de inicio del nombramiento es obligatoria.";

        return null;   // todo OK
    }


    /**
     * Carga las opciones de un grupo del catalogo_maestro (SECTOR,
     * PUESTO, etc.) para llenar selects en la vista.
     */
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


    /**
     * Reenvia al formulario de alta con un mensaje de error y
     * preservando los valores que el usuario ya habia escrito,
     * para que no tenga que reescribir todo.
     */
    private void volverAlFormularioConError(HttpServletRequest req,
                                            HttpServletResponse resp,
                                            String mensaje)
            throws ServletException, IOException {

        req.setAttribute("error", mensaje);

        // Preservar lo que el usuario escribio
        req.setAttribute("cedulaPrev",      req.getParameter("cedula"));
        req.setAttribute("nombrePrev",      req.getParameter("nombre"));
        req.setAttribute("correoPrev",      req.getParameter("correo"));
        req.setAttribute("idSectorPrev",    req.getParameter("idSector"));
        req.setAttribute("idPuestoPrev",    req.getParameter("idPuesto"));
        req.setAttribute("fechaInicioPrev", req.getParameter("fechaInicio"));
        req.setAttribute("fechaFinPrev",    req.getParameter("fechaFin"));

        try {
            req.setAttribute("sectores", cargarOpciones("SECTOR"));
            req.setAttribute("puestos",  cargarOpciones("PUESTO"));
        } catch (SQLException e) {
            // si no se cargan los catalogos, el error principal ya
            // esta seteado; no sobreescribimos
        }

        req.getRequestDispatcher("/views/asambleistas/asambleista-registro.jsp")
           .forward(req, resp);
    }


    /** Trim seguro: devuelve null si el input es null. */
    private static String trim(String s) {
        return s == null ? null : s.trim();
    }


    /* ============================================================
       DTOs internos (publicos para que las JSP los accedan)
       ============================================================ */
    public static class FilaAsambleista {
        public Asambleista asambleista;
        public boolean vigente;

        public Asambleista getAsambleista() { return asambleista; }
        public boolean isVigente()          { return vigente; }
    }

    public static class OpcionCatalogo {
        public int id;
        public String nombre;

        public int getId()        { return id; }
        public String getNombre() { return nombre; }
    }
}
