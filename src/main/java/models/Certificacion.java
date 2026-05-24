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
}