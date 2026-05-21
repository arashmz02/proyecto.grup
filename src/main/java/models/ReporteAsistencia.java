package models;

public class ReporteAsistencia {

    private int idAsambleista;
    private String nombreAsambleista;
    private String tipoSesion;
    private int totalConvocadas;
    private int totalAsistidas;
    private double porcentaje;

    public ReporteAsistencia() {
    }

    public ReporteAsistencia(
            int idAsambleista,
            String nombreAsambleista,
            String tipoSesion,
            int totalConvocadas,
            int totalAsistidas,
            double porcentaje
    ) {
        this.idAsambleista = idAsambleista;
        this.nombreAsambleista = nombreAsambleista;
        this.tipoSesion = tipoSesion;
        this.totalConvocadas = totalConvocadas;
        this.totalAsistidas = totalAsistidas;
        this.porcentaje = porcentaje;
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

    public String getTipoSesion() {
        return tipoSesion;
    }

    public void setTipoSesion(String tipoSesion) {
        this.tipoSesion = tipoSesion;
    }

    public int getTotalConvocadas() {
        return totalConvocadas;
    }

    public void setTotalConvocadas(int totalConvocadas) {
        this.totalConvocadas = totalConvocadas;
    }

    public int getTotalAsistidas() {
        return totalAsistidas;
    }

    public void setTotalAsistidas(int totalAsistidas) {
        this.totalAsistidas = totalAsistidas;
    }

    public double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(double porcentaje) {
        this.porcentaje = porcentaje;
    }
}
