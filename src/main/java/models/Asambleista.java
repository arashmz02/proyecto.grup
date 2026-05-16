package models;

import config.Conexion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de la entidad asambleista (Issue #9).
 *
 * Encapsula el acceso a las tablas:
 *   - asambleista
 *   - nombramiento
 *   - bitacora_asambleistas (escrita por el trigger tg_cambio_identidad)
 *
 * IMPORTANTE - triggers de la BD:
 *   - tg_traslape_sector: rechaza nombramientos con fechas solapadas
 *     (lanza error con numero 50001). El controlador captura este
 *     caso y muestra un mensaje al usuario.
 *   - tg_auditoria_asambleista / tg_auditoria_nombramiento: registran
 *     INSERT/UPDATE/DELETE en sys_log_auditoria. Necesitan que la
 *     conexion tenga el contexto de sesion con el id del usuario.
 *     Por eso TODOS los metodos que escriben llaman a
 *     Conexion.establecerContexto() antes del INSERT/UPDATE.
 *
 * Es responsabilidad del controlador pasar el idUsuario actual al
 * llamar a estos metodos.
 */
public class Asambleista {

    private int idAsambleista;
    private String cedula;
    private String nombre;
    private String correoInstitucional;

    // ----- Getters -----
    public int getIdAsambleista()           { return idAsambleista; }
    public String getCedula()               { return cedula; }
    public String getNombre()               { return nombre; }
    public String getCorreoInstitucional()  { return correoInstitucional; }


    /**
     * Crea un nuevo asambleista. Antes del INSERT establece el
     * contexto de sesion para que el trigger de auditoria sepa
     * quien hizo el cambio.
     *
     * @param cedula  cedula formateada (ej. 1-1234-5678)
     * @param nombre  nombre completo
     * @param correo  correo institucional
     * @param idUsuarioRegistro  id del usuario logueado que registra
     * @return el id generado para el nuevo asambleista
     * @throws SQLException si la cedula o correo ya existen, etc.
     */
    public static int crear(String cedula, String nombre, String correo,
                            int idUsuarioRegistro) throws SQLException {

        String sql = "INSERT INTO asambleista (cedula, nombre, correo_institucional) " +
                     "VALUES (?, ?, ?)";

        try (Connection con = Conexion.obtener()) {
            // Trigger de auditoria necesita el id del usuario
            Conexion.establecerContexto(con, idUsuarioRegistro, null);

            try (PreparedStatement ps = con.prepareStatement(
                    sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, cedula);
                ps.setString(2, nombre);
                ps.setString(3, correo);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
                throw new SQLException("No se pudo obtener el id del asambleista creado.");
            }
        }
    }


    /**
     * Busca un asambleista por su id.
     */
    public static Asambleista obtenerPorId(int id) throws SQLException {
        String sql = "SELECT id_asambleista, cedula, nombre, correo_institucional " +
                     "FROM asambleista WHERE id_asambleista = ?";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }


    /**
     * Busca un asambleista por su cedula. Util para detectar
     * duplicados antes de un alta y mostrar mensaje claro al
     * usuario en lugar de un error generico de constraint.
     */
    public static Asambleista obtenerPorCedula(String cedula) throws SQLException {
        String sql = "SELECT id_asambleista, cedula, nombre, correo_institucional " +
                     "FROM asambleista WHERE cedula = ?";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, cedula);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }


    /**
     * Lista todos los asambleistas ordenados por nombre.
     */
    public static List<Asambleista> listarTodos() throws SQLException {
        String sql = "SELECT id_asambleista, cedula, nombre, correo_institucional " +
                     "FROM asambleista ORDER BY nombre";

        List<Asambleista> lista = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }


    /**
     * Indica si el asambleista tiene al menos un nombramiento
     * vigente a la fecha actual. Vigente significa que la fecha de
     * inicio ya paso y la fecha fin es nula o posterior a hoy.
     */
    public static boolean estaVigente(int idAsambleista) throws SQLException {
        String sql = "SELECT 1 FROM nombramiento " +
                     "WHERE id_asambleista = ? " +
                     "  AND fecha_inicio <= CAST(SYSUTCDATETIME() AS DATE) " +
                     "  AND (fecha_fin IS NULL OR fecha_fin >= CAST(SYSUTCDATETIME() AS DATE)) " +
                     "  AND estado = 'Vigente'";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idAsambleista);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();   // true si hay al menos una fila
            }
        }
    }


    /**
     * Registra un nuevo nombramiento para un asambleista.
     *
     * Si las fechas se solapan con un nombramiento existente del
     * mismo asambleista, el trigger tg_traslape_sector lanza un
     * SQLException con error number 50001. El controlador debe
     * capturar ese caso especifico.
     *
     * @return id del nombramiento creado
     */
    public static int agregarNombramiento(int idAsambleista,
                                          int idSector,
                                          Integer idPuesto,
                                          LocalDate fechaInicio,
                                          LocalDate fechaFin,
                                          int idUsuarioRegistro) throws SQLException {

        String sql = "INSERT INTO nombramiento " +
                     "(id_asambleista, id_sector, id_puesto, fecha_inicio, fecha_fin, " +
                     " estado, id_usuario_registro) " +
                     "VALUES (?, ?, ?, ?, ?, 'Vigente', ?)";

        try (Connection con = Conexion.obtener()) {
            Conexion.establecerContexto(con, idUsuarioRegistro, null);

            try (PreparedStatement ps = con.prepareStatement(
                    sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, idAsambleista);
                ps.setInt(2, idSector);

                if (idPuesto != null) {
                    ps.setInt(3, idPuesto);
                } else {
                    ps.setNull(3, java.sql.Types.INTEGER);
                }

                ps.setDate(4, Date.valueOf(fechaInicio));

                if (fechaFin != null) {
                    ps.setDate(5, Date.valueOf(fechaFin));
                } else {
                    ps.setNull(5, java.sql.Types.DATE);
                }

                ps.setInt(6, idUsuarioRegistro);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
                throw new SQLException("No se pudo obtener el id del nombramiento creado.");
            }
        }
    }


    /**
     * Devuelve el historial completo de nombramientos de un
     * asambleista, ordenado del mas reciente al mas antiguo.
     * Se incluyen los nombres del sector y puesto resolviendo
     * los joins contra catalogo_maestro.
     */
    public static List<NombramientoVista> obtenerHistorialNombramientos(int idAsambleista)
            throws SQLException {

        String sql =
            "SELECT n.id_nombramiento, n.fecha_inicio, n.fecha_fin, n.estado, " +
            "       cs.nombre AS sector, cp.nombre AS puesto " +
            "FROM nombramiento n " +
            "JOIN catalogo_maestro cs ON n.id_sector = cs.id_item " +
            "LEFT JOIN catalogo_maestro cp ON n.id_puesto = cp.id_item " +
            "WHERE n.id_asambleista = ? " +
            "ORDER BY n.fecha_inicio DESC";

        List<NombramientoVista> historial = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idAsambleista);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NombramientoVista nv = new NombramientoVista();
                    nv.idNombramiento = rs.getInt("id_nombramiento");
                    nv.fechaInicio    = rs.getDate("fecha_inicio").toLocalDate();
                    Date ff           = rs.getDate("fecha_fin");
                    nv.fechaFin       = (ff != null) ? ff.toLocalDate() : null;
                    nv.estado         = rs.getString("estado");
                    nv.sector         = rs.getString("sector");
                    nv.puesto         = rs.getString("puesto");
                    historial.add(nv);
                }
            }
        }
        return historial;
    }


    /**
     * Helper privado: convierte una fila del ResultSet en un
     * objeto Asambleista. Evita repetir el codigo de mapeo.
     */
    private static Asambleista mapear(ResultSet rs) throws SQLException {
        Asambleista a = new Asambleista();
        a.idAsambleista       = rs.getInt("id_asambleista");
        a.cedula              = rs.getString("cedula");
        a.nombre              = rs.getString("nombre");
        a.correoInstitucional = rs.getString("correo_institucional");
        return a;
    }


    /* ============================================================
       DTO para vistas: nombramientos con datos resueltos.
       Se mantiene como clase interna publica para no contaminar
       el paquete models con tipos pequenos. La vista accede a
       los campos directamente.
       ============================================================ */
    public static class NombramientoVista {
        public int idNombramiento;
        public LocalDate fechaInicio;
        public LocalDate fechaFin;        // puede ser null
        public String estado;
        public String sector;
        public String puesto;             // puede ser null

        // Helpers para la vista (formatear sin meter logica en la JSP)
        public String getFechaInicioFormateada() {
            return fechaInicio.toString();
        }

        public String getFechaFinFormateada() {
            return fechaFin == null ? "Vigente" : fechaFin.toString();
        }
    }
}
