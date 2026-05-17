Diccionario General de datos
## Tabla: elemento_normativo
| Campo | Tipo | Descripción |
|---|---|---|
| id_elemento | INT IDENTITY | ID único del elemento normativo |
| id_reglamento | INT | Reglamento al que pertenece |
| id_elemento_padre | INT | Elemento padre dentro de la jerarquía |
| id_nivel_reglamento | INT | Nivel normativo del elemento |
| numero_etiqueta | NVARCHAR(20) | Etiqueta del elemento (ej: 18, a), i. |
| contenido_texto | NVARCHAR(MAX) | Contenido textual del elemento |
| orden | INT | Orden de aparición |
| fecha_inicio_vigencia | DATE | Fecha de inicio de vigencia |
| fecha_fin_vigencia | DATE | Fecha de finalización de vigencia |
| id_estado_vigencia | INT | Estado actual de vigencia |
| id_acuerdo_origen | INT | Acuerdo o resolución de origen |

## Trigger: tg_vigencia_normativa
 
Trigger encargado de gestionar automáticamente la vigencia histórica de los elementos normativos.
 
## Funcionalidad
- Detecta cuando se inserta una nueva versión vigente de un elemento normativo.
- Marca automáticamente como histórico el registro vigente anterior.
- Actualiza la fecha de finalización de vigencia.
- Inserta la nueva versión vigente evitando conflictos de unicidad.

## Tablas relacionadas
- elemento_normativo
- catalogo_estado_vigencia
- reglamento
- catalogo_nivel_reglamento
 
## Estados utilizados
- Vigente
- Histórico

## Tabla: control_folio
 
Tabla utilizada para llevar el control de los consecutivos institucionales.
 
| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_control | INT | PK, IDENTITY | Identificador del control de folio |
| anio | INT | NOT NULL | Año del consecutivo |
| prefijo | NVARCHAR(10) | NOT NULL, DEFAULT 'DAIR' | Prefijo utilizado en el folio |
| ultimo_numero | INT | NOT NULL, DEFAULT 0 | Último número generado |
| fecha_actualizacion | DATETIME2 | NOT NULL, DEFAULT SYSUTCDATETIME() | Fecha de actualización |
| uq_control_anio_prefijo | CONSTRAINT | UNIQUE | Evita duplicar control por año y prefijo |
 
## Tabla: certificacion_emitida
 
Tabla donde se guardan las certificaciones emitidas con su folio único.
 
| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_certificacion | INT | PK, IDENTITY | Identificador de la certificación |
| id_asambleista | INT | FK | Asambleísta relacionado |
| folio_unico | NVARCHAR(30) | NOT NULL, UNIQUE | Folio único de la certificación |
| hash_seguridad | NVARCHAR(80) | NULL | Hash o código de seguridad |
| fecha_emision | DATETIME2 | NOT NULL, DEFAULT SYSUTCDATETIME() | Fecha de emisión |
| usuario_secretaria | INT | FK | Usuario que emite la certificación |
| fk_certificacion_asambleista | CONSTRAINT | FOREIGN KEY | Relación con asambleista |
| fk_certificacion_usuario | CONSTRAINT | FOREIGN KEY | Relación con sys_usuario |
 
## Tabla: sys_log_auditoria
 
Tabla de auditoría general del sistema.
 
| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_log | INT | PK | Identificador del registro de auditoría |
| id_usuario | INT | FK | Usuario que realizó la acción |
| accion | VARCHAR/NVARCHAR | NOT NULL | Acción realizada |
| tabla_afectada | VARCHAR/NVARCHAR | NOT NULL | Tabla afectada |
| registro_id | INT | NULL | Registro modificado |
| detalle | TEXT/NVARCHAR | NULL | Detalle del cambio |
| fecha_hora | DATETIME/DATETIME2 | DEFAULT | Fecha y hora del evento |
 
## Tabla: catalogo_maestro
 
Tabla para manejar catálogos reutilizables del sistema, como sectores, puestos o estados.
 
| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_catalogo | INT | PK | Identificador del catálogo |
| tipo_catalogo | VARCHAR/NVARCHAR | NOT NULL | Tipo de catálogo |
| codigo | VARCHAR/NVARCHAR | NULL | Código interno |
| nombre | VARCHAR/NVARCHAR | NOT NULL | Nombre del valor |
| descripcion | VARCHAR/NVARCHAR | NULL | Descripción |
| activo | BIT | DEFAULT | Indica si está activo |
 
## Tabla: bitacora_cedula
 
Tabla utilizada para registrar cambios históricos relacionados con números de cédula dentro del sistema.
 
| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_bitacora | INT | PK, IDENTITY | Identificador del registro |
| tabla_origen | NVARCHAR(100) | NOT NULL | Tabla donde ocurrió el cambio |
| id_registro | INT | NOT NULL | Registro afectado |
| cedula_anterior | NVARCHAR(30) | NULL | Valor anterior de la cédula |
| cedula_nueva | NVARCHAR(30) | NOT NULL | Nuevo valor de la cédula |
| usuario_modificacion | INT | FK | Usuario que realizó el cambio |
| fecha_modificacion | DATETIME2 | DEFAULT SYSUTCDATETIME() | Fecha del cambio |
| observacion | NVARCHAR(255) | NULL | Motivo o comentario del cambio |
 
## Trigger: trg_bitacora_cedula
 
Trigger encargado de registrar automáticamente cualquier modificación realizada sobre números de cédula en entidades sensibles del sistema.
 
# Diccionario de Datos - Módulo de Jerarquía Normativa (Issue #10)
## Tabla: catalogo_nivel_reglamento

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_nivel_reglamento | INT IDENTITY(1,1) | ID único del nivel (PK) |
| nombre | NVARCHAR(40) NOT NULL UNIQUE | Nombre del nivel: Título, Capítulo, Artículo, Inciso, Sub-inciso |
| orden | INT NOT NULL | Orden jerárquico del nivel |

---

## Tabla: catalogo_estado_vigencia

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_estado_vigencia | INT IDENTITY(1,1) | ID único del estado (PK) |
| nombre | NVARCHAR(20) NOT NULL UNIQUE | Estado: Vigente, Histórico, Derogado |

---

## Tabla: reglamento

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_reglamento | INT IDENTITY(1,1) | ID único del reglamento (PK) |
| nombre_normativa | NVARCHAR(150) NOT NULL | Nombre completo del reglamento |
| sigla | NVARCHAR(20) NOT NULL UNIQUE | Acrónimo único del reglamento |
| emisor | NVARCHAR(10) | Órgano emisor: AIR o CI |

---

## Tabla: elemento_normativo

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_elemento | INT IDENTITY(1,1) | ID único del elemento (PK) |
| id_reglamento | INT NOT NULL | FK a reglamento.id_reglamento |
| id_elemento_padre | INT | FK a elemento_normativo.id_elemento (NULL si es raíz) |
| id_nivel_reglamento | INT NOT NULL | FK a catalogo_nivel_reglamento.id_nivel_reglamento |
| numero_etiqueta | NVARCHAR(20) NOT NULL | Etiqueta visible: "18", "a)", "I.1.a" |
| contenido_texto | NVARCHAR(MAX) NOT NULL | Texto completo del elemento |
| orden | INT NOT NULL | Orden entre hermanos |
| fecha_inicio_vigencia | DATE NOT NULL | Fecha de inicio de vigencia |
| fecha_fin_vigencia | DATE | Fecha de fin de vigencia |
| id_estado_vigencia | INT NOT NULL | FK a catalogo_estado_vigencia.id_estado_vigencia |
| id_acuerdo_origen | INT | FK opcional a futura tabla resolucion |


## Trigger: tg_vigencia_normativa
 
Trigger encargado de gestionar automáticamente el versionamiento histórico de los elementos normativos dentro de un reglamento.
 
### Tipo de Trigger
INSTEAD OF INSERT
 
### Objetivo
Cuando se inserta una nueva versión de un elemento normativo marcada como “Vigente”, el sistema:
 
- Busca la versión vigente anterior con la misma etiqueta y mismo elemento padre dentro del mismo reglamento.
- Cambia automáticamente su estado a “Histórico”.
- Actualiza la fecha de finalización de vigencia.
- Inserta la nueva versión vigente evitando conflictos de unicidad.
 
---
 
### Justificación Técnica
 
La implementación original utilizaba un trigger `BEFORE INSERT FOR EACH ROW`.
 
En Azure SQL Server fue migrado a `INSTEAD OF INSERT` debido a que el índice parcial `uq_etiqueta_vigente` valida unicidad durante el proceso de inserción.
 
Un trigger `AFTER INSERT` se ejecutaría demasiado tarde, provocando que el índice rechazara la nueva reforma antes de poder archivar la versión anterior.

### Tablas relacionadas
- elemento_normativo
- catalogo_estado_vigencia
- reglamento
- catalogo_nivel_reglamento
 
---
 
### Estados utilizados
- Vigente
- Histórico
 
---
 
### Funcionalidad principal
- Mantener historial normativo automático.
- Garantizar una única versión vigente por etiqueta.
- Preservar trazabilidad histórica normativa.
- Evitar conflictos con índices de unicidad.
