package services;

import config.Conexion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExportService {

    public byte[] exportarCertificaciones(String fechaDesde, String fechaHasta)
            throws SQLException, IOException {

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet hoja = wb.createSheet("Certificaciones");

            CellStyle estiloH = wb.createCellStyle();
            Font fuenteH = wb.createFont();
            fuenteH.setBold(true);
            fuenteH.setColor(IndexedColors.WHITE.getIndex());
            estiloH.setFont(fuenteH);
            estiloH.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            estiloH.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row titulo = hoja.createRow(0);
            Cell celdaTitulo = titulo.createCell(0);
            celdaTitulo.setCellValue("AIR - Reporte de Certificaciones Emitidas");
            hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            String[] columnas = {
                "Folio", "Nombre del funcionario", "Cedula",
                "Fecha de emision", "Emitido por", "Hash"
            };
            Row encabezado = hoja.createRow(2);
            for (int i = 0; i < columnas.length; i++) {
                Cell c = encabezado.createCell(i);
                c.setCellValue(columnas[i]);
                c.setCellStyle(estiloH);
            }

            List<Object[]> datos = consultarCertificaciones(fechaDesde, fechaHasta);
            int fila = 3;
            for (Object[] row : datos) {
                Row r = hoja.createRow(fila++);
                for (int i = 0; i < row.length; i++) {
                    r.createCell(i).setCellValue(
                        row[i] != null ? row[i].toString() : "");
                }
            }

            for (int i = 0; i < columnas.length; i++) {
                hoja.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportarAportesPorAsambleista(int asambleistaId)
            throws SQLException, IOException {

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet hoja = wb.createSheet("Aportes");

            CellStyle estiloH = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            estiloH.setFont(font);
            estiloH.setFillForegroundColor(
                IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            estiloH.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] columnas = {
                "Codigo Propuesta", "Fecha", "Titulo de la propuesta",
                "Tipo participacion", "Estado"
            };
            Row encabezado = hoja.createRow(0);
            for (int i = 0; i < columnas.length; i++) {
                Cell c = encabezado.createCell(i);
                c.setCellValue(columnas[i]);
                c.setCellStyle(estiloH);
            }

            List<Object[]> datos = consultarAportes(asambleistaId);
            int fila = 1;
            for (Object[] row : datos) {
                Row r = hoja.createRow(fila++);
                for (int i = 0; i < row.length; i++) {
                    r.createCell(i).setCellValue(
                        row[i] != null ? row[i].toString() : "");
                }
            }

            for (int i = 0; i < columnas.length; i++) {
                hoja.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private List<Object[]> consultarCertificaciones(String desde, String hasta)
            throws SQLException {
        List<Object[]> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT ce.folio_unico, a.nombre, a.cedula, " +
            "       CONVERT(VARCHAR, ce.fecha_emision, 103), " +
            "       ISNULL(u.username, 'N/D'), " +
            "       ISNULL(ce.hash_seguridad, '') " +
            "FROM certificacion_emitida ce " +
            "JOIN asambleista a ON a.id_asambleista = ce.id_asambleista " +
            "LEFT JOIN sys_usuario u ON u.id_usuario = ce.usuario_secretaria " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();
        if (desde != null && !desde.isEmpty()) {
            sql.append("AND ce.fecha_emision >= ? ");
            params.add(desde);
        }
        if (hasta != null && !hasta.isEmpty()) {
            sql.append("AND ce.fecha_emision <= ? ");
            params.add(hasta + " 23:59:59");
        }
        sql.append("ORDER BY ce.fecha_emision DESC");

        try (Connection conn = Conexion.obtener();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new Object[]{
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getString(6)
                });
            }
        }
        return lista;
    }

    private List<Object[]> consultarAportes(int id) throws SQLException {
        List<Object[]> lista = new ArrayList<>();
        String sql =
            "SELECT p.codigo_air, " +
            "       CONVERT(VARCHAR, s.fecha, 103) AS fecha_sesion, " +
            "       p.titulo, " +
            "       'Proponente' AS tipo_participacion, " +
            "       ISNULL(cm.nombre, 'Sin estado') AS estado " +
            "FROM propuesta p " +
            "JOIN proponente_propuesta pp " +
            "    ON pp.id_propuesta = p.id_propuesta " +
            "    AND pp.id_asambleista = ? " +
            "LEFT JOIN punto_agenda pa " +
            "    ON pa.id_propuesta = p.id_propuesta " +
            "LEFT JOIN sesiones s ON s.id_sesion = pa.id_sesion " +
            "LEFT JOIN catalogo_maestro cm ON cm.id_item = p.id_estado_propuesta " +
            "ORDER BY s.fecha DESC";

        try (Connection conn = Conexion.obtener();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new Object[]{
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5)
                });
            }
        }
        return lista;
    }
}