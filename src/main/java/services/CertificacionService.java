package services;

import models.Certificacion;

import java.time.LocalDate;

public class CertificacionService {

    private final NotasService notasService;
    private final AsistenciaService asistenciaService;
    private final PDFService pdfService;

    public CertificacionService() {
        this.notasService = new NotasService();
        this.asistenciaService = new AsistenciaService();
        this.pdfService = new PDFService();
    }

    public String generarCertificacionDemo(
            int idAsambleista,
            String nombre,
            String cedula,
            int totalConvocadas,
            int totalAsistidas,
            String tipoSesion,
            String origenPropuesta,
            String etapaPropuesta
    ) {
        String resumenAsistencia = asistenciaService.generarResumenAsistencia(
                totalConvocadas,
                totalAsistidas,
                tipoSesion
        );

        String nota = notasService.obtenerNotaCondicional(
                origenPropuesta,
                etapaPropuesta
        );

        Certificacion certificacion = new Certificacion();
        certificacion.setIdAsambleista(idAsambleista);
        certificacion.setNombreAsambleista(nombre);
        certificacion.setCedula(cedula);
        certificacion.setResumenAsistencia(resumenAsistencia);
        certificacion.setNotaCondicional(nota);
        certificacion.setFolioUnico("PENDIENTE-FOLIO-BD");
        certificacion.setHashSeguridad("PENDIENTE-HASH-SHA256");
        certificacion.setFechaEmision(LocalDate.now().toString());

        return pdfService.generarContenidoCertificacion(certificacion);
    }
}