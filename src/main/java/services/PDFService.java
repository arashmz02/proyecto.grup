package services;

public class PDFService {

    public String generarContenidoCertificacion(Certificacion certificacion) {
        if (certificacion == null) {
            throw new IllegalArgumentException("La certificación no puede ser nula.");
        }

        StringBuilder contenido = new StringBuilder();

        contenido.append("CERTIFICACIÓN AIR\n\n");
        contenido.append("La Secretaría de la Asamblea Institucional Representativa certifica que:\n\n");

        contenido.append("Nombre: ")
                .append(valorSeguro(certificacion.getNombreAsambleista()))
                .append("\n");

        contenido.append("Cédula: ")
                .append(valorSeguro(certificacion.getCedula()))
                .append("\n\n");

        contenido.append("Participación y asistencia:\n");
        contenido.append(valorSeguro(certificacion.getResumenAsistencia()))
                .append("\n\n");

        if (certificacion.getNotaCondicional() != null && !certificacion.getNotaCondicional().isBlank()) {
            contenido.append("Nota legal:\n");
            contenido.append(certificacion.getNotaCondicional()).append("\n\n");
        }

        contenido.append("Folio: ")
                .append(valorSeguro(certificacion.getFolioUnico()))
                .append("\n");

        contenido.append("Hash de seguridad: ")
                .append(valorSeguro(certificacion.getHashSeguridad()))
                .append("\n");

        contenido.append("Fecha de emisión: ")
                .append(valorSeguro(certificacion.getFechaEmision()))
                .append("\n\n");

        contenido.append("La presente certificación se emite para los fines administrativos correspondientes, ");
        contenido.append("con fundamento en los registros oficiales de la Secretaría de la AIR.");

        return contenido.toString();
    }

    private String valorSeguro(String valor) {
        return valor == null || valor.isBlank() ? "No disponible" : valor;
    }
}