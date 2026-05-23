package models;

import config.Conexion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*Modelo del modulo de Resoluciones (Issue #10 Parte II).
Responsable: Josue 

NOTA IMPORTANTE: el metodo aplicarReforma() inserta en elemento_normativo una nueva version 'Vigente'.
Esto DISPARA el trigger tg_vigencia_normativa del Sprint 2 que automaticamente
marca la version anterior como 'Historico'. Es el punto donde el Issue #10 Parte II se conecta con la infraestructura del
Sprint 2 (versionamiento automatico de reglamentos)*/
public class Resolucion {

    private int idResolucion;
    private int idPuntoAgenda;
    private String numeroResolucion;
    private LocalDateTime fechaEmision;

    // ----- Getters -----
    public int getIdResolucion()             { return idResolucion; }
    public int getIdPuntoAgenda()            { return idPuntoAgenda; }
    public String getNumeroResolucion()      { return numeroResolucion; }
    public LocalDateTime getFechaEmision()   { return fechaEmision; }


    /*Crea un punto de agenda: vincula una propuesta con una sesion en un orden especifico. 
    @return id del punto de agenda creado*/
    public static int crearPuntoAgenda(int idSesion, int idPropuesta,
                                       int orden, String descripcion) throws SQLException {

        String sql = "INSERT INTO punto_agenda " +
                     "(id_sesion, id_propuesta, orden, descripcion) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(
                     sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, idSesion);
            ps.setInt(2, idPropuesta);
            ps.setInt(3, orden);
            ps.setString(4, descripcion);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            throw new SQLException("No se pudo obtener el id del punto de agenda creado.");
        }
    }


    /*Lista los puntos de agenda de una sesion, ordenados*/
    public static List<PuntoAgendaVista> listarPuntosDeSesion(int idSesion)
            throws SQLException {

        String sql = "SELECT pa.id_punto_agenda, pa.id_propuesta, pa.orden, " +
                     "       pa.descripcion, p.codigo_air, p.titulo " +
                     "FROM punto_agenda pa " +
                     "JOIN propuesta p ON pa.id_propuesta = p.id_propuesta " +
                     "WHERE pa.id_sesion = ? " +
                     "ORDER BY pa.orden";

        List<PuntoAgendaVista> lista = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idSesion);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PuntoAgendaVista pv = new PuntoAgendaVista();
                    pv.idPuntoAgenda = rs.getInt("id_punto_agenda");
                    pv.idPropuesta   = rs.getInt("id_propuesta");
                    pv.orden         = rs.getInt("orden");
                    pv.descripcion   = rs.getString("descripcion");
                    pv.codigoAir     = rs.getString("codigo_air");
                    pv.tituloPropuesta = rs.getString("titulo");
                    lista.add(pv);
                }
            }
        }
        return lista;
    }


    /*Emite una resolucion oficial sobre un punto de agenda.
    @param numeroResolucion codigo oficial (ej. 'AIR-RES-001-2024')
    @return id de la resolucion creada*/
    public static int emitirResolucion(int idPuntoAgenda, String numeroResolucion,
                                       int idUsuario) throws SQLException {

        String sql = "INSERT INTO resolucion (id_punto_agenda, numero_resolucion) " +
                     "VALUES (?, ?)";

        try (Connection con = Conexion.obtener()) {
            Conexion.establecerContexto(con, idUsuario, null);

            try (PreparedStatement ps = con.prepareStatement(
                    sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, idPuntoAgenda);
                ps.setString(2, numeroResolucion);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
                throw new SQLException("No se pudo obtener el id de la resolucion emitida.");
            }
        }
    }


    /*Busca una resolucion por su id.*/
    public static Resolucion obtenerPorId(int id) throws SQLException {
        String sql = "SELECT id_resolucion, id_punto_agenda, numero_resolucion, fecha_emision " +
                     "FROM resolucion WHERE id_resolucion = ?";

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


    /*Busca una resolucion por su numero oficial (ej. 'AIR-RES-001-2024')*/
    public static Resolucion obtenerPorNumero(String numeroResolucion) throws SQLException {
        String sql = "SELECT id_resolucion, id_punto_agenda, numero_resolucion, fecha_emision " +
                     "FROM resolucion WHERE numero_resolucion = ?";

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, numeroResolucion);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }


    /*Lista todas las resoluciones emitidas, de la mas reciente a la mas antigua.*/
    public static List<Resolucion> listarTodas() throws SQLException {
        String sql = "SELECT id_resolucion, id_punto_agenda, numero_resolucion, fecha_emision " +
                     "FROM resolucion ORDER BY fecha_emision DESC";

        List<Resolucion> lista = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }


    /*Aplica una reforma normativa derivada de una resolucion.
    El trigger tg_vigencia_normativa del Sprint 2 se encarga automaticamente de archivar la version anterior como
    'Historico'.Aqui solo se registra el rastro en reforma_aplicada*/
    public static int aplicarReforma(int idResolucion, int idElementoNormativo,
                                     int idTipoReforma, String textoAnterior,
                                     String textoNuevo, LocalDate fechaInicioVigencia,
                                     int idUsuario) throws SQLException {

        String sql = "INSERT INTO reforma_aplicada " +
                     "(id_resolucion, id_elemento_normativo, id_tipo_reforma, " +
                     " texto_anterior, texto_nuevo, fecha_inicio_vigencia) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = Conexion.obtener()) {
            Conexion.establecerContexto(con, idUsuario, null);

            try (PreparedStatement ps = con.prepareStatement(
                    sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, idResolucion);
                ps.setInt(2, idElementoNormativo);
                ps.setInt(3, idTipoReforma);
                ps.setString(4, textoAnterior);
                ps.setString(5, textoNuevo);

                if (fechaInicioVigencia != null) {
                    ps.setDate(6, Date.valueOf(fechaInicioVigencia));
                } else {
                    ps.setDate(6, Date.valueOf(LocalDate.now()));
                }

                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
                throw new SQLException("No se pudo obtener el id de la reforma aplicada.");
            }
        }
    }


    /*Devuelve todas las reformas aplicadas por una resolucion. Una
    sola resolucion puede modificar varios elementos normativos a la vez*/
    public static List<ReformaVista> obtenerReformasDeResolucion(int idResolucion)
            throws SQLException {

        String sql = "SELECT r.id_reforma, r.id_elemento_normativo, " +
                     "       en.numero_etiqueta, en.contenido_texto, " +
                     "       ct.nombre AS tipo_reforma, " +
                     "       r.texto_anterior, r.texto_nuevo, r.fecha_inicio_vigencia " +
                     "FROM reforma_aplicada r " +
                     "JOIN elemento_normativo en ON r.id_elemento_normativo = en.id_elemento " +
                     "JOIN catalogo_maestro ct ON r.id_tipo_reforma = ct.id_item " +
                     "WHERE r.id_resolucion = ? " +
                     "ORDER BY r.fecha_inicio_vigencia DESC";

        List<ReformaVista> lista = new ArrayList<>();

        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idResolucion);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReformaVista rv = new ReformaVista();
                    rv.idReforma            = rs.getInt("id_reforma");
                    rv.idElementoNormativo  = rs.getInt("id_elemento_normativo");
                    rv.numeroEtiqueta       = rs.getString("numero_etiqueta");
                    rv.contenidoTexto       = rs.getString("contenido_texto");
                    rv.tipoReforma          = rs.getString("tipo_reforma");
                    rv.textoAnterior        = rs.getString("texto_anterior");
                    rv.textoNuevo           = rs.getString("texto_nuevo");
                    rv.fechaInicioVigencia  = rs.getDate("fecha_inicio_vigencia").toLocalDate();
                    lista.add(rv);
                }
            }
        }
        return lista;
    }


    private static Resolucion mapear(ResultSet rs) throws SQLException {
        Resolucion r = new Resolucion();
        r.idResolucion     = rs.getInt("id_resolucion");
        r.idPuntoAgenda    = rs.getInt("id_punto_agenda");
        r.numeroResolucion = rs.getString("numero_resolucion");
        r.fechaEmision     = rs.getTimestamp("fecha_emision").toLocalDateTime();
        return r;
    }


    /*DTO para vistas: punto de agenda con datos de propuesta. */
    public static class PuntoAgendaVista {
        public int idPuntoAgenda;
        public int idPropuesta;
        public int orden;
        public String descripcion;
        public String codigoAir;
        public String tituloPropuesta;

        public int getIdPuntoAgenda()       { return idPuntoAgenda; }
        public int getIdPropuesta()         { return idPropuesta; }
        public int getOrden()               { return orden; }
        public String getDescripcion()      { return descripcion; }
        public String getCodigoAir()        { return codigoAir; }
        public String getTituloPropuesta()  { return tituloPropuesta; }
    }


    /*DTO para vistas: reforma aplicada con datos resueltos. */
    public static class ReformaVista {
        public int idReforma;
        public int idElementoNormativo;
        public String numeroEtiqueta;
        public String contenidoTexto;
        public String tipoReforma;
        public String textoAnterior;
        public String textoNuevo;
        public LocalDate fechaInicioVigencia;

        public int getIdReforma()                  { return idReforma; }
        public int getIdElementoNormativo()        { return idElementoNormativo; }
        public String getNumeroEtiqueta()          { return numeroEtiqueta; }
        public String getContenidoTexto()          { return contenidoTexto; }
        public String getTipoReforma()             { return tipoReforma; }
        public String getTextoAnterior()           { return textoAnterior; }
        public String getTextoNuevo()              { return textoNuevo; }
        public LocalDate getFechaInicioVigencia()  { return fechaInicioVigencia; }
    }
}