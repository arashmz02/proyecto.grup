package controllers;

import services.NotasService;

public class ReporteController {

    private final NotasService notasService;

    public ReporteController() {
        this.notasService = new NotasService();
    }

    public String generarNotaParaCertificacion(String origenPropuesta, String etapaPropuesta) {
        return notasService.obtenerNotaCondicional(origenPropuesta, etapaPropuesta);
    }
}