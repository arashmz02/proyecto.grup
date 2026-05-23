package services;

public class NotasService {

    public String obtenerNotaCondicional(String origenPropuesta, String etapaPropuesta) {

        if (origenPropuesta == null || etapaPropuesta == null) {
            return "";
        }

        String origen = origenPropuesta.trim().toUpperCase();
        String etapa = etapaPropuesta.trim().toUpperCase();

        if (origen.contains("CONSEJO") && etapa.contains("PROCEDENCIA")) {
            return "Nota: La propuesta se encuentra en etapa de procedencia por remisión del Consejo Institucional.";
        }

        if (origen.contains("10") || origen.contains("ASAMBLEA")) {
            return "Nota: La propuesta fue presentada por al menos el diez por ciento de los miembros de la Asamblea Institucional Representativa.";
        }

        if (etapa.contains("APROBACION") || etapa.contains("APROBACIÓN")) {
            return "Nota: La propuesta se encuentra en etapa de aprobación por parte de la Asamblea Institucional Representativa.";
        }

        return "";
    }
}
