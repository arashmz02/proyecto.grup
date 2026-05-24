package controllers;
 
import config.Conexion;
import models.Sesion;
 
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
 
/*Controlador del modulo de Sesiones (Issue #10 Parte II).
Responsable: Josue 
 
NOTA DE INTEGRACION (post-merge):
- La tabla 'sesion' (singular) es de Frank (Issue #11). Ahora se
  adapta este controller a su esquema:
  * fecha (DATE) -> fecha_sesion (DATETIME2)
  * id_tipo_modalidad y link_acta YA NO van en 'sesion'
    -> se mueven al formulario/registro de 'acta'
  * se agrega 'total_convocados' como campo nuevo del formulario
- Las URLs (/sesiones, /sesiones/nueva, etc.) se mantienen.
 
VALIDACIONES:
- numero_sesion: formato 'AIR-NNN-AAAA' (ej. AIR-110-2024)
- fecha: no puede ser futura
- quorum: positivo y <= total_convocados (lo valida el CHECK de Azure)*/
@WebServlet(name = "SesionController", urlPatterns = {
    "/sesiones",
    "/sesiones/nueva",
    "/sesiones/detalle",
    "/sesiones/asistencia"
})
public class SesionController extends HttpServlet {
 
    //Formato 'AIR-NNN-AAAA' (ej. AIR-110-2024)
    private static final Pattern PATRON_NUMERO_SESION =
        Pattern.compile("^AIR-[0-9]{1,4}-[0-9]{4}$");
 
 
    /*GET: enrutado por ruta solicitada */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
 
        if (!AuthController.middlewareAuth(req, resp)) return;
 
        String ruta = req.getServletPath();
 
