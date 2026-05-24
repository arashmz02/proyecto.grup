package controllers;

import com.google.gson.Gson;
import config.Conexion;
import models.Asambleista;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/*Controlador del modulo de asambleistas (Issue #9)*/
@WebServlet(name = "AsambleistaController", urlPatterns = {
    "/asambleistas",
    "/asambleistas/nuevo",
    "/asambleistas/detalle",
    "/asambleistas/buscar"
})
public class AsambleistaController extends HttpServlet {

    private static final Pattern PATRON_CEDULA =
        Pattern.compile("^[0-9]-[0-9]{4}-[0-9]{4}$");

    private static final Pattern PATRON_CORREO_INSTITUCIONAL =
        Pattern.compile("^[A-Za-z0-9._%+-]+@(itcr\\.ac\\.cr|estudiantec\\.cr)$");


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

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
            case "/asambleistas/buscar":
                buscarJson(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }


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


    private void listar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            List<Asambleista> asambleistas = Asambleista.listarTodos();

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


    private void mostrarFormulario(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!AuthController.middlewarePermiso(req, resp,
                "REGISTRAR_ASAMBLEISTAS")) {
            return;
        }

        try {
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


    private void procesarAlta(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!AuthController.middlewarePermiso(req, resp,
                "REGISTRAR_ASAMBLEISTAS")) {
            return;
        }

        String cedula = trim(req.getParameter("cedula"));
        String nombre = trim(req.getParameter("nombre"));
        String correo = trim(req.getParameter("correo"));
        String idSectorStr     = req.getParameter("idSector");
        String idPuestoStr     = req.getParameter("idPuesto");
        String fechaInicioStr  = req.getParameter("fechaInicio");
        String fechaFinStr     = req.getParameter("fechaFin");

        String error = validarCampos(cedula, nombre, correo,
                                     idSectorStr, fechaInicioStr);
        if (error != null) {
            volverAlFormularioConError(req, resp, error);
            return;
        }

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

            HttpSession sesion = req.getSession(false);
            int idUsuario = (Integer) sesion.getAttribute("idUsuario");

            int idAsambleista = Asambleista.crear(cedula, nombre, correo, idUsuario);
            Asambleista.agregarNombramiento(
                idAsambleista, idSector, idPuesto, fechaInicio, fechaFin, idUsuario);

            resp.sendRedirect(req.getContextPath() + "/asambleistas?creado=1");

        } catch (DateTimeParseException e) {
            volverAlFormularioConError(req, resp,
                "Las fechas deben tener formato YYYY-MM-DD.");

        } catch (NumberFormatException e) {
            volverAlFormularioConError(req, resp,
                "Los identificadores de sector y puesto deben ser numericos.");

        } catch (SQLException e) {
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


    /*Endpoint de busqueda para autocomplete del Dashboard (Issue #16).
     Recibe un parametro 'q' con el texto a buscar y devuelve JSON con los asambleistas que matcheen (max 10).*/
    private void buscarJson(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String texto = req.getParameter("q");

        try (PrintWriter out = resp.getWriter()) {
            List<Asambleista> encontrados = Asambleista.buscarPorTexto(texto);

            // Construimos una lista de mapas para la respuesta JSON.
            // No exponemos el correo institucional aqui por privacidad.
            List<Map<String, Object>> respuesta = new ArrayList<>();
            for (Asambleista a : encontrados) {
                Map<String, Object> item = new HashMap<>();
                item.put("id",     a.getIdAsambleista());
                item.put("nombre", a.getNombre());
                item.put("cedula", a.getCedula());
                respuesta.add(item);
            }

            Gson gson = new Gson();
            out.print(gson.toJson(respuesta));

        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = resp.getWriter()) {
                out.print("{\"error\":\"Error al buscar asambleistas.\"}");
            }
        }
    }


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

        return null;
    }


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


    private void volverAlFormularioConError(HttpServletRequest req,
                                            HttpServletResponse resp,
                                            String mensaje)
            throws ServletException, IOException {

        req.setAttribute("error", mensaje);

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
        }

        req.getRequestDispatcher("/views/asambleistas/asambleista-registro.jsp")
           .forward(req, resp);
    }


    private static String trim(String s) {
        return s == null ? null : s.trim();
    }


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