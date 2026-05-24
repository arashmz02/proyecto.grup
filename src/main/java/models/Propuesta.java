package models;

import config.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/*Modelo del modulo de Propuestas (Issue #10 Parte II)
Responsable: Josue 
NOTA: Cuando se cambia el estado de una propuesta, este modelo
inserta automaticamente un registro en bitacora_propuesta. Asi
el Controller no tiene que recordar registrarlo*/
public class Propuesta {

    private int idPropuesta;
    private String codigoAir;
    private String titulo;
    private String textoSustitutivo;
    private Integer idReglamentoBase;
    private Integer idPropuestaPadre;
    private int idEtapaPropuesta;
    private int idEstadoPropuesta;
    private int idTipoMayoriaRequerida;
    private String linkDocumentacion;

    //Getters 
    public int getIdPropuesta()              { return idPropuesta; }
    public String getCodigoAir()             { return codigoAir; }
    public String getTitulo()                { return titulo; }
    public String getTextoSustitutivo()      { return textoSustitutivo; }
    public Integer getIdReglamentoBase()     { return idReglamentoBase; }
    public Integer getIdPropuestaPadre()     { return idPropuestaPadre; }
    public int getIdEtapaPropuesta()         { return idEtapaPropuesta; }
    public int getIdEstadoPropuesta()        { return idEstadoPropuesta; }
    public int getIdTipoMayoriaRequerida()   { return idTipoMayoriaRequerida; }
    public String getLinkDocumentacion()     { return linkDocumentacion; }

    public boolean esConciliada()            { return idPropuestaPadre != null; }


    /*Crea una propuesta base (sin propuesta padre)*/
    public static int crear(String codigoAir, String titulo, String textoSustitutivo,
                            Integer idReglamentoBase, int idEtapaPropuesta,
                            int idEstadoPropuesta, int idTipoMayoriaRequerida,
                            String linkDocumentacion, int idUsuario) throws SQLException {

        return crearInterno(codigoAir, titulo, textoSustitutivo, idReglamentoBase,
                            null, idEtapaPropuesta, idEstadoPropuesta,
                            idTipoMayoriaRequerida, linkDocumentacion, idUsuario);
    }


    /*Crea una propuesta conciliada que hereda de una propuesta base*/
    public static int crearConciliada(String codigoAir, String titulo,
                                      String textoSustitutivo, Integer idReglamentoBase,
                                      int idPropuestaPadre, int idEtapaPropuesta,
                                      int idEstadoPropuesta, int idTipoMayoriaRequerida,
                                      String linkDocumentacion, int idUsuario)
            throws SQLException {

        return crearInterno(codigoAir, titulo, textoSustitutivo, idReglamentoBase,
                            idPropuestaPadre, idEtapaPropuesta, idEstadoPropuesta,
                            idTipoMayoriaRequerida, linkDocumentacion, idUsuario);
    }


