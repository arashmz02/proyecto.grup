# Issue #8 - Reporte de Asistencia Unificado

## Objetivo

Implementar la base Java para generar reportes consolidados de asistencia de asambleístas.

## Archivos implementados

- `src/main/java/services/AsistenciaService.java`
- `src/main/java/models/ReporteAsistencia.java`

## Funcionalidad

El servicio permite calcular porcentajes de asistencia y generar textos resumen para certificaciones.

## Dependencia

Este módulo consumirá la vista SQL `v_asistencia`, creada en Azure SQL Server/T-SQL por el módulo de control de quórum.

## Validaciones

- El total de sesiones convocadas no puede ser negativo.
- El total de asistencias no puede superar el total de sesiones convocadas.
- Si no existen sesiones convocadas, se retorna un mensaje informativo.

## Stack

- Java
- MVC
- Maven
- Tomcat
- Compatible con Azure SQL Server / T-SQL
