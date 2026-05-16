# Diccionario de Datos - Sistema AIR

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

