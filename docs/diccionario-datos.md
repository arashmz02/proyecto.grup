# Diccionario General de Datos

---

# 1. Módulo de Seguridad y Roles

## Tabla: sys_rol

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_rol | INT IDENTITY | PK | Identificador del rol |
| nombre_rol | NVARCHAR(50) | NOT NULL, UNIQUE | Nombre del rol |

---

## Tabla: sys_permiso

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_permiso | INT IDENTITY | PK | Identificador del permiso |
| nombre_permiso | NVARCHAR(80) | NOT NULL, UNIQUE | Nombre del permiso |
| descripcion | NVARCHAR(200) | NULL | Descripción del permiso |

---

## Tabla: sys_usuario

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_usuario | INT IDENTITY | PK | Identificador del usuario |
| username | NVARCHAR(50) | NOT NULL, UNIQUE | Nombre de usuario |
| password_hash | NVARCHAR(200) | NOT NULL | Contraseña cifrada |
| email | NVARCHAR(120) | NOT NULL, UNIQUE | Correo electrónico |
| activo | BIT | DEFAULT 1 | Estado activo/inactivo |

---

## Tabla: sys_usuario_rol

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_usuario | INT | PK, FK | Usuario relacionado |
| id_rol | INT | PK, FK | Rol asignado |

---

## Tabla: sys_rol_permiso

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_rol | INT | PK, FK | Rol relacionado |
| id_permiso | INT | PK, FK | Permiso asignado |

---

## Tabla: sys_log_auditoria

Tabla de auditoría general del sistema.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_log | INT IDENTITY | PK | Identificador del registro |
| id_usuario | INT | FK | Usuario que realizó la acción |
| accion | NVARCHAR(20) | NOT NULL | Acción realizada |
| tabla_afectada | NVARCHAR(60) | NOT NULL | Tabla afectada |
| registro_id | INT | NULL | Registro modificado |
| detalle | NVARCHAR(MAX) | NULL | Detalle del cambio |
| fecha_hora | DATETIME2 | DEFAULT SYSUTCDATETIME() | Fecha y hora del evento |
| ip_origen | NVARCHAR(45) | NULL | Dirección IP de origen |

---

# 2. Catálogo Maestro

## Tabla: catalogo_maestro

Tabla para manejar catálogos reutilizables del sistema.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_item | INT IDENTITY | PK | Identificador del catálogo |
| grupo_catalogo | NVARCHAR(40) | NOT NULL | Grupo del catálogo |
| nombre | NVARCHAR(120) | NOT NULL | Nombre del elemento |
| activo | BIT | DEFAULT 1 | Estado activo/inactivo |

---

# 3. Módulo de Identidad y Nombramientos

## Tabla: asambleista

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_asambleista | INT IDENTITY | PK | Identificador del asambleísta |
| cedula | NVARCHAR(20) | NOT NULL, UNIQUE | Cédula |
| nombre | NVARCHAR(150) | NOT NULL | Nombre completo |
| correo_institucional | NVARCHAR(120) | NOT NULL, UNIQUE | Correo institucional |

---

## Tabla: bitacora_asambleistas

Tabla utilizada para registrar cambios históricos de identidad.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_bitacora_asambleista | INT IDENTITY | PK | Identificador de la bitácora |
| id_asambleista | INT | FK | Asambleísta relacionado |
| cedula_anterior | NVARCHAR(20) | NULL | Cédula anterior |
| nombre_anterior | NVARCHAR(150) | NULL | Nombre anterior |
| razon_cambio | NVARCHAR(200) | NULL | Motivo del cambio |
| fecha_actualizacion | DATETIME2 | DEFAULT SYSUTCDATETIME() | Fecha del cambio |

---

## Trigger: tg_cambio_identidad

Trigger encargado de registrar automáticamente cambios de identidad realizados sobre asambleistas.

### Funcionalidad
- Detecta cambios de cédula o nombre.
- Guarda automáticamente los valores anteriores.
- Mantiene trazabilidad histórica de identidad.

---

## Tabla: nombramiento

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_nombramiento | INT IDENTITY | PK | Identificador del nombramiento |
| id_asambleista | INT | NOT NULL, FK | Asambleísta relacionado |
| id_sector | INT | NOT NULL, FK | Sector asignado |
| id_puesto | INT | FK | Puesto asignado |
| resolucion_id | INT | NULL | Resolución asociada |
| fecha_inicio | DATE | NOT NULL | Fecha de inicio |
| fecha_fin | DATE | NULL | Fecha de finalización |
| estado | NVARCHAR(20) | DEFAULT 'Vigente' | Estado del nombramiento |
| id_usuario_registro | INT | FK | Usuario que registró |
| fecha_registro | DATETIME2 | DEFAULT SYSUTCDATETIME() | Fecha del registro |

---

## Trigger: tg_traslape_sector

Trigger encargado de validar que un asambleísta no tenga nombramientos traslapados.

