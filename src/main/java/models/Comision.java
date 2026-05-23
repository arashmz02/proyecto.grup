package models;

public class Comision {

    private int idComision;
    private String nombre;
    private String descripcion;
    private boolean activa;

    public Comision() {
    }

    public Comision(int idComision, String nombre, String descripcion, boolean activa) {
        this.idComision = idComision;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activa = activa;
    }

    public int getIdComision() {
        return idComision;
    }

    public void setIdComision(int idComision) {
        this.idComision = idComision;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}