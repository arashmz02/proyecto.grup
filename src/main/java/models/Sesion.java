package models;
 
import config.Conexion;
 
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
 
/*Modelo del modulo de Sesiones (Issue #10 Parte II).
Responsable: Josue 
 
NOTA DE INTEGRACION (post-merge):
- La tabla 'sesion' (singular) es propiedad de Frank (Issue #11).
- Este modelo se integra con esa tabla, no crea una propia.
- Campos como id_tipo_modalidad y link_acta se almacenan en la
  tabla 'acta' (propiedad de este modulo) para no contaminar la
  tabla 'sesion' de Frank.*/
public class Sesion {
    private int idSesion;
    private String numeroSesion;
    private LocalDateTime fechaSesion;
    private int idTipoSesion;
    private int quorumRequerido;
    private int totalConvocados;
    private boolean cerrada;
 
    //Getters
    public int getIdSesion()             { return idSesion; }
    public String getNumeroSesion()      { return numeroSesion; }
    public LocalDateTime getFechaSesion(){ return fechaSesion; }
    public int getIdTipoSesion()         { return idTipoSesion; }
    public int getQuorumRequerido()      { return quorumRequerido; }
    public int getTotalConvocados()      { return totalConvocados; }
    public boolean isCerrada()           { return cerrada; }
 
 
    /*Crea una nueva sesion plenaria en la tabla 'sesion' (Issue #11).
     * @param numeroSesion    codigo unico (ej. 'AIR-110-2024')
     * @param fechaSesion     fecha y hora de la sesion
     * @param idTipoSesion    FK a catalogo_maestro (grupo TIPO_SESION)
     * @param quorumRequerido cantidad minima de asistentes
     * @param totalConvocados cantidad total de asambleistas convocados
     * @param idUsuario       usuario que registra (para auditoria futura)
     * @return id de la sesion creada
     */
    public static int crear(String numeroSesion, LocalDateTime fechaSesion,
                            int idTipoSesion, int quorumRequerido,
                            int totalConvocados, int idUsuario) throws SQLException {
 
        String sql = "INSERT INTO sesion " +
                     "(numero_sesion, fecha_sesion, id_tipo_sesion, " +
                     " quorum_requerido, total_convocados) " +
                     "VALUES (?, ?, ?, ?, ?)";
 
        try (Connection con = Conexion.obtener()) {
            Conexion.establecerContexto(con, idUsuario, null);
 
            try (PreparedStatement ps = con.prepareStatement(
                    sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
 
                ps.setString(1, numeroSesion);
                ps.setTimestamp(2, Timestamp.valueOf(fechaSesion));
                ps.setInt(3, idTipoSesion);
                ps.setInt(4, quorumRequerido);
                ps.setInt(5, totalConvocados);
 
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
        String sql = "SELECT id_sesion, numero_sesion, fecha_sesion, id_tipo_sesion, " +
                     "       quorum_requerido, total_convocados, cerrada " +
                     "FROM sesion WHERE id_sesion = ?";
 
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
        String sql = "SELECT id_sesion, numero_sesion, fecha_sesion, id_tipo_sesion, " +
                     "       quorum_requerido, total_convocados, cerrada " +
                     "FROM sesion WHERE numero_sesion = ?";
 
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
 
 
    /*Lista todas las sesiones ordenadas por fecha descendente (las mas recientes primero).*/
    public static List<Sesion> listarTodas() throws SQLException {
        String sql = "SELECT id_sesion, numero_sesion, fecha_sesion, id_tipo_sesion, " +
                     "       quorum_requerido, total_convocados, cerrada " +
                     "FROM sesion ORDER BY fecha_sesion DESC";
 
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
 
 
    /*Registra el acta formal de una sesion. La tabla acta tiene UNIQUE en id_sesion, asi que solo puede haber un acta por sesion. 
    Si ya existe, este metodo lanza SQLException.
    
    NOTA: id_tipo_modalidad y link_acta viven en 'acta' (no en'sesion' que es de Frank).*/
    public static int agregarActa(int idSesion, Integer idTipoModalidad,
                                  LocalDate fechaAprobacion, String urlDocumento,
                                  String linkActa, String observaciones)
            throws SQLException {
 
        String sql = "INSERT INTO acta " +
                     "(id_sesion, id_tipo_modalidad, fecha_aprobacion, " +
                     " url_documento, link_acta, observaciones) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
 
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(
                     sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
 
            ps.setInt(1, idSesion);
 
            if (idTipoModalidad != null) ps.setInt(2, idTipoModalidad);
            else ps.setNull(2, java.sql.Types.INTEGER);
 
            if (fechaAprobacion != null) {
                ps.setDate(3, Date.valueOf(fechaAprobacion));
            } else {
                ps.setNull(3, java.sql.Types.DATE);
            }
 
            ps.setString(4, urlDocumento);
            ps.setString(5, linkActa);
            ps.setString(6, observaciones);
 
            ps.executeUpdate();
 
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            throw new SQLException("No se pudo obtener el id del acta creada.");
        }
    }
 
 
    /*Registra la asistencia de un asambleista a una sesion. El UNIQUE (id_sesion, id_asambleista) impide registros
    duplicados para la misma persona en la misma sesion.*/
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
 
 
    /*Cuenta cuantos asambleistas figuran como 'Presente' en una sesion. Util para que el Controller valide si se cumplio el
    quorum requerido antes de habilitar votaciones.*/
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
 
 
    /*Devuelve el detalle de asistencia de una sesion: lista de
    asambleistas con su estado (Presente, Ausente, Justificado).
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
 
 
    /*Helper privado: convierte una fila del ResultSet en un objeto
    Sesion. Evita repetir el codigo de mapeo.*/
    private static Sesion mapear(ResultSet rs) throws SQLException {
        Sesion s = new Sesion();
        s.idSesion        = rs.getInt("id_sesion");
        s.numeroSesion    = rs.getString("numero_sesion");
        s.fechaSesion     = rs.getTimestamp("fecha_sesion").toLocalDateTime();
        s.idTipoSesion    = rs.getInt("id_tipo_sesion");
        s.quorumRequerido = rs.getInt("quorum_requerido");
        s.totalConvocados = rs.getInt("total_convocados");
        s.cerrada         = rs.getBoolean("cerrada");
        return s;
    }
 
 
    /*DTO para vistas: asistencia con datos del asambleista resueltos.
    La vista accede a los campos directamente.*/
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