package services;

public class AsistenciaService {

    public String generarResumenAsistencia(int totalConvocadas, int totalAsistidas, String tipoSesion) {
        if (totalConvocadas <= 0) {
            return "No existen sesiones convocadas para el periodo consultado.";
        }

        if (totalAsistidas < 0 || totalAsistidas > totalConvocadas) {
            throw new IllegalArgumentException("El total de asistencias no puede ser negativo ni mayor al total de sesiones convocadas.");
        }

        double porcentaje = (totalAsistidas * 100.0) / totalConvocadas;

        return String.format(
            "El asambleísta registra %d asistencias de %d sesiones convocadas de tipo %s, equivalente a %.2f%% de participación.",
            totalAsistidas,
            totalConvocadas,
            tipoSesion != null ? tipoSesion : "No especificado",
            porcentaje
        );
    }

    public double calcularPorcentajeAsistencia(int totalConvocadas, int totalAsistidas) {
        if (totalConvocadas <= 0) {
            return 0.0;
        }

        if (totalAsistidas < 0 || totalAsistidas > totalConvocadas) {
            throw new IllegalArgumentException("Datos de asistencia inválidos.");
        }

        return (totalAsistidas * 100.0) / totalConvocadas;
    }
}