### Funcionalidad
- Detecta traslapes de fechas.
- Rechaza la transacción si existe conflicto.
- Garantiza integridad de periodos de nombramiento.

---

# 4. Módulo de Jerarquía Normativa

## Tabla: catalogo_nivel_reglamento

| Campo | Tipo | Descripción |
|---|---|---|
| id_nivel_reglamento | INT IDENTITY(1,1) | ID único del nivel |
| nombre | NVARCHAR(40) | Nombre del nivel |
| orden | INT | Orden jerárquico |

---

## Tabla: catalogo_estado_vigencia

| Campo | Tipo | Descripción |
|---|---|---|
| id_estado_vigencia | INT IDENTITY(1,1) | ID del estado |
| nombre | NVARCHAR(20) | Estado de vigencia |

---

## Tabla: reglamento

| Campo | Tipo | Descripción |
|---|---|---|
| id_reglamento | INT IDENTITY(1,1) | ID del reglamento |
| nombre_normativa | NVARCHAR(150) | Nombre de la normativa |
| sigla | NVARCHAR(20) | Sigla única |
| emisor | NVARCHAR(10) | Órgano emisor |

---

## Tabla: elemento_normativo

Tabla encargada de almacenar la estructura jerárquica normativa institucional.

| Campo | Tipo | Descripción |
|---|---|---|
| id_elemento | INT IDENTITY | ID único del elemento normativo |
| id_reglamento | INT | Reglamento relacionado |
| id_elemento_padre | INT | Elemento padre jerárquico |
| id_nivel_reglamento | INT | Nivel normativo |
| numero_etiqueta | NVARCHAR(20) | Etiqueta visible |
| contenido_texto | NVARCHAR(MAX) | Contenido normativo |
| orden | INT | Orden jerárquico |
| fecha_inicio_vigencia | DATE | Inicio de vigencia |
| fecha_fin_vigencia | DATE | Fin de vigencia |
| id_estado_vigencia | INT | Estado de vigencia |
| id_acuerdo_origen | INT | Acuerdo origen |

---

## Trigger: tg_vigencia_normativa

Trigger encargado de gestionar automáticamente el versionamiento histórico normativo.

### Tipo de Trigger
INSTEAD OF INSERT

### Funcionalidad
- Detecta nuevas versiones vigentes.
- Archiva automáticamente la versión anterior.
- Actualiza fechas históricas.
- Inserta la nueva versión normativa.
- Garantiza una única versión vigente por etiqueta.

### Justificación Técnica
Se utiliza `INSTEAD OF INSERT` porque el índice parcial de unicidad se valida durante el INSERT y un trigger AFTER no permitiría archivar previamente la versión anterior.

---

# 5. Control de Folios y Certificaciones

## Tabla: control_folio

Tabla utilizada para manejar consecutivos institucionales.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_control | INT IDENTITY | PK | Identificador del control |
| anio | INT | NOT NULL | Año del consecutivo |
| prefijo | NVARCHAR(10) | DEFAULT 'DAIR' | Prefijo institucional |
| ultimo_numero | INT | DEFAULT 0 | Último consecutivo utilizado |
| fecha_actualizacion | DATETIME2 | DEFAULT SYSUTCDATETIME() | Fecha de actualización |

---

## Tabla: certificacion_emitida

Tabla donde se almacenan certificaciones emitidas.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_certificacion | INT IDENTITY | PK | Identificador de certificación |
| id_asambleista | INT | FK | Asambleísta relacionado |
| folio_unico | NVARCHAR(30) | UNIQUE | Folio generado |
| hash_seguridad | NVARCHAR(80) | NULL | Código hash de seguridad |
| fecha_emision | DATETIME2 | DEFAULT SYSUTCDATETIME() | Fecha de emisión |
| usuario_secretaria | INT | FK | Usuario emisor |

---

## Trigger: tg_folio_secuencial

Trigger encargado de generar automáticamente folios consecutivos institucionales.

### Funcionalidad
- Genera folios únicos automáticos.
- Controla concurrencia mediante bloqueo.
- Mantiene consecutivos por año.
- Garantiza integridad de foliado.

# 6. Módulo de Sesiones, Propuestas y Resoluciones
## Tabla: sesiones

| Campo | Tipo | Restricción | Descripción |
| id_sesion | INT IDENTITY | PK | Identificador de la sesión |
| numero_sesion | NVARCHAR(20) | NOT NULL, UNIQUE | Número oficial (ej. AIR-110-2024) |
| fecha | DATE | NOT NULL | Fecha de realización |
| id_tipo_sesion | INT | NOT NULL, FK | Tipo de sesión |
| id_tipo_modalidad | INT | NOT NULL, FK | Modalidad |
| quorum_requerido | INT | NOT NULL, DEFAULT 0, CHECK ≥ 0 | Quórum mínimo legal |
| link_acta | NVARCHAR(500) | NULL | URL del acta digital |

## Tabla: acta

