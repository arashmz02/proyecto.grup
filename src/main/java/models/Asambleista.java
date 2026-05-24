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
 
/*Modelo de la entidad asambleista (Issue #9).
 Encapsula el acceso a las tablas:
 - asambleista
 - nombramiento
 - bitacora_asambleistas (escrita por el trigger tg_cambio_identidad)
 IMPORTANTE - triggers de la BD:
 - tg_traslape_sector: rechaza nombramientos con fechas solapadas (lanza error con numero 50001). El controlador captura este
 caso y muestra un mensaje al usuario.
 - tg_auditoria_asambleista / tg_auditoria_nombramiento: registran
 INSERT/UPDATE/DELETE en sys_log_auditoria. Necesitan que la conexion tenga el contexto de sesion con el id del usuario.
 Por eso TODOS los metodos que escriben llaman a Conexion.establecerContexto() antes del INSERT/UPDATE.
 Es responsabilidad del controlador pasar el idUsuario actual al llamar a estos metodos.*/
public class Asambleista {
 
    private int idAsambleista;
    private String cedula;
    private String nombre;
    private String correoInstitucional;
 
    // Getters 
    public int getIdAsambleista()           { return idAsambleista; }
    public String getCedula()               { return cedula; }
    public String getNombre()               { return nombre; }
    public String getCorreoInstitucional()  { return correoInstitucional; }
 
 
    /*Crea un nuevo asambleista.*/
    public static int crear(String cedula, String nombre, String correo,
                            int idUsuarioRegistro) throws SQLException {
 
        String sql = "INSERT INTO asambleista (cedula, nombre, correo_institucional) " +
                     "VALUES (?, ?, ?)";
 
        try (Connection con = Conexion.obtener()) {
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
 
 
    /*Busca un asambleista por su id.*/
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
 
 
    /*Busca un asambleista por su cedula.*/
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
 
 
    /*Lista todos los asambleistas ordenados por nombre.*/
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
 
 
    /*Busca asambleistas por coincidencia parcial en nombre o cedula.
     Util para autocompletado en formularios (Issue #16 Dashboard).
     Devuelve un maximo de 10 resultados para evitar respuestas pesadas. 
     La busqueda es case-insensitive y matchea cualquier parte de los campos (LIKE %texto%).
     * @param texto texto parcial a buscar (cedula o nombre)
     * @return lista de asambleistas que matchean, max 10*/
    public static List<Asambleista> buscarPorTexto(String texto) throws SQLException {
        List<Asambleista> resultados = new ArrayList<>();
 
        // Si el texto es vacio o muy corto, no devolvemos nada
        if (texto == null || texto.trim().length() < 2) {
            return resultados;
        }
 
        // TOP 10 limita los resultados; el % al inicio y al final
        // hace match parcial en cualquier parte del campo.
        String sql =
            "SELECT TOP 10 id_asambleista, cedula, nombre, correo_institucional " +
            "FROM asambleista " +
            "WHERE LOWER(nombre) LIKE LOWER(?) OR cedula LIKE ? " +
            "ORDER BY nombre";
 
        String patron = "%" + texto.trim() + "%";
 
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
 
            ps.setString(1, patron);
            ps.setString(2, patron);
 
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultados.add(mapear(rs));
                }
            }
        }
        return resultados;
    }
 
 
    /*Indica si el asambleista tiene al menos un nombramiento vigente a la fecha actual.*/
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
                return rs.next();
            }
        }
    }
 
 
    /*Registra un nuevo nombramiento para un asambleista.*/
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
 
 
    /*Devuelve el historial completo de nombramientos de un asambleista, ordenado del mas reciente al mas antiguo.*/
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
 
 
    /*Helper privado: convierte una fila del ResultSet en un objeto Asambleista.*/
    private static Asambleista mapear(ResultSet rs) throws SQLException {
        Asambleista a = new Asambleista();
        a.idAsambleista       = rs.getInt("id_asambleista");
        a.cedula              = rs.getString("cedula");
        a.nombre              = rs.getString("nombre");
        a.correoInstitucional = rs.getString("correo_institucional");
        return a;
    }
 
 
    /*  DTO para vistas: nombramientos con datos resueltos. */
    public static class NombramientoVista {
        public int idNombramiento;
        public LocalDate fechaInicio;
        public LocalDate fechaFin;
        public String estado;
        public String sector;
        public String puesto;
 
        public String getFechaInicioFormateada() {
            return fechaInicio.toString();
        }
 
        public String getFechaFinFormateada() {
            return fechaFin == null ? "Vigente" : fechaFin.toString();
        }
    }
}