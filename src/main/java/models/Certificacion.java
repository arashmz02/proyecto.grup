package models;

public class Certificacion {

    private int idCertificacion;
    private int idAsambleista;
    private String nombreAsambleista;
    private String cedula;
    private String folioUnico;
    private String hashSeguridad;
    private String resumenAsistencia;
    private String notaCondicional;
    private String fechaEmision;

    // ====== Issue #15: Anulaciones y Sustituciones (Frank) ======
    private Integer idEstado;
    private String  nombreEstado;
    private String  motivoAnulacion;
    private String  fechaAnulacion;
    private Integer usuarioAnulacion;
    private Integer idCertificacionSustituye;

    public Certificacion() {
    }

    public int getIdCertificacion() {
        return idCertificacion;
    }

    public void setIdCertificacion(int idCertificacion) {
        this.idCertificacion = idCertificacion;
    }

    public int getIdAsambleista() {
        return idAsambleista;
    }

    public void setIdAsambleista(int idAsambleista) {
        this.idAsambleista = idAsambleista;
    }

    public String getNombreAsambleista() {
        return nombreAsambleista;
    }

    public void setNombreAsambleista(String nombreAsambleista) {
        this.nombreAsambleista = nombreAsambleista;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getFolioUnico() {
        return folioUnico;
    }

    public void setFolioUnico(String folioUnico) {
        this.folioUnico = folioUnico;
    }

    public String getHashSeguridad() {
        return hashSeguridad;
    }

    public void setHashSeguridad(String hashSeguridad) {
        this.hashSeguridad = hashSeguridad;
    }

    public String getResumenAsistencia() {
        return resumenAsistencia;
    }

    public void setResumenAsistencia(String resumenAsistencia) {
        this.resumenAsistencia = resumenAsistencia;
    }

    public String getNotaCondicional() {
        return notaCondicional;
    }

    public void setNotaCondicional(String notaCondicional) {
        this.notaCondicional = notaCondicional;
    }

    public String getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(String fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    // ====== Getters y setters del Issue #15 (Frank) ======
    public Integer getIdEstado() { return idEstado; }
    public void setIdEstado(Integer idEstado) { this.idEstado = idEstado; }

    public String getNombreEstado() { return nombreEstado; }
    public void setNombreEstado(String nombreEstado) { this.nombreEstado = nombreEstado; }

    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }

    public String getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(String fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }

    public Integer getUsuarioAnulacion() { return usuarioAnulacion; }
    public void setUsuarioAnulacion(Integer usuarioAnulacion) { this.usuarioAnulacion = usuarioAnulacion; }

    public Integer getIdCertificacionSustituye() { return idCertificacionSustituye; }
    public void setIdCertificacionSustituye(Integer idCertificacionSustituye) {
        this.idCertificacionSustituye = idCertificacionSustituye;
    }

    public boolean estaActiva()      { return idEstado != null && idEstado == 37; }
    public boolean estaAnulada()     { return idEstado != null && idEstado == 38; }
    public boolean estaSustituida()  { return idEstado != null && idEstado == 39; }
}
