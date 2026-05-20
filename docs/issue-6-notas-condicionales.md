# Issue #6 - Motor de Reglas para Notas Condicionales

## Objetivo

Centralizar la generación de notas legales condicionales para certificaciones.

## Archivos implementados

- src/main/java/services/NotasService.java
- src/main/java/controllers/ReporteController.java

## Funcionamiento

El servicio evalúa el origen y la etapa de una propuesta para determinar si debe agregarse una nota legal al cuerpo de la certificación.

## Reglas iniciales

| Condición | Nota generada |
|---|---|
| Consejo Institucional + Procedencia | Nota de procedencia por Consejo Institucional |
| 10% de Asamblea | Nota por propuesta presentada por porcentaje mínimo de asambleístas |
| Etapa de aprobación | Nota de etapa de aprobación |
