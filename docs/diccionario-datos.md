Tabla: catalogo_maestro
| Campo | Tipo | Descripción |
|---|---|---|
| id_item | INT IDENTITY | ID del catálogo |
| grupo_catalogo | NVARCHAR(40) | Grupo del catálogo |
| nombre | NVARCHAR(120) | Nombre del valor |
| activo | BIT | Estado activo/inactivo |
 
 Tabla: control_folio
| Campo | Tipo | Descripción |
|---|---|---|
| id_control | INT IDENTITY | ID del control |
| anio | INT | Año del consecutivo |
| prefijo | NVARCHAR(10) | Prefijo institucional |
| ultimo_numero | INT | Último número usado |
| fecha_actualizacion | DATETIME2 | Fecha de actualización |
 
Tabla: certificacion_emitida
| Campo | Tipo | Descripción |
|---|---|---|
| id_certificacion | INT IDENTITY | ID de certificación |
| id_asambleista | INT | Asambleísta relacionado |
| folio_unico | NVARCHAR(30) | Folio generado |
| hash_seguridad | NVARCHAR(80) | Hash de seguridad |
| fecha_emision | DATETIME2 | Fecha de emisión |
| usuario_secretaria | INT | Usuario que emite |
 
Tabla: bitacora_asambleistas
| Campo | Tipo | Descripción |
|---|---|---|
| id_bitacora_asambleista | INT IDENTITY | ID de bitácora |
| id_asambleista | INT | Asambleísta afectado |
| cedula_anterior | NVARCHAR(20) | Cédula anterior |
| nombre_anterior | NVARCHAR(150) | Nombre anterior |
| razon_cambio | NVARCHAR(200) | Razón del cambio |
| fecha_actualizacion | DATETIME2 | Fecha del cambio |
 
Tabla: sys_log_auditoria
| Campo | Tipo | Descripción |
|---|---|---|
| id_log | INT IDENTITY | ID del log |
| id_usuario | INT | Usuario que realizó la acción |
| accion | NVARCHAR(20) | INSERT, UPDATE o DELETE |
| tabla_afectada | NVARCHAR(60) | Tabla modificada |
| registro_id | INT | Registro afectado |
| detalle | NVARCHAR(MAX) | Detalle del cambio |
| fecha_hora | DATETIME2 | Fecha/hora del evento |
| ip_origen | NVARCHAR(45) | IP de origen |