| Campo | Tipo | Restricción | Descripción |
| id_acta | INT IDENTITY | PK | Identificador del acta |
| id_sesion | INT | NOT NULL, UNIQUE, FK | Sesión asociada |
| fecha_aprobacion | DATE | NULL | Fecha de aprobación |
| url_documento | NVARCHAR(500) | NULL | Documento PDF |
| observaciones | NVARCHAR(MAX) | NULL | Observaciones |

## Tabla: propuesta

| Campo | Tipo | Restricción | Descripción |
| id_propuesta | INT IDENTITY | PK | Identificador de la propuesta |
| codigo_air | NVARCHAR(30) | NOT NULL, UNIQUE | Código oficial |
| titulo | NVARCHAR(300) | NOT NULL | Título |
| texto_sustitutivo | NVARCHAR(MAX) | NULL | Texto propuesto |
| id_reglamento_base | INT | FK | Reglamento asociado |
| id_propuesta_padre | INT | FK | Propuesta base (recursiva) |
| id_etapa_propuesta | INT | NOT NULL, FK | Etapa |
| id_estado_propuesta | INT | NOT NULL, FK | Estado |
| id_tipo_mayoria_requerida | INT | NOT NULL, FK | Tipo de mayoría |
| link_documentacion | NVARCHAR(500) | NULL | Documentación |
| fecha_registro | DATETIME2 | DEFAULT SYSUTCDATETIME() | Fecha de registro |

## Tabla: proponente_propuesta

| Campo | Tipo | Restricción | Descripción |
| id_proponente_propuesta | INT IDENTITY | PK | Identificador |
| id_propuesta | INT | NOT NULL, FK | Propuesta |
| id_asambleista | INT | NOT NULL, FK | Asambleísta |
| fecha_registro | DATETIME2 | DEFAULT SYSUTCDATETIME() | Fecha |

Restricción UNIQUE sobre (id_propuesta, id_asambleista)

## Tabla: bitacora_propuesta

| Campo | Tipo | Restricción | Descripción |
| id_registro_bitacora | INT IDENTITY | PK | Identificador |
| id_propuesta | INT | NOT NULL, FK | Propuesta |
| id_reglamento_base | INT | FK | Reglamento |
| id_etapa_propuesta | INT | NOT NULL, FK | Etapa |
| id_estado_propuesta | INT | NOT NULL, FK | Estado |
| titulo | NVARCHAR(300) | NOT NULL | Título |
| codigo_air | NVARCHAR(30) | NOT NULL | Código |
| fecha_modificacion | DATETIME2 | DEFAULT SYSUTCDATETIME() | Fecha |
| usuario_modificacion | INT | FK | Usuario |

## Tabla: punto_agenda

| Campo | Tipo | Restricción | Descripción |
| id_punto_agenda | INT IDENTITY | PK | Identificador |
| id_sesion | INT | NOT NULL, FK | Sesión |
| id_propuesta | INT | NOT NULL, FK | Propuesta |
| orden | INT | NOT NULL | Orden |
| descripcion | NVARCHAR(500) | NULL | Descripción |

Restricción UNIQUE sobre (id_sesion, id_propuesta)
Restricción UNIQUE sobre (id_sesion, orden)

## Tabla: resolucion

| Campo | Tipo | Restricción | Descripción |
| id_resolucion | INT IDENTITY | PK | Identificador |
| id_punto_agenda | INT | NOT NULL, UNIQUE, FK | Punto de agenda |
| numero_resolucion | NVARCHAR(30) | NOT NULL, UNIQUE | Número oficial |
| fecha_emision | DATETIME2 | DEFAULT SYSUTCDATETIME() | Fecha |

## Tabla: reforma_aplicada

| Campo | Tipo | Restricción | Descripción |
| id_reforma | INT IDENTITY | PK | Identificador |
| id_resolucion | INT | NOT NULL, FK | Resolución |
| id_elemento_normativo | INT | NOT NULL, FK | Elemento afectado |
| id_tipo_reforma | INT | NOT NULL, FK | Tipo de reforma |
| texto_anterior | NVARCHAR(MAX) | NULL | Texto anterior |
| texto_nuevo | NVARCHAR(MAX) | NULL | Texto nuevo |
| fecha_inicio_vigencia | DATE | DEFAULT CAST(SYSUTCDATETIME() AS DATE) | Vigencia |

## Tabla: asistencia_sesion_plenaria

| Campo | Tipo | Restricción | Descripción |
| id_asistencia | INT IDENTITY | PK | Identificador |
| id_sesion | INT | NOT NULL, FK | Sesión |
| id_asambleista | INT | NOT NULL, FK | Asambleísta |
| id_estado_asistencia | INT | NOT NULL, FK | Estado |

Restricción UNIQUE sobre (id_sesion, id_asambleista)

Restricciones FK activadas en Sprint 3

| Tabla | Columna | FK | Referencia |
| nombramiento | resolucion_id | fk_nombramiento_resolucion | resolucion(id_resolucion) |
| elemento_normativo | id_acuerdo_origen | fk_elemento_acuerdo_origen | resolucion(id_resolucion) |