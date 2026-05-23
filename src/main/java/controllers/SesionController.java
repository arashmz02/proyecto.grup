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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/*Controlador del modulo de Sesiones (Issue #10 Parte II).
Responsable: Josue - Issue #10 Parte II
VALIDACIONES:
- numero_sesion: formato 'AIR-NNN-AAAA' (ej. AIR-110-2024)
- fecha: no puede ser futura (no se registran sesiones del futuro)
- quorum: numero positivo*/
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


    /*ACCION: mostrar formulario de nueva sesion
    Requiere permiso REGISTRAR_ASAMBLEISTAS (mismo rol que gestiona sesiones). */
    private void mostrarFormulario(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!AuthController.middlewarePermiso(req, resp,
                "REGISTRAR_ASAMBLEISTAS")) {
            return;
        }

        try {
            req.setAttribute("tiposSesion",   cargarOpciones("TIPO_SESION"));
            req.setAttribute("modalidades",   cargarOpciones("TIPO_MODALIDAD"));

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
        String modStr       = req.getParameter("idTipoModalidad");
        String quorumStr    = req.getParameter("quorumRequerido");

        //2 Validaciones 
        String error = validarCampos(numeroSesion, fechaStr, tipoStr, modStr, quorumStr);
        if (error != null) {
            volverAlFormularioConError(req, resp, error);
            return;
        }

        //3 Parsear y verificar duplicado 
        try {
            if (Sesion.obtenerPorNumero(numeroSesion) != null) {
                volverAlFormularioConError(req, resp,
                    "Ya existe una sesion con el numero " + numeroSesion + ".");
                return;
            }

            LocalDate fecha = LocalDate.parse(fechaStr);
            if (fecha.isAfter(LocalDate.now())) {
                volverAlFormularioConError(req, resp,
                    "La fecha de la sesion no puede ser futura.");
                return;
            }

            int idTipo    = Integer.parseInt(tipoStr);
            int idMod     = Integer.parseInt(modStr);
            int quorum    = Integer.parseInt(quorumStr);

            //4 Crear 
            HttpSession sesion = req.getSession(false);
            int idUsuario = (Integer) sesion.getAttribute("idUsuario");

            Sesion.crear(numeroSesion, fecha, idTipo, idMod, quorum, idUsuario);

            resp.sendRedirect(req.getContextPath() + "/sesiones?creado=1");

        } catch (DateTimeParseException e) {
            volverAlFormularioConError(req, resp,
                "La fecha debe tener formato YYYY-MM-DD.");

        } catch (NumberFormatException e) {
            volverAlFormularioConError(req, resp,
                "El tipo, modalidad y quorum deben ser numericos.");

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

            //Para registrar asistencia: cargar todos los estados disponibles
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


    /*ACCION: procesar registro de asistencia (masivo o individual)*/
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
    /*Valida los campos del formulario de nueva sesion*/
    private String validarCampos(String numeroSesion, String fecha, String idTipo,
                                 String idMod, String quorum) {

        if (numeroSesion == null || numeroSesion.isBlank())
            return "El numero de sesion es obligatorio.";
        if (!PATRON_NUMERO_SESION.matcher(numeroSesion).matches())
            return "El numero de sesion debe tener formato AIR-NNN-AAAA (ej. AIR-110-2024).";

        if (fecha == null || fecha.isBlank())
            return "La fecha de la sesion es obligatoria.";

        if (idTipo == null || idTipo.isBlank())
            return "Debe seleccionar un tipo de sesion.";

        if (idMod == null || idMod.isBlank())
            return "Debe seleccionar una modalidad.";

        if (quorum == null || quorum.isBlank())
            return "El quorum requerido es obligatorio.";

        try {
            int q = Integer.parseInt(quorum);
            if (q < 0) return "El quorum no puede ser negativo.";
        } catch (NumberFormatException e) {
            return "El quorum debe ser un numero entero.";
        }

        return null;
    }


    /*Carga las opciones de un grupo del catalogo_maestro*/
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


    /*Reenvia al formulario con mensaje de error preservando lo que el usuario habia escrito*/
    private void volverAlFormularioConError(HttpServletRequest req,
                                            HttpServletResponse resp,
                                            String mensaje)
            throws ServletException, IOException {

        req.setAttribute("error", mensaje);

        req.setAttribute("numeroSesionPrev",   req.getParameter("numeroSesion"));
        req.setAttribute("fechaPrev",          req.getParameter("fecha"));
        req.setAttribute("idTipoSesionPrev",   req.getParameter("idTipoSesion"));
        req.setAttribute("idTipoModalidadPrev", req.getParameter("idTipoModalidad"));
        req.setAttribute("quorumPrev",         req.getParameter("quorumRequerido"));

        try {
            req.setAttribute("tiposSesion", cargarOpciones("TIPO_SESION"));
            req.setAttribute("modalidades", cargarOpciones("TIPO_MODALIDAD"));
        } catch (SQLException e) {
            //si fallan los catalogos, el error principal ya esta seteado
        }

        req.getRequestDispatcher("/views/sesiones/sesion-registro.jsp")
           .forward(req, resp);
    }


    /*Trim seguro: devuelve null si el input es null*/
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