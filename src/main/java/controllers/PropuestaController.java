package controllers;

import config.Conexion;
import models.Propuesta;

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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/*Controlador del modulo de Propuestas (Issue #10 Parte II).
Responsable: Josue - Issue #10 Parte II
VALIDACIONES:
- codigo_air: formato 'AIR-NN-AAAA' o 'AIR-NN-CONC-AAAA'
- titulo: obligatorio, max 300 caracteres
- debe seleccionarse al menos 1 proponente*/
@WebServlet(name = "PropuestaController", urlPatterns = {
    "/propuestas",
    "/propuestas/nueva",
    "/propuestas/detalle",
    "/propuestas/cambiar-estado"
})
public class PropuestaController extends HttpServlet {

    //Formato 'AIR-NN-AAAA' o 'AIR-NN-CONC-AAAA'
    private static final Pattern PATRON_CODIGO_AIR =
        Pattern.compile("^AIR-[0-9]{1,4}(-CONC)?-[0-9]{4}$");


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!AuthController.middlewareAuth(req, resp)) return;

        String ruta = req.getServletPath();

        switch (ruta) {
            case "/propuestas":
                listar(req, resp);
                break;
            case "/propuestas/nueva":
                mostrarFormulario(req, resp);
                break;
            case "/propuestas/detalle":
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
            case "/propuestas/nueva":
                procesarAlta(req, resp);
                break;
            case "/propuestas/cambiar-estado":
                procesarCambioEstado(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }


    /*ACCION: listar propuestas (con filtro opcional por estado)*/
    private void listar(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String filtroEstado = req.getParameter("estado");   // opcional

        try {
            List<Propuesta> propuestas;

            if (filtroEstado != null && !filtroEstado.isBlank()) {
                int idEstado = Integer.parseInt(filtroEstado);
                propuestas = Propuesta.listarPorEstado(idEstado);
                req.setAttribute("filtroEstadoActivo", idEstado);
            } else {
                propuestas = Propuesta.listarTodas();
            }

            req.setAttribute("propuestas", propuestas);
            req.setAttribute("estados", cargarOpciones("ESTADO_PROPUESTA"));

            req.getRequestDispatcher("/views/propuestas/propuesta-lista.jsp")
               .forward(req, resp);

        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Estado invalido.");
        } catch (SQLException e) {
            e.printStackTrace();
            req.setAttribute("error",
                "No se pudo cargar la lista de propuestas. Intente mas tarde.");
            req.getRequestDispatcher("/views/propuestas/propuesta-lista.jsp")
               .forward(req, resp);
        }
    }


    /*ACCION: mostrar formulario de nueva propuesta*/
    private void mostrarFormulario(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!AuthController.middlewarePermiso(req, resp,
                "REGISTRAR_ASAMBLEISTAS")) {
            return;
        }

        try {
            req.setAttribute("etapas",     cargarOpciones("ETAPA_PROPUESTA"));
            req.setAttribute("estados",    cargarOpciones("ESTADO_PROPUESTA"));
            req.setAttribute("mayorias",   cargarOpciones("TIPO_MAYORIA"));
            req.setAttribute("reglamentos", cargarReglamentos());
            req.setAttribute("asambleistas", cargarAsambleistas());

            req.getRequestDispatcher("/views/propuestas/propuesta-registro.jsp")
               .forward(req, resp);

        } catch (SQLException e) {
            e.printStackTrace();
            req.setAttribute("error", "No se pudieron cargar los catalogos.");
            req.getRequestDispatcher("/views/propuestas/propuesta-registro.jsp")
               .forward(req, resp);
        }
    }


    /*ACCION: procesar alta del formulario*/
    private void procesarAlta(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!AuthController.middlewarePermiso(req, resp,
                "REGISTRAR_ASAMBLEISTAS")) {
            return;
        }

        //1 Leer parametros 
        String codigoAir         = trim(req.getParameter("codigoAir"));
        String titulo            = trim(req.getParameter("titulo"));
        String textoSustitutivo  = req.getParameter("textoSustitutivo");
        String idReglamentoStr   = req.getParameter("idReglamentoBase");
        String idEtapaStr        = req.getParameter("idEtapaPropuesta");
        String idEstadoStr       = req.getParameter("idEstadoPropuesta");
        String idMayoriaStr      = req.getParameter("idTipoMayoria");
        String linkDoc           = trim(req.getParameter("linkDocumentacion"));
        String[] idsProponentes  = req.getParameterValues("idProponente");

        //2 Validaciones 
        String error = validarCampos(codigoAir, titulo, idEtapaStr, idEstadoStr,
                                     idMayoriaStr, idsProponentes);
        if (error != null) {
            volverAlFormularioConError(req, resp, error);
            return;
        }

        //3 Verificar duplicado de codigo_air 
        try {
            if (Propuesta.obtenerPorCodigoAir(codigoAir) != null) {
                volverAlFormularioConError(req, resp,
                    "Ya existe una propuesta con el codigo " + codigoAir + ".");
                return;
            }

            Integer idReglamento = (idReglamentoStr == null || idReglamentoStr.isBlank())
                                   ? null : Integer.parseInt(idReglamentoStr);
            int idEtapa   = Integer.parseInt(idEtapaStr);
            int idEstado  = Integer.parseInt(idEstadoStr);
            int idMayoria = Integer.parseInt(idMayoriaStr);

            //4 Crear propuesta + proponentes 
            HttpSession sesion = req.getSession(false);
            int idUsuario = (Integer) sesion.getAttribute("idUsuario");

            int idPropuesta = Propuesta.crear(codigoAir, titulo, textoSustitutivo,
                                              idReglamento, idEtapa, idEstado,
                                              idMayoria, linkDoc, idUsuario);

            //Agregar cada proponente seleccionado
            for (String idProponenteStr : idsProponentes) {
                int idAsambleista = Integer.parseInt(idProponenteStr);
                Propuesta.agregarProponente(idPropuesta, idAsambleista);
            }

            resp.sendRedirect(req.getContextPath() + "/propuestas?creado=1");

        } catch (NumberFormatException e) {
            volverAlFormularioConError(req, resp,
                "Los identificadores deben ser numericos.");

        } catch (SQLException e) {
            e.printStackTrace();
            volverAlFormularioConError(req, resp,
                "Error al guardar la propuesta. Intente de nuevo.");
        }
    }


    /*ACCION: detalle de la propuesta*/
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
            Propuesta p = Propuesta.obtenerPorId(id);

            if (p == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Propuesta no encontrada.");
                return;
            }

            req.setAttribute("propuesta",  p);
            req.setAttribute("proponentes", Propuesta.obtenerProponentes(id));
            req.setAttribute("historial",   Propuesta.obtenerHistorial(id));
            req.setAttribute("conciliadas", Propuesta.obtenerConciliadas(id));
            req.setAttribute("estados",     cargarOpciones("ESTADO_PROPUESTA"));

            req.getRequestDispatcher("/views/propuestas/propuesta-detalle.jsp")
               .forward(req, resp);

        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Id invalido.");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error al cargar el detalle.");
        }
    }


    /*ACCION: cambiar el estado de una propuesta*/
    private void procesarCambioEstado(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!AuthController.middlewarePermiso(req, resp,
                "REGISTRAR_ASAMBLEISTAS")) {
            return;
        }

        String idPropuestaStr = req.getParameter("idPropuesta");
        String nuevoEstadoStr = req.getParameter("idNuevoEstado");

        if (idPropuestaStr == null || nuevoEstadoStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Faltan parametros para cambiar estado.");
            return;
        }

        try {
            int idPropuesta = Integer.parseInt(idPropuestaStr);
            int nuevoEstado = Integer.parseInt(nuevoEstadoStr);

            HttpSession sesion = req.getSession(false);
            int idUsuario = (Integer) sesion.getAttribute("idUsuario");

            Propuesta.cambiarEstado(idPropuesta, nuevoEstado, idUsuario);

            resp.sendRedirect(req.getContextPath() +
                "/propuestas/detalle?id=" + idPropuesta + "&estadoCambiado=1");

        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Los identificadores deben ser numericos.");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Error al cambiar el estado de la propuesta.");
        }
    }


    /*HELPERS*/

    private String validarCampos(String codigoAir, String titulo, String idEtapa,
                                 String idEstado, String idMayoria,
                                 String[] proponentes) {

        if (codigoAir == null || codigoAir.isBlank())
            return "El codigo AIR es obligatorio.";
        if (!PATRON_CODIGO_AIR.matcher(codigoAir).matches())
            return "El codigo debe tener formato AIR-NN-AAAA o AIR-NN-CONC-AAAA.";

        if (titulo == null || titulo.isBlank())
            return "El titulo es obligatorio.";
        if (titulo.length() > 300)
            return "El titulo no puede exceder 300 caracteres.";

        if (idEtapa == null || idEtapa.isBlank())
            return "Debe seleccionar una etapa.";

        if (idEstado == null || idEstado.isBlank())
            return "Debe seleccionar un estado.";

        if (idMayoria == null || idMayoria.isBlank())
            return "Debe seleccionar un tipo de mayoria.";

        if (proponentes == null || proponentes.length == 0)
            return "Debe seleccionar al menos un proponente.";

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


    /*Carga la lista de reglamentos para el selector del formulario*/
    private List<OpcionCatalogo> cargarReglamentos() throws SQLException {
        String sql = "SELECT id_reglamento AS id, nombre_normativa AS nombre " +
                     "FROM reglamento ORDER BY nombre_normativa";

        List<OpcionCatalogo> opciones = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                OpcionCatalogo o = new OpcionCatalogo();
                o.id     = rs.getInt("id");
                o.nombre = rs.getString("nombre");
                opciones.add(o);
            }
        }
        return opciones;
    }


    /*Carga la lista de asambleistas para el selector multiple*/
    private List<OpcionCatalogo> cargarAsambleistas() throws SQLException {
        String sql = "SELECT id_asambleista AS id, nombre " +
                     "FROM asambleista ORDER BY nombre";

        List<OpcionCatalogo> opciones = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                OpcionCatalogo o = new OpcionCatalogo();
                o.id     = rs.getInt("id");
                o.nombre = rs.getString("nombre");
                opciones.add(o);
            }
        }
        return opciones;
    }


    private void volverAlFormularioConError(HttpServletRequest req,
                                            HttpServletResponse resp,
                                            String mensaje)
            throws ServletException, IOException {

        req.setAttribute("error", mensaje);

        req.setAttribute("codigoAirPrev",         req.getParameter("codigoAir"));
        req.setAttribute("tituloPrev",            req.getParameter("titulo"));
        req.setAttribute("textoSustitutivoPrev",  req.getParameter("textoSustitutivo"));
        req.setAttribute("idReglamentoBasePrev",  req.getParameter("idReglamentoBase"));
        req.setAttribute("idEtapaPrev",           req.getParameter("idEtapaPropuesta"));
        req.setAttribute("idEstadoPrev",          req.getParameter("idEstadoPropuesta"));
        req.setAttribute("idMayoriaPrev",         req.getParameter("idTipoMayoria"));
        req.setAttribute("linkDocPrev",           req.getParameter("linkDocumentacion"));

        try {
            req.setAttribute("etapas",       cargarOpciones("ETAPA_PROPUESTA"));
            req.setAttribute("estados",      cargarOpciones("ESTADO_PROPUESTA"));
            req.setAttribute("mayorias",     cargarOpciones("TIPO_MAYORIA"));
            req.setAttribute("reglamentos",  cargarReglamentos());
            req.setAttribute("asambleistas", cargarAsambleistas());
        } catch (SQLException e) {
            //no sobreescribir el error principal
        }

        req.getRequestDispatcher("/views/propuestas/propuesta-registro.jsp")
           .forward(req, resp);
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