        switch (ruta) {
            case "/sesiones":
                listar(req, resp);
                break;
            case "/sesiones/nueva":
                mostrarFormulario(req, resp);
                break;
            case "/sesiones/detalle":
                mostrarDetalle(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
 
 
    /*POST: alta de sesion o registro de asistencia */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
 
        if (!AuthController.middlewareAuth(req, resp)) return;
 
        String ruta = req.getServletPath();
 
        switch (ruta) {
            case "/sesiones/nueva":
                procesarAlta(req, resp);
                break;
            case "/sesiones/asistencia":
                procesarAsistencia(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
 
 
    /*ACCION: listar sesiones */
    private void listar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
 
        try {
            List<Sesion> sesiones = Sesion.listarTodas();
            req.setAttribute("sesiones", sesiones);
 
            req.getRequestDispatcher("/views/sesiones/sesion-lista.jsp")
               .forward(req, resp);
 
        } catch (SQLException e) {
            e.printStackTrace();
            req.setAttribute("error",
                "No se pudo cargar la lista de sesiones. Intente mas tarde.");
            req.getRequestDispatcher("/views/sesiones/sesion-lista.jsp")
               .forward(req, resp);
        }
    }
 
 
    /*ACCION: mostrar formulario de nueva sesion Requiere permiso REGISTRAR_ASAMBLEISTAS. */
    private void mostrarFormulario(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
 
        if (!AuthController.middlewarePermiso(req, resp,
                "REGISTRAR_ASAMBLEISTAS")) {
            return;
        }
 
        try {
            req.setAttribute("tiposSesion", cargarOpciones("TIPO_SESION"));
            // Modalidades ahora se ofrecen pero se guardan en 'acta'
            req.setAttribute("modalidades", cargarOpciones("TIPO_MODALIDAD"));
 
            req.getRequestDispatcher("/views/sesiones/sesion-registro.jsp")
               .forward(req, resp);
 
        } catch (SQLException e) {
            e.printStackTrace();
            req.setAttribute("error", "No se pudieron cargar los catalogos.");
            req.getRequestDispatcher("/views/sesiones/sesion-registro.jsp")
               .forward(req, resp);
        }
    }
 
 
    /*ACCION: procesar alta del formulario */
    private void procesarAlta(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
 
        if (!AuthController.middlewarePermiso(req, resp,
                "REGISTRAR_ASAMBLEISTAS")) {
            return;
        }
 
        //1 Leer parametros
        String numeroSesion = trim(req.getParameter("numeroSesion"));
        String fechaStr     = req.getParameter("fecha");
        String tipoStr      = req.getParameter("idTipoSesion");
        String modStr       = req.getParameter("idTipoModalidad"); // modalidad se mueve a 'acta'
        String quorumStr    = req.getParameter("quorumRequerido");
        String totalStr     = req.getParameter("totalConvocados");
        String linkActa     = trim(req.getParameter("linkActa"));   // tambien va al 'acta'
 
        //2 Validaciones
        String error = validarCampos(numeroSesion, fechaStr, tipoStr,
                                     quorumStr, totalStr);
        if (error != null) {
            volverAlFormularioConError(req, resp, error);
            return;
        }
 
        try {
            //3 Verificar duplicado
            if (Sesion.obtenerPorNumero(numeroSesion) != null) {
                volverAlFormularioConError(req, resp,
                    "Ya existe una sesion con el numero " + numeroSesion + ".");
                return;
            }
 
            LocalDate fechaDate = LocalDate.parse(fechaStr);
            if (fechaDate.isAfter(LocalDate.now())) {
                volverAlFormularioConError(req, resp,
                    "La fecha de la sesion no puede ser futura.");
                return;
            }
 
            // La tabla 'sesion' de Frank requiere DATETIME2, asi que
            // se combina la fecha con hora 09:00 por defecto.
            LocalDateTime fechaSesion = LocalDateTime.of(fechaDate, LocalTime.of(9, 0));
 
            int idTipo    = Integer.parseInt(tipoStr);
            int quorum    = Integer.parseInt(quorumStr);
            int total     = Integer.parseInt(totalStr);
 
            if (quorum > total) {
                volverAlFormularioConError(req, resp,
                    "El quorum requerido no puede ser mayor al total de convocados.");
                return;
            }
 
            Integer idMod = (modStr != null && !modStr.isBlank())
                            ? Integer.parseInt(modStr) : null;
 
            //4 Crear sesion
            HttpSession httpSesion = req.getSession(false);
            int idUsuario = (Integer) httpSesion.getAttribute("idUsuario");
 
            int idSesionCreada = Sesion.crear(numeroSesion, fechaSesion,
                                              idTipo, quorum, total, idUsuario);
 
            //5 Si se proporciono modalidad o link, crear el acta vinculada
            if (idMod != null || (linkActa != null && !linkActa.isBlank())) {
                try {
                    Sesion.agregarActa(idSesionCreada, idMod, null,
                                       linkActa, linkActa, null);
                } catch (SQLException eActa) {
                    // No bloquear: la sesion ya quedo registrada
                    eActa.printStackTrace();
                }
            }
 
            resp.sendRedirect(req.getContextPath() + "/sesiones?creado=1");
 
        } catch (DateTimeParseException e) {
            volverAlFormularioConError(req, resp,
                "La fecha debe tener formato YYYY-MM-DD.");
 
        } catch (NumberFormatException e) {
            volverAlFormularioConError(req, resp,
                "Los campos numericos (tipo, modalidad, quorum, total) deben ser numericos.");
 
        } catch (SQLException e) {
            e.printStackTrace();
            volverAlFormularioConError(req, resp,
                "Error al guardar la sesion. Intente de nuevo.");
        }
    }
 
 
    /*ACCION: detalle de la sesion + asistencia */
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
            Sesion s = Sesion.obtenerPorId(id);
 
            if (s == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Sesion no encontrada.");
                return;
            }
 
            req.setAttribute("sesion", s);
            req.setAttribute("asistencia", Sesion.obtenerAsistenciaDeSesion(id));
            req.setAttribute("presentes", Sesion.contarPresentes(id));
            req.setAttribute("estadosAsistencia", cargarOpciones("ESTADO_ASISTENCIA"));
 
            req.getRequestDispatcher("/views/sesiones/sesion-detalle.jsp")
               .forward(req, resp);
 
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Id invalido.");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error al cargar el detalle.");
        }
    }
 
 
    /*ACCION: procesar registro de asistencia */
    private void procesarAsistencia(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
 
        if (!AuthController.middlewarePermiso(req, resp,
                "REGISTRAR_ASAMBLEISTAS")) {
            return;
        }
 
        String idSesionStr        = req.getParameter("idSesion");
        String idAsambleistaStr   = req.getParameter("idAsambleista");
        String idEstadoStr        = req.getParameter("idEstadoAsistencia");
 
        if (idSesionStr == null || idAsambleistaStr == null || idEstadoStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Faltan parametros para registrar asistencia.");
            return;
        }
 
        try {
            int idSesion       = Integer.parseInt(idSesionStr);
            int idAsambleista  = Integer.parseInt(idAsambleistaStr);
            int idEstado       = Integer.parseInt(idEstadoStr);
 
            Sesion.registrarAsistencia(idSesion, idAsambleista, idEstado);
 
            resp.sendRedirect(req.getContextPath() +
                "/sesiones/detalle?id=" + idSesion + "&asistencia=1");
 
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Los identificadores deben ser numericos.");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error al registrar la asistencia. Verifique que el asambleista " +
                "no este ya registrado en esta sesion.");
        }
    }
 
 
    /*HELPERS*/
 
    private String validarCampos(String numeroSesion, String fecha, String idTipo,
                                 String quorum, String total) {
 
        if (numeroSesion == null || numeroSesion.isBlank())
            return "El numero de sesion es obligatorio.";
        if (!PATRON_NUMERO_SESION.matcher(numeroSesion).matches())
            return "El numero de sesion debe tener formato AIR-NNN-AAAA (ej. AIR-110-2024).";
 
        if (fecha == null || fecha.isBlank())
            return "La fecha de la sesion es obligatoria.";
 
        if (idTipo == null || idTipo.isBlank())
            return "Debe seleccionar un tipo de sesion.";
 
        if (quorum == null || quorum.isBlank())
            return "El quorum requerido es obligatorio.";
 
        if (total == null || total.isBlank())
            return "El total de convocados es obligatorio.";
 
        try {
            int q = Integer.parseInt(quorum);
            if (q <= 0) return "El quorum debe ser mayor a 0.";
        } catch (NumberFormatException e) {
            return "El quorum debe ser un numero entero.";
        }
 
        try {
            int t = Integer.parseInt(total);
            if (t <= 0) return "El total de convocados debe ser mayor a 0.";
        } catch (NumberFormatException e) {
            return "El total de convocados debe ser un numero entero.";
        }
 
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
 
        req.setAttribute("numeroSesionPrev",    req.getParameter("numeroSesion"));
        req.setAttribute("fechaPrev",           req.getParameter("fecha"));
        req.setAttribute("idTipoSesionPrev",    req.getParameter("idTipoSesion"));
        req.setAttribute("idTipoModalidadPrev", req.getParameter("idTipoModalidad"));
        req.setAttribute("quorumPrev",          req.getParameter("quorumRequerido"));
        req.setAttribute("totalConvocadosPrev", req.getParameter("totalConvocados"));
        req.setAttribute("linkActaPrev",        req.getParameter("linkActa"));
 
        try {
            req.setAttribute("tiposSesion", cargarOpciones("TIPO_SESION"));
            req.setAttribute("modalidades", cargarOpciones("TIPO_MODALIDAD"));
        } catch (SQLException e) {
            //si fallan los catalogos, el error principal ya esta seteado
        }
 
        req.getRequestDispatcher("/views/sesiones/sesion-registro.jsp")
           .forward(req, resp);
    }
 
 
    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
 
 
    /*DTO interno para opciones de catalogos en las vistas*/
    public static class OpcionCatalogo {
        public int id;
        public String nombre;
 
        public int getId()        { return id; }
        public String getNombre() { return nombre; }
    }
}