    private static int crearInterno(String codigoAir, String titulo,
                                    String textoSustitutivo, Integer idReglamentoBase,
                                    Integer idPropuestaPadre, int idEtapaPropuesta,
                                    int idEstadoPropuesta, int idTipoMayoriaRequerida,
                                    String linkDocumentacion, int idUsuario)
            throws SQLException {

        String sql = "INSERT INTO propuesta " +
                     "(codigo_air, titulo, texto_sustitutivo, id_reglamento_base, " +
                     " id_propuesta_padre, id_etapa_propuesta, id_estado_propuesta, " +
                     " id_tipo_mayoria_requerida, link_documentacion) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = Conexion.obtener()) {
            Conexion.establecerContexto(con, idUsuario, null);

            int idNueva;

            try (PreparedStatement ps = con.prepareStatement(
                    sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, codigoAir);
                ps.setString(2, titulo);
                ps.setString(3, textoSustitutivo);

                if (idReglamentoBase != null) ps.setInt(4, idReglamentoBase);
                else ps.setNull(4, java.sql.Types.INTEGER);

                if (idPropuestaPadre != null) ps.setInt(5, idPropuestaPadre);
                else ps.setNull(5, java.sql.Types.INTEGER);

                ps.setInt(6, idEtapaPropuesta);
                ps.setInt(7, idEstadoPropuesta);
                ps.setInt(8, idTipoMayoriaRequerida);
                ps.setString(9, linkDocumentacion);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        idNueva = rs.getInt(1);
                    } else {
                        throw new SQLException("No se pudo obtener el id de la propuesta creada.");
                    }
                }
            }

            insertarBitacora(con, idNueva, idReglamentoBase, idEtapaPropuesta,
                             idEstadoPropuesta, titulo, codigoAir, idUsuario);

            return idNueva;
        }
    }


    public static Propuesta obtenerPorId(int id) throws SQLException {
        String sql = "SELECT id_propuesta, codigo_air, titulo, texto_sustitutivo, " +
                     "       id_reglamento_base, id_propuesta_padre, id_etapa_propuesta, " +
                     "       id_estado_propuesta, id_tipo_mayoria_requerida, link_documentacion " +
                     "FROM propuesta WHERE id_propuesta = ?";

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


    public static Propuesta obtenerPorCodigoAir(String codigoAir) throws SQLException {
        String sql = "SELECT id_propuesta, codigo_air, titulo, texto_sustitutivo, " +
                     "       id_reglamento_base, id_propuesta_padre, id_etapa_propuesta, " +
                     "       id_estado_propuesta, id_tipo_mayoria_requerida, link_documentacion " +
                     "FROM propuesta WHERE codigo_air = ?";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, codigoAir);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }


    public static List<Propuesta> listarTodas() throws SQLException {
        String sql = "SELECT id_propuesta, codigo_air, titulo, texto_sustitutivo, " +
                     "       id_reglamento_base, id_propuesta_padre, id_etapa_propuesta, " +
                     "       id_estado_propuesta, id_tipo_mayoria_requerida, link_documentacion " +
                     "FROM propuesta ORDER BY fecha_registro DESC";

        List<Propuesta> lista = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }


    public static List<Propuesta> listarPorEstado(int idEstadoPropuesta) throws SQLException {
        String sql = "SELECT id_propuesta, codigo_air, titulo, texto_sustitutivo, " +
                     "       id_reglamento_base, id_propuesta_padre, id_etapa_propuesta, " +
                     "       id_estado_propuesta, id_tipo_mayoria_requerida, link_documentacion " +
                     "FROM propuesta WHERE id_estado_propuesta = ? " +
                     "ORDER BY fecha_registro DESC";

        List<Propuesta> lista = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idEstadoPropuesta);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }


    public static List<Propuesta> obtenerConciliadas(int idPropuestaPadre) throws SQLException {
        String sql = "SELECT id_propuesta, codigo_air, titulo, texto_sustitutivo, " +
                     "       id_reglamento_base, id_propuesta_padre, id_etapa_propuesta, " +
                     "       id_estado_propuesta, id_tipo_mayoria_requerida, link_documentacion " +
                     "FROM propuesta WHERE id_propuesta_padre = ? " +
                     "ORDER BY fecha_registro DESC";

        List<Propuesta> lista = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPropuestaPadre);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }


    /*Cambia el estado de una propuesta y registra el cambio en bitacora_propuesta para garantizar trazabilidad.*/
    public static void cambiarEstado(int idPropuesta, int nuevoEstado, int idUsuario)
            throws SQLException {

        try (Connection con = Conexion.obtener()) {
            Conexion.establecerContexto(con, idUsuario, null);

            String sqlSelect = "SELECT id_reglamento_base, id_etapa_propuesta, " +
                               "       titulo, codigo_air " +
                               "FROM propuesta WHERE id_propuesta = ?";

            Integer idReglamentoBase;
            int idEtapaPropuesta;
            String titulo;
            String codigoAir;

            try (PreparedStatement ps = con.prepareStatement(sqlSelect)) {
                ps.setInt(1, idPropuesta);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Propuesta no encontrada: " + idPropuesta);
                    }
                    int rb = rs.getInt("id_reglamento_base");
                    idReglamentoBase = rs.wasNull() ? null : rb;
                    idEtapaPropuesta = rs.getInt("id_etapa_propuesta");
                    titulo           = rs.getString("titulo");
                    codigoAir        = rs.getString("codigo_air");
                }
            }

            String sqlUpdate = "UPDATE propuesta SET id_estado_propuesta = ? " +
                               "WHERE id_propuesta = ?";

            try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                ps.setInt(1, nuevoEstado);
                ps.setInt(2, idPropuesta);
                ps.executeUpdate();
            }

            insertarBitacora(con, idPropuesta, idReglamentoBase, idEtapaPropuesta,
                             nuevoEstado, titulo, codigoAir, idUsuario);
        }
    }


    private static void insertarBitacora(Connection con, int idPropuesta,
                                         Integer idReglamentoBase, int idEtapaPropuesta,
                                         int idEstadoPropuesta, String titulo,
                                         String codigoAir, int idUsuario)
            throws SQLException {

        String sql = "INSERT INTO bitacora_propuesta " +
                     "(id_propuesta, id_reglamento_base, id_etapa_propuesta, " +
                     " id_estado_propuesta, titulo, codigo_air, usuario_modificacion) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPropuesta);

            if (idReglamentoBase != null) ps.setInt(2, idReglamentoBase);
            else ps.setNull(2, java.sql.Types.INTEGER);

            ps.setInt(3, idEtapaPropuesta);
            ps.setInt(4, idEstadoPropuesta);
            ps.setString(5, titulo);
            ps.setString(6, codigoAir);
            ps.setInt(7, idUsuario);
            ps.executeUpdate();
        }
    }


    public static void agregarProponente(int idPropuesta, int idAsambleista)
            throws SQLException {

        String sql = "INSERT INTO proponente_propuesta (id_propuesta, id_asambleista) " +
                     "VALUES (?, ?)";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPropuesta);
            ps.setInt(2, idAsambleista);
            ps.executeUpdate();
        }
    }


    public static List<ProponenteVista> obtenerProponentes(int idPropuesta)
            throws SQLException {

        String sql = "SELECT pp.id_proponente_propuesta, pp.id_asambleista, " +
                     "       a.nombre, a.cedula " +
                     "FROM proponente_propuesta pp " +
                     "JOIN asambleista a ON pp.id_asambleista = a.id_asambleista " +
                     "WHERE pp.id_propuesta = ? " +
                     "ORDER BY a.nombre";

        List<ProponenteVista> lista = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPropuesta);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProponenteVista pv = new ProponenteVista();
                    pv.idProponentePropuesta = rs.getInt("id_proponente_propuesta");
                    pv.idAsambleista         = rs.getInt("id_asambleista");
                    pv.nombre                = rs.getString("nombre");
                    pv.cedula                = rs.getString("cedula");
                    lista.add(pv);
                }
            }
        }
        return lista;
    }


    public static List<BitacoraVista> obtenerHistorial(int idPropuesta) throws SQLException {
        String sql = "SELECT b.id_registro_bitacora, b.fecha_modificacion, " +
                     "       ce.nombre AS estado, cp.nombre AS etapa, " +
                     "       u.username " +
                     "FROM bitacora_propuesta b " +
                     "JOIN catalogo_maestro ce ON b.id_estado_propuesta = ce.id_item " +
                     "JOIN catalogo_maestro cp ON b.id_etapa_propuesta = cp.id_item " +
                     "LEFT JOIN sys_usuario u ON b.usuario_modificacion = u.id_usuario " +
                     "WHERE b.id_propuesta = ? " +
                     "ORDER BY b.fecha_modificacion DESC";

        List<BitacoraVista> lista = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idPropuesta);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BitacoraVista bv = new BitacoraVista();
                    bv.idRegistro       = rs.getInt("id_registro_bitacora");
                    bv.fechaModificacion = rs.getTimestamp("fecha_modificacion").toLocalDateTime();
                    bv.estado            = rs.getString("estado");
                    bv.etapa             = rs.getString("etapa");
                    bv.usuario           = rs.getString("username");
                    lista.add(bv);
                }
            }
        }
        return lista;
    }


    private static Propuesta mapear(ResultSet rs) throws SQLException {
        Propuesta p = new Propuesta();
        p.idPropuesta            = rs.getInt("id_propuesta");
        p.codigoAir              = rs.getString("codigo_air");
        p.titulo                 = rs.getString("titulo");
        p.textoSustitutivo       = rs.getString("texto_sustitutivo");

        int rb = rs.getInt("id_reglamento_base");
        p.idReglamentoBase = rs.wasNull() ? null : rb;

        int pp = rs.getInt("id_propuesta_padre");
        p.idPropuestaPadre = rs.wasNull() ? null : pp;

        p.idEtapaPropuesta       = rs.getInt("id_etapa_propuesta");
        p.idEstadoPropuesta      = rs.getInt("id_estado_propuesta");
        p.idTipoMayoriaRequerida = rs.getInt("id_tipo_mayoria_requerida");
        p.linkDocumentacion      = rs.getString("link_documentacion");
        return p;
    }


    /*DTO para vistas: proponente con datos del asambleista. */
    public static class ProponenteVista {
        public int idProponentePropuesta;
        public int idAsambleista;
        public String nombre;
        public String cedula;

        public int getIdProponentePropuesta() { return idProponentePropuesta; }
        public int getIdAsambleista()         { return idAsambleista; }
        public String getNombre()             { return nombre; }
        public String getCedula()             { return cedula; }
    }


    /*DTO para vistas: registro de bitacora con datos resueltos. */
    public static class BitacoraVista {
        public int idRegistro;
        public java.time.LocalDateTime fechaModificacion;
        public String estado;
        public String etapa;
        public String usuario;

        public int getIdRegistro()                             { return idRegistro; }
        public java.time.LocalDateTime getFechaModificacion()  { return fechaModificacion; }
        public String getEstado()                              { return estado; }
        public String getEtapa()                               { return etapa; }
        public String getUsuario()                             { return usuario; }
    }
}