package controllers;

import models.Comision;

import java.util.ArrayList;
import java.util.List;

public class ComisionController {

    private final List<Comision> comisiones;

    public ComisionController() {
        this.comisiones = new ArrayList<>();
    }

    public void registrarComision(Comision comision) {
        if (comision == null) {
            throw new IllegalArgumentException("La comisión no puede ser nula.");
        }

        if (comision.getNombre() == null || comision.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la comisión es obligatorio.");
        }

        comisiones.add(comision);
    }

    public List<Comision> listarComisiones() {
        return comisiones;
    }

    public List<Comision> buscarPorNombre(String textoBusqueda) {
        List<Comision> resultado = new ArrayList<>();

        if (textoBusqueda == null || textoBusqueda.trim().isEmpty()) {
            return resultado;
        }

        String filtro = textoBusqueda.trim().toLowerCase();

        for (Comision comision : comisiones) {
            if (comision.getNombre() != null &&
                comision.getNombre().toLowerCase().contains(filtro)) {
                resultado.add(comision);
            }
        }

        return resultado;
    }
}