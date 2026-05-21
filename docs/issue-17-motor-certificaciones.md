# Issue #17 - Motor de Certificaciones

## Objetivo

Implementar la base del motor principal de certificaciones del sistema AIR.

## Archivos implementados

- `src/main/java/models/Certificacion.java`
- `src/main/java/services/PDFService.java`
- `src/main/java/services/CertificacionService.java`

## Funcionalidad

El módulo integra la información de asistencia, notas condicionales, folio, hash y datos del asambleísta para construir el contenido base de una certificación.

## Integraciones

- Issue #6: notas condicionales.
- Issue #8: resumen de asistencia.
- Issue #1: folio único institucional.
- Issue #13: hash SHA-256.

## Estado

Se deja preparada la estructura Java para la generación del PDF institucional y la integración posterior con iText y Azure SQL Server.

## Stack

- Java
- MVC
- Maven
- Tomcat
- Azure SQL Server / T-SQL
