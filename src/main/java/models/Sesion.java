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

/*Modelo del modulo de Sesiones (Issue #10 Parte II).
Responsable: Josue - Issue #10 Parte II*/
public class Sesion {

    private int idSesion;
    private String numeroSesion;
    private LocalDate fecha;
    private int idTipoSesion;
    private int idTipoModalidad;
    private int quorumRequerido;
    private String linkActa;

    //Getters 
    public int getIdSesion()         { return idSesion; }
    public String getNumeroSesion()  { return numeroSesion; }
    public LocalDate getFecha()      { return fecha; }
    public int getIdTipoSesion()     { return idTipoSesion; }
    public int getIdTipoModalidad()  { return idTipoModalidad; }
    public int getQuorumRequerido()  { return quorumRequerido; }
    public String getLinkActa()      { return linkActa; }


    /*Crea una nueva sesion plenaria.
     * @param numeroSesion    codigo unico (ej. 'AIR-110-2024')
     * @param fecha           fecha de la sesion
     * @param idTipoSesion    FK a catalogo_maestro (grupo TIPO_SESION)
     * @param idTipoModalidad FK a catalogo_maestro (grupo TIPO_MODALIDAD)
     * @param quorumRequerido cantidad minima de asistentes
     * @param idUsuario       usuario que registra (para auditoria futura)
     * @return id de la sesion creada
     */
    public static int crear(String numeroSesion, LocalDate fecha,
                            int idTipoSesion, int idTipoModalidad,
                            int quorumRequerido, int idUsuario) throws SQLException {

        String sql = "INSERT INTO sesiones " +
                     "(numero_sesion, fecha, id_tipo_sesion, id_tipo_modalidad, quorum_requerido) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = Conexion.obtener()) {
            Conexion.establecerContexto(con, idUsuario, null);

            try (PreparedStatement ps = con.prepareStatement(
                    sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, numeroSesion);
                ps.setDate(2, Date.valueOf(fecha));
                ps.setInt(3, idTipoSesion);
                ps.setInt(4, idTipoModalidad);
                ps.setInt(5, quorumRequerido);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
                throw new SQLException("No se pudo obtener el id de la sesion creada.");
            }
        }
    }


    /*Busca una sesion por su id.*/
    public static Sesion obtenerPorId(int id) throws SQLException {
        String sql = "SELECT id_sesion, numero_sesion, fecha, id_tipo_sesion, " +
                     "       id_tipo_modalidad, quorum_requerido, link_acta " +
                     "FROM sesiones WHERE id_sesion = ?";

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


    /*Busca una sesion por su numero (codigo unico).*/
    public static Sesion obtenerPorNumero(String numeroSesion) throws SQLException {
        String sql = "SELECT id_sesion, numero_sesion, fecha, id_tipo_sesion, " +
                     "       id_tipo_modalidad, quorum_requerido, link_acta " +
                     "FROM sesiones WHERE numero_sesion = ?";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, numeroSesion);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }


    /*Lista todas las sesiones ordenadas por fecha descendente(las mas recientes primero)*/
    public static List<Sesion> listarTodas() throws SQLException {
        String sql = "SELECT id_sesion, numero_sesion, fecha, id_tipo_sesion, " +
                     "       id_tipo_modalidad, quorum_requerido, link_acta " +
                     "FROM sesiones ORDER BY fecha DESC";

        List<Sesion> lista = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }


    /*Registra el acta formal de una sesion. La tabla acta tieneUNIQUE en id_sesion, asi que solo puede haber un acta porsesion. Si ya existe, este metodo lanza SQLException.*/
    public static int agregarActa(int idSesion, LocalDate fechaAprobacion,
                                  String urlDocumento, String observaciones)
            throws SQLException {

        String sql = "INSERT INTO acta (id_sesion, fecha_aprobacion, url_documento, observaciones) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(
                     sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, idSesion);

            if (fechaAprobacion != null) {
                ps.setDate(2, Date.valueOf(fechaAprobacion));
            } else {
                ps.setNull(2, java.sql.Types.DATE);
            }

            ps.setString(3, urlDocumento);
            ps.setString(4, observaciones);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            throw new SQLException("No se pudo obtener el id del acta creada.");
        }
    }


    /*Registra la asistencia de un asambleista a una sesion.
    El UNIQUE (id_sesion, id_asambleista) impide registrosduplicados para la misma persona en la misma sesion.*/
    public static void registrarAsistencia(int idSesion, int idAsambleista,
                                           int idEstadoAsistencia) throws SQLException {

        String sql = "INSERT INTO asistencia_sesion_plenaria " +
                     "(id_sesion, id_asambleista, id_estado_asistencia) " +
                     "VALUES (?, ?, ?)";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idSesion);
            ps.setInt(2, idAsambleista);
            ps.setInt(3, idEstadoAsistencia);
            ps.executeUpdate();
        }
    }


    /*Cuenta cuantos asambleistas figuran como 'Presente' en una sesion.
    Util para que el Controller valide si se cumplio elquorum requerido antes de habilitar votaciones.*/
    public static int contarPresentes(int idSesion) throws SQLException {
        String sql = "SELECT COUNT(*) AS total " +
                     "FROM asistencia_sesion_plenaria a " +
                     "JOIN catalogo_maestro c ON a.id_estado_asistencia = c.id_item " +
                     "WHERE a.id_sesion = ? " +
                     "  AND c.grupo_catalogo = 'ESTADO_ASISTENCIA' " +
                     "  AND c.nombre = 'Presente'";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idSesion);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }


    /*Devuelve el detalle de asistencia de una sesion: lista de asambleistas con su estado (Presente, Ausente, Justificado).
    Util para la vista de detalle de sesion y para certificaciones.*/
    public static List<AsistenciaVista> obtenerAsistenciaDeSesion(int idSesion)
            throws SQLException {

        String sql =
            "SELECT a.id_asistencia, a.id_asambleista, " +
            "       asm.nombre AS nombre_asambleista, asm.cedula, " +
            "       c.nombre AS estado " +
            "FROM asistencia_sesion_plenaria a " +
            "JOIN asambleista asm ON a.id_asambleista = asm.id_asambleista " +
            "JOIN catalogo_maestro c ON a.id_estado_asistencia = c.id_item " +
            "WHERE a.id_sesion = ? " +
            "ORDER BY asm.nombre";

        List<AsistenciaVista> lista = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idSesion);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AsistenciaVista av = new AsistenciaVista();
                    av.idAsistencia       = rs.getInt("id_asistencia");
                    av.idAsambleista      = rs.getInt("id_asambleista");
                    av.nombreAsambleista  = rs.getString("nombre_asambleista");
                    av.cedula             = rs.getString("cedula");
                    av.estado             = rs.getString("estado");
                    lista.add(av);
                }
            }
        }
        return lista;
    }


    /*Helper privado: convierte una fila del ResultSet en un objeto Sesion. Evita repetir el codigo de mapeo.*/
    private static Sesion mapear(ResultSet rs) throws SQLException {
        Sesion s = new Sesion();
        s.idSesion        = rs.getInt("id_sesion");
        s.numeroSesion    = rs.getString("numero_sesion");
        s.fecha           = rs.getDate("fecha").toLocalDate();
        s.idTipoSesion    = rs.getInt("id_tipo_sesion");
        s.idTipoModalidad = rs.getInt("id_tipo_modalidad");
        s.quorumRequerido = rs.getInt("quorum_requerido");
        s.linkActa        = rs.getString("link_acta");
        return s;
    }


    /*DTO para vistas: asistencia con datos del asambleista resueltos.La vista accede a los campos directamente. */
    public static class AsistenciaVista {
        public int idAsistencia;
        public int idAsambleista;
        public String nombreAsambleista;
        public String cedula;
        public String estado;

        //Getters para la JSP
        public int getIdAsistencia()         { return idAsistencia; }
        public int getIdAsambleista()        { return idAsambleista; }
        public String getNombreAsambleista() { return nombreAsambleista; }
        public String getCedula()            { return cedula; }
        public String getEstado()            { return estado; }
    }
}