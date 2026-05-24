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

# 6. Módulo de Sesiones, Propuestas y Resoluciones (Issue #10 Parte II)

Módulo encargado del proceso legislativo del AIR. Conecta las sesiones plenarias con las propuestas de reforma, las resoluciones oficiales y los cambios al reglamento. Reutiliza el trigger `tg_vigencia_normativa` del Sprint 2 para el versionamiento automático del articulado.

**Nota de integración:** Las tablas `sesion` y `asistencia_sesion_plenaria` son propiedad del módulo de Quórum (Issue #11). Este módulo se apoya en ellas mediante FK, no las crea.

---

## Tabla: acta

Acta formal de cada sesión plenaria. Relación 1:1 con `sesion`.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_acta | INT IDENTITY | PK | Identificador del acta |
| id_sesion | INT | NOT NULL, UNIQUE, FK | Sesión asociada |
| id_tipo_modalidad | INT | FK | Modalidad (presencial, virtual, mixta) |
| fecha_aprobacion | DATE | NULL | Fecha en que se aprobó el acta |
| url_documento | NVARCHAR(500) | NULL | URL del PDF oficial |
| link_acta | NVARCHAR(500) | NULL | Link adicional del acta |
| observaciones | NVARCHAR(MAX) | NULL | Observaciones |

La FK `fk_acta_sesion` se activa después de que el Issue #11 cree la tabla `sesion`.

---

## Tabla: propuesta

Mociones o propuestas de reforma presentadas ante el plenario. Soporta recursión base → conciliada mediante `id_propuesta_padre`.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_propuesta | INT IDENTITY | PK | Identificador de la propuesta |
| codigo_air | NVARCHAR(30) | NOT NULL, UNIQUE | Código oficial (ej. AIR-99-2024) |
| titulo | NVARCHAR(300) | NOT NULL | Título de la propuesta |
| texto_sustitutivo | NVARCHAR(MAX) | NULL | Texto propuesto del artículo |
| id_reglamento_base | INT | FK | Reglamento al que aplica |
| id_propuesta_padre | INT | FK (recursivo) | Propuesta base (NULL si es base) |
| id_etapa_propuesta | INT | NOT NULL, FK | Etapa actual (ETAPA_PROPUESTA) |
| id_estado_propuesta | INT | NOT NULL, FK | Estado (ESTADO_PROPUESTA) |
| id_tipo_mayoria_requerida | INT | NOT NULL, FK | Mayoría requerida (TIPO_MAYORIA) |
| link_documentacion | NVARCHAR(500) | NULL | URL de documentación de respaldo |
| fecha_registro | DATETIME2 | DEFAULT SYSUTCDATETIME() | Fecha de registro |

---

## Tabla: proponente_propuesta

Relación N:M entre asambleísta y propuesta. Permite autoría múltiple sobre una misma moción.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_proponente_propuesta | INT IDENTITY | PK | Identificador del registro |
| id_propuesta | INT | NOT NULL, FK | Propuesta relacionada |
| id_asambleista | INT | NOT NULL, FK | Asambleísta proponente |
| fecha_registro | DATETIME2 | DEFAULT SYSUTCDATETIME() | Fecha de registro |

Restricción UNIQUE sobre `(id_propuesta, id_asambleista)` para evitar duplicados.

---

## Tabla: bitacora_propuesta

Bitácora inmutable de cambios de estado y etapa de cada propuesta.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_registro_bitacora | INT IDENTITY | PK | Identificador del registro |
| id_propuesta | INT | NOT NULL, FK | Propuesta afectada |
| id_reglamento_base | INT | FK | Reglamento al que aplica |
| id_etapa_propuesta | INT | NOT NULL, FK | Etapa al momento del cambio |
| id_estado_propuesta | INT | NOT NULL, FK | Estado al momento del cambio |
| titulo | NVARCHAR(300) | NOT NULL | Título de la propuesta |
| codigo_air | NVARCHAR(30) | NOT NULL | Código AIR de la propuesta |
| fecha_modificacion | DATETIME2 | DEFAULT SYSUTCDATETIME() | Fecha del cambio |
| usuario_modificacion | INT | FK | Usuario que registró el cambio |

---

## Tabla: punto_agenda

Ítems formales de la sesión plenaria. Cada propuesta puede aparecer como máximo una vez por sesión.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_punto_agenda | INT IDENTITY | PK | Identificador del punto |
| id_sesion | INT | NOT NULL, FK | Sesión a la que pertenece |
| id_propuesta | INT | NOT NULL, FK | Propuesta discutida |
| orden | INT | NOT NULL | Posición en el orden del día |
| descripcion | NVARCHAR(500) | NULL | Descripción del punto |

Restricciones UNIQUE sobre `(id_sesion, id_propuesta)` y `(id_sesion, orden)`.
La FK `fk_punto_sesion` se activa después de que el Issue #11 cree la tabla `sesion`.

---

## Tabla: resolucion_propuesta

Resolución oficial derivada de un punto de agenda aprobado. Es el número que cita la certificación legal (ej. AIR-RES-001-2024).

**Nota:** Esta tabla se llama `resolucion_propuesta` (no `resolucion`) para evitar conflicto con la tabla `resolucion` del motor de votaciones del Issue #12, que representa un concepto diferente (resoluciones derivadas de votaciones).

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_resolucion_propuesta | INT IDENTITY | PK | Identificador de la resolución |
| id_punto_agenda | INT | NOT NULL, UNIQUE, FK | Punto de agenda asociado |
| numero_resolucion | NVARCHAR(30) | NOT NULL, UNIQUE | Número oficial (AIR-RES-XXX-YYYY) |
| fecha_emision | DATETIME2 | DEFAULT SYSUTCDATETIME() | Fecha de emisión |

---

## Tabla: reforma_aplicada

Rastro legal de cada cambio normativo derivado de una resolución. El trigger `tg_vigencia_normativa` (Sprint 2) se encarga del versionamiento automático en `elemento_normativo`.

| Campo | Tipo | Restricción | Descripción |
|---|---|---|---|
| id_reforma | INT IDENTITY | PK | Identificador de la reforma |
| id_resolucion_propuesta | INT | NOT NULL, FK | Resolución que origina la reforma |
| id_elemento_normativo | INT | NOT NULL, FK | Elemento normativo afectado |
| id_tipo_reforma | INT | NOT NULL, FK | Tipo de reforma (TIPO_REFORMA) |
| texto_anterior | NVARCHAR(MAX) | NULL | Redacción previa del artículo |
| texto_nuevo | NVARCHAR(MAX) | NULL | Nueva redacción del artículo |
| fecha_inicio_vigencia | DATE | DEFAULT CAST(SYSUTCDATETIME() AS DATE) | Fecha de entrada en vigencia |