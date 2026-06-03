
/* 
   PROYECTO-AIR.SQL
   Sistema de Gestion Legislativa AIR (SGL-AIR)
   Instituto Tecnologico de Costa Rica
 
   Motor: Azure SQL Database (T-SQL)
 
   ORDEN DEL SCRIPT (importante por dependencias de FK):
     1. DROP de objetos (orden inverso a la creacion)
     2. Modulo de Seguridad y Roles        (Frank  - Issue #0)
     3. Catalogo Maestro                   (Arash  - Issue #1)
     4. Modulo de Identidad y Nombramientos(Frank  - Issue #9)
     5. Modulo de Jerarquia Normativa      (Josue  - Issue #10)
     6. Control de Folios y Certificaciones(Arash  - Issue #1)
     6.5 Sesiones, Propuestas y Resoluciones (Josue - Issue #10 Parte II)
     7. Triggers
     8. Datos semilla
     9. Issue #11 - Control de Quorum (Frank)
    10. Issue #13 - Bitacora Certificaciones (Frank)
    11. Issue #12 - Motor de Votaciones (Frank)
    12. Issue #15 - Anulaciones y Sustituciones (Arash) */
 
 
/* 1. LIMPIEZA - DROP EN ORDEN INVERSO
   Se eliminan primero los objetos que dependen de otros. */
 
--Triggers
IF OBJECT_ID('tg_cambio_identidad', 'TR')      IS NOT NULL DROP TRIGGER tg_cambio_identidad;
IF OBJECT_ID('tg_folio_secuencial', 'TR')      IS NOT NULL DROP TRIGGER tg_folio_secuencial;
IF OBJECT_ID('tg_vigencia_normativa', 'TR')    IS NOT NULL DROP TRIGGER tg_vigencia_normativa;
IF OBJECT_ID('tg_traslape_sector', 'TR')       IS NOT NULL DROP TRIGGER tg_traslape_sector;
IF OBJECT_ID('tg_auditoria_nombramiento', 'TR')IS NOT NULL DROP TRIGGER tg_auditoria_nombramiento;
IF OBJECT_ID('tg_auditoria_asambleista', 'TR') IS NOT NULL DROP TRIGGER tg_auditoria_asambleista;
 
--Tablas (hijas antes que padres)
IF OBJECT_ID('reforma_aplicada', 'U')           IS NOT NULL DROP TABLE reforma_aplicada;
IF OBJECT_ID('resolucion_propuesta', 'U')       IS NOT NULL DROP TABLE resolucion_propuesta;
IF OBJECT_ID('punto_agenda', 'U')               IS NOT NULL DROP TABLE punto_agenda;
IF OBJECT_ID('bitacora_propuesta', 'U')         IS NOT NULL DROP TABLE bitacora_propuesta;
IF OBJECT_ID('proponente_propuesta', 'U')       IS NOT NULL DROP TABLE proponente_propuesta;
IF OBJECT_ID('propuesta', 'U')                  IS NOT NULL DROP TABLE propuesta;
IF OBJECT_ID('acta', 'U')                       IS NOT NULL DROP TABLE acta;
IF OBJECT_ID('certificacion_emitida', 'U')     IS NOT NULL DROP TABLE certificacion_emitida;
IF OBJECT_ID('control_folio', 'U')             IS NOT NULL DROP TABLE control_folio;
IF OBJECT_ID('elemento_normativo', 'U')        IS NOT NULL DROP TABLE elemento_normativo;
IF OBJECT_ID('reglamento', 'U')                IS NOT NULL DROP TABLE reglamento;
IF OBJECT_ID('catalogo_estado_vigencia', 'U')  IS NOT NULL DROP TABLE catalogo_estado_vigencia;
IF OBJECT_ID('catalogo_nivel_reglamento', 'U') IS NOT NULL DROP TABLE catalogo_nivel_reglamento;
IF OBJECT_ID('nombramiento', 'U')              IS NOT NULL DROP TABLE nombramiento;
IF OBJECT_ID('bitacora_asambleistas', 'U')     IS NOT NULL DROP TABLE bitacora_asambleistas;
IF OBJECT_ID('asambleista', 'U')               IS NOT NULL DROP TABLE asambleista;
IF OBJECT_ID('catalogo_maestro', 'U')          IS NOT NULL DROP TABLE catalogo_maestro;
IF OBJECT_ID('sys_log_auditoria', 'U')         IS NOT NULL DROP TABLE sys_log_auditoria;
IF OBJECT_ID('sys_rol_permiso', 'U')           IS NOT NULL DROP TABLE sys_rol_permiso;
IF OBJECT_ID('sys_usuario_rol', 'U')           IS NOT NULL DROP TABLE sys_usuario_rol;
IF OBJECT_ID('sys_usuario', 'U')               IS NOT NULL DROP TABLE sys_usuario;
IF OBJECT_ID('sys_permiso', 'U')               IS NOT NULL DROP TABLE sys_permiso;
IF OBJECT_ID('sys_rol', 'U')                   IS NOT NULL DROP TABLE sys_rol;
GO
 
 
/* 2. MODULO DE SEGURIDAD Y ROLES (sys_*)
   Responsable: Frank - Issue #0 */
 
CREATE TABLE sys_rol (
    id_rol      INT IDENTITY(1,1) PRIMARY KEY,
    nombre_rol  NVARCHAR(50) NOT NULL UNIQUE
);
GO
 
CREATE TABLE sys_permiso (
    id_permiso      INT IDENTITY(1,1) PRIMARY KEY,
    nombre_permiso  NVARCHAR(80) NOT NULL UNIQUE,
    descripcion     NVARCHAR(200)
);
GO
 
CREATE TABLE sys_usuario (
    id_usuario     INT IDENTITY(1,1) PRIMARY KEY,
    username       NVARCHAR(50)  NOT NULL UNIQUE,
    password_hash  NVARCHAR(200) NOT NULL,
    email          NVARCHAR(120) NOT NULL UNIQUE,
    activo         BIT NOT NULL DEFAULT 1
);
GO
 
CREATE TABLE sys_usuario_rol (
    id_usuario INT NOT NULL,
    id_rol     INT NOT NULL,
    CONSTRAINT pk_sys_usuario_rol PRIMARY KEY (id_usuario, id_rol),
    CONSTRAINT fk_usuariorol_usuario FOREIGN KEY (id_usuario) REFERENCES sys_usuario(id_usuario),
    CONSTRAINT fk_usuariorol_rol     FOREIGN KEY (id_rol)     REFERENCES sys_rol(id_rol)
);
GO
 
CREATE TABLE sys_rol_permiso (
    id_rol     INT NOT NULL,
    id_permiso INT NOT NULL,
    CONSTRAINT pk_sys_rol_permiso PRIMARY KEY (id_rol, id_permiso),
    CONSTRAINT fk_rolpermiso_rol     FOREIGN KEY (id_rol)     REFERENCES sys_rol(id_rol),
    CONSTRAINT fk_rolpermiso_permiso FOREIGN KEY (id_permiso) REFERENCES sys_permiso(id_permiso)
);
GO
 
CREATE TABLE sys_log_auditoria (
    id_log          INT IDENTITY(1,1) PRIMARY KEY,
    id_usuario      INT,
    accion          NVARCHAR(20) NOT NULL,
    tabla_afectada  NVARCHAR(60) NOT NULL,
    registro_id     INT,
    detalle         NVARCHAR(MAX),
    fecha_hora      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ip_origen       NVARCHAR(45),
    CONSTRAINT fk_logauditoria_usuario FOREIGN KEY (id_usuario) REFERENCES sys_usuario(id_usuario)
);
GO
 
 
/* 3. CATALOGO MAESTRO (Universal Lookup Table)
   Responsable: Arash - Issue #1 */
 
CREATE TABLE catalogo_maestro (
    id_item        INT IDENTITY(1,1) PRIMARY KEY,
    grupo_catalogo NVARCHAR(40)  NOT NULL,
    nombre         NVARCHAR(120) NOT NULL,
    activo         BIT NOT NULL DEFAULT 1,
    CONSTRAINT uq_catalogo_grupo_nombre UNIQUE (grupo_catalogo, nombre)
);
GO
 
 
/* 4. MODULO DE IDENTIDAD Y NOMBRAMIENTOS
   Responsable: Frank - Issue #9 */
 
CREATE TABLE asambleista (
    id_asambleista       INT IDENTITY(1,1) PRIMARY KEY,
    cedula               NVARCHAR(20)  NOT NULL UNIQUE,
    nombre               NVARCHAR(150) NOT NULL,
    correo_institucional NVARCHAR(120) NOT NULL UNIQUE
);
GO
 
CREATE TABLE bitacora_asambleistas (
    id_bitacora_asambleista INT IDENTITY(1,1) PRIMARY KEY,
    id_asambleista          INT,
    cedula_anterior         NVARCHAR(20),
    nombre_anterior         NVARCHAR(150),
    razon_cambio            NVARCHAR(200),
    fecha_actualizacion     DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_bitacora_asambleista FOREIGN KEY (id_asambleista) REFERENCES asambleista(id_asambleista)
);
GO
 
CREATE TABLE nombramiento (
    id_nombramiento     INT IDENTITY(1,1) PRIMARY KEY,
    id_asambleista      INT NOT NULL,
    id_sector           INT NOT NULL,
    id_puesto           INT,
    resolucion_id       INT,
    fecha_inicio        DATE NOT NULL,
    fecha_fin           DATE,
    estado              NVARCHAR(20) NOT NULL DEFAULT 'Vigente',
    id_usuario_registro INT,
    fecha_registro      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_nombramiento_asambleista FOREIGN KEY (id_asambleista)      REFERENCES asambleista(id_asambleista),
    CONSTRAINT fk_nombramiento_sector      FOREIGN KEY (id_sector)           REFERENCES catalogo_maestro(id_item),
    CONSTRAINT fk_nombramiento_puesto      FOREIGN KEY (id_puesto)           REFERENCES catalogo_maestro(id_item),
    CONSTRAINT fk_nombramiento_usuario     FOREIGN KEY (id_usuario_registro) REFERENCES sys_usuario(id_usuario),
    CONSTRAINT chk_fechas_nombramiento CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);
GO
 
 
/* 5. MODULO DE JERARQUIA NORMATIVA
   Responsable: Josue - Issue #10 */
 
CREATE TABLE catalogo_nivel_reglamento (
    id_nivel_reglamento INT IDENTITY(1,1) PRIMARY KEY,
    nombre              NVARCHAR(40) NOT NULL UNIQUE,
    orden               INT NOT NULL
);
GO
 
CREATE TABLE catalogo_estado_vigencia (
    id_estado_vigencia INT IDENTITY(1,1) PRIMARY KEY,
    nombre             NVARCHAR(20) NOT NULL UNIQUE
);
GO
 
CREATE TABLE reglamento (
    id_reglamento    INT IDENTITY(1,1) PRIMARY KEY,
    nombre_normativa NVARCHAR(150) NOT NULL,
    sigla            NVARCHAR(20)  NOT NULL UNIQUE,
    emisor           NVARCHAR(10)  CHECK (emisor IN ('AIR', 'CI'))
);
GO
 
CREATE TABLE elemento_normativo (
    id_elemento           INT IDENTITY(1,1) PRIMARY KEY,
    id_reglamento         INT NOT NULL,
    id_elemento_padre     INT,
    id_nivel_reglamento   INT NOT NULL,
    numero_etiqueta       NVARCHAR(20)  NOT NULL,
    contenido_texto       NVARCHAR(MAX) NOT NULL,
    orden                 INT NOT NULL,
    fecha_inicio_vigencia DATE NOT NULL DEFAULT CAST(SYSUTCDATETIME() AS DATE),
    fecha_fin_vigencia    DATE,
    id_estado_vigencia    INT NOT NULL,
    id_acuerdo_origen     INT,
    CONSTRAINT fk_elemento_reglamento FOREIGN KEY (id_reglamento)       REFERENCES reglamento(id_reglamento),
    CONSTRAINT fk_elemento_padre      FOREIGN KEY (id_elemento_padre)   REFERENCES elemento_normativo(id_elemento),
    CONSTRAINT fk_elemento_nivel      FOREIGN KEY (id_nivel_reglamento) REFERENCES catalogo_nivel_reglamento(id_nivel_reglamento),
    CONSTRAINT fk_elemento_estado     FOREIGN KEY (id_estado_vigencia)  REFERENCES catalogo_estado_vigencia(id_estado_vigencia)
);
GO
 
ALTER TABLE elemento_normativo
    ADD padre_norm AS (ISNULL(id_elemento_padre, 0)) PERSISTED;
GO
 
CREATE UNIQUE INDEX uq_etiqueta_vigente
    ON elemento_normativo (id_reglamento, padre_norm, numero_etiqueta)
    WHERE id_estado_vigencia = 1;
GO
 
 
/* 6. CONTROL DE FOLIOS Y CERTIFICACIONES
   Responsable: Arash - Issue #1 */
 
CREATE TABLE control_folio (
    id_control          INT IDENTITY(1,1) PRIMARY KEY,
    anio                INT NOT NULL,
    prefijo             NVARCHAR(10) NOT NULL DEFAULT 'DAIR',
    ultimo_numero       INT NOT NULL DEFAULT 0,
    fecha_actualizacion DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_control_anio_prefijo UNIQUE (anio, prefijo)
);
GO
 
CREATE TABLE certificacion_emitida (
    id_certificacion   INT IDENTITY(1,1) PRIMARY KEY,
    id_asambleista     INT,
    folio_unico        NVARCHAR(30) NOT NULL UNIQUE,
    hash_seguridad     NVARCHAR(80),
    fecha_emision      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    usuario_secretaria INT,
    CONSTRAINT fk_certificacion_asambleista FOREIGN KEY (id_asambleista)     REFERENCES asambleista(id_asambleista),
    CONSTRAINT fk_certificacion_usuario     FOREIGN KEY (usuario_secretaria) REFERENCES sys_usuario(id_usuario)
);
GO
 
 
/* 
   6.5 SPRINT 3 - ISSUE #10 PARTE II
   MODULO DE SESIONES, PROPUESTAS Y RESOLUCIONES
   Responsable: Josue 
   
   NOTAS DE INTEGRACION:
   - Las tablas 'sesion' y 'asistencia_sesion_plenaria' son creadas por Frank en Issue #11 (mas abajo en este archivo). Las FKde este modulo apuntan a ellas.
   - La tabla 'resolucion' de Frank (Issue #12) tiene un proposito distinto (resoluciones derivadas de votaciones). Por eso esta tabla se llama 'resolucion_propuesta' para evitar conflicto.
   - Este modulo APROVECHA el trigger tg_vigencia_normativa del Sprint 2 para el versionamiento automatico del articulado.
 
   NOTA: Como las tablas de Frank se crean MAS ABAJO en este archivo, las tablas de este modulo no pueden declarar las FK
   a 'sesion' en su CREATE TABLE. Esas FK se agregan al final de Issue #11 con ALTER TABLE. */
 
--6.5.1 Actas (1:1 con sesion: UNIQUE en id_sesion garantiza una acta por sesion)
CREATE TABLE acta (
    id_acta           INT IDENTITY(1,1) PRIMARY KEY,
    id_sesion         INT NOT NULL UNIQUE,
    id_tipo_modalidad INT,
    fecha_aprobacion  DATE,
    url_documento     NVARCHAR(500),
    link_acta         NVARCHAR(500),
    observaciones     NVARCHAR(MAX),
    CONSTRAINT fk_acta_modalidad FOREIGN KEY (id_tipo_modalidad) REFERENCES catalogo_maestro(id_item)
);
GO
 
--6.5.2 Propuestas (con recursividad para conciliadas via id_propuesta_padre)
CREATE TABLE propuesta (
    id_propuesta              INT IDENTITY(1,1) PRIMARY KEY,
    codigo_air                NVARCHAR(30)  NOT NULL UNIQUE,
    titulo                    NVARCHAR(300) NOT NULL,
    texto_sustitutivo         NVARCHAR(MAX),
    id_reglamento_base        INT,
    id_propuesta_padre        INT,
    id_etapa_propuesta        INT NOT NULL,
    id_estado_propuesta       INT NOT NULL,
    id_tipo_mayoria_requerida INT NOT NULL,
    link_documentacion        NVARCHAR(500),
    fecha_registro            DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_propuesta_reglamento FOREIGN KEY (id_reglamento_base)        REFERENCES reglamento(id_reglamento),
    CONSTRAINT fk_propuesta_padre      FOREIGN KEY (id_propuesta_padre)        REFERENCES propuesta(id_propuesta),
    CONSTRAINT fk_propuesta_etapa      FOREIGN KEY (id_etapa_propuesta)        REFERENCES catalogo_maestro(id_item),
    CONSTRAINT fk_propuesta_estado     FOREIGN KEY (id_estado_propuesta)       REFERENCES catalogo_maestro(id_item),
    CONSTRAINT fk_propuesta_mayoria    FOREIGN KEY (id_tipo_mayoria_requerida) REFERENCES catalogo_maestro(id_item)
);
GO
 
--6.5.3 Proponentes (autoria multiple, relacion N:M)
CREATE TABLE proponente_propuesta (
    id_proponente_propuesta INT IDENTITY(1,1) PRIMARY KEY,
    id_propuesta            INT NOT NULL,
    id_asambleista          INT NOT NULL,
    fecha_registro          DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_proponente_propuesta   FOREIGN KEY (id_propuesta)   REFERENCES propuesta(id_propuesta),
    CONSTRAINT fk_proponente_asambleista FOREIGN KEY (id_asambleista) REFERENCES asambleista(id_asambleista),
    CONSTRAINT uq_proponente UNIQUE (id_propuesta, id_asambleista)
);
GO
 
--6.5.4 Bitacora de cambios de estado de las propuestas
CREATE TABLE bitacora_propuesta (
    id_registro_bitacora INT IDENTITY(1,1) PRIMARY KEY,
    id_propuesta         INT NOT NULL,
    id_reglamento_base   INT,
    id_etapa_propuesta   INT NOT NULL,
    id_estado_propuesta  INT NOT NULL,
    titulo               NVARCHAR(300) NOT NULL,
    codigo_air           NVARCHAR(30)  NOT NULL,
    fecha_modificacion   DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    usuario_modificacion INT,
    CONSTRAINT fk_bitprop_propuesta  FOREIGN KEY (id_propuesta)         REFERENCES propuesta(id_propuesta),
    CONSTRAINT fk_bitprop_reglamento FOREIGN KEY (id_reglamento_base)   REFERENCES reglamento(id_reglamento),
    CONSTRAINT fk_bitprop_etapa      FOREIGN KEY (id_etapa_propuesta)   REFERENCES catalogo_maestro(id_item),
    CONSTRAINT fk_bitprop_estado     FOREIGN KEY (id_estado_propuesta)  REFERENCES catalogo_maestro(id_item),
    CONSTRAINT fk_bitprop_usuario    FOREIGN KEY (usuario_modificacion) REFERENCES sys_usuario(id_usuario)
);
GO
 
--6.5.5 Puntos de agenda (orden del dia de cada sesion)
-- NOTA: la FK a 'sesion' se agrega despues de que Frank crea esa
-- tabla en Issue #11.
CREATE TABLE punto_agenda (
    id_punto_agenda INT IDENTITY(1,1) PRIMARY KEY,
    id_sesion       INT NOT NULL,
    id_propuesta    INT NOT NULL,
    orden           INT NOT NULL,
    descripcion     NVARCHAR(500),
    CONSTRAINT fk_punto_propuesta FOREIGN KEY (id_propuesta) REFERENCES propuesta(id_propuesta),
    CONSTRAINT uq_punto_sesion_propuesta UNIQUE (id_sesion, id_propuesta),
    CONSTRAINT uq_punto_sesion_orden     UNIQUE (id_sesion, orden)
);
GO
 
--6.5.6 Resoluciones de propuestas (numero oficial AIR-RES-XXX-YYYY)
-- Nombre: 'resolucion_propuesta' para no chocar con la 'resolucion'
-- del motor de votaciones de Frank (Issue #12).
CREATE TABLE resolucion_propuesta (
    id_resolucion_propuesta INT IDENTITY(1,1) PRIMARY KEY,
    id_punto_agenda         INT NOT NULL UNIQUE,
    numero_resolucion       NVARCHAR(30) NOT NULL UNIQUE,
    fecha_emision           DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_resprop_punto FOREIGN KEY (id_punto_agenda) REFERENCES punto_agenda(id_punto_agenda)
);
GO
 
--6.5.7 Reforma aplicada (conecta una resolucion de propuesta con el cambio al reglamento)
CREATE TABLE reforma_aplicada (
    id_reforma              INT IDENTITY(1,1) PRIMARY KEY,
    id_resolucion_propuesta INT NOT NULL,
    id_elemento_normativo   INT NOT NULL,
    id_tipo_reforma         INT NOT NULL,
    texto_anterior          NVARCHAR(MAX),
    texto_nuevo             NVARCHAR(MAX),
    fecha_inicio_vigencia   DATE NOT NULL DEFAULT CAST(SYSUTCDATETIME() AS DATE),
    CONSTRAINT fk_reforma_resprop  FOREIGN KEY (id_resolucion_propuesta) REFERENCES resolucion_propuesta(id_resolucion_propuesta),
    CONSTRAINT fk_reforma_elemento FOREIGN KEY (id_elemento_normativo)   REFERENCES elemento_normativo(id_elemento),
    CONSTRAINT fk_reforma_tipo     FOREIGN KEY (id_tipo_reforma)         REFERENCES catalogo_maestro(id_item)
);
GO
 
 
/* 7. TRIGGERS
   Nota general de migracion PostgreSQL -> T-SQL:
   - PostgreSQL usa una FUNCTION + un TRIGGER que la llama, con
     las pseudo-filas NEW y OLD, y se dispara FOR EACH ROW.
   - T-SQL integra la logica directamente en el TRIGGER y usa
     las pseudo-tablas INSERTED y DELETED, que contienen TODAS
     las filas afectadas (el trigger se dispara una vez por
     sentencia, no por fila). Por eso la logica debe escribirse
     pensando en conjuntos, no en una sola fila.
   - El contexto de usuario (current_setting en PostgreSQL) se
     traduce a SESSION_CONTEXT en T-SQL. La aplicacion Java debe
     ejecutar, al inicio de cada conexion/transaccion:
       EXEC sp_set_session_context @key=N'usuario_id', @value=?;
       EXEC sp_set_session_context @key=N'razon_cambio', @value=?; */
 
/* 7.1 Trigger generico de auditoria  (Frank - Issue #0) */
GO
CREATE TRIGGER tg_auditoria_asambleista
ON asambleista
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
 
    DECLARE @id_usuario INT =
        TRY_CAST(CAST(SESSION_CONTEXT(N'usuario_id') AS NVARCHAR(20)) AS INT);
 
    INSERT INTO sys_log_auditoria (id_usuario, accion, tabla_afectada, registro_id, detalle)
    SELECT
        @id_usuario,
        CASE WHEN EXISTS (SELECT 1 FROM deleted) THEN 'UPDATE' ELSE 'INSERT' END,
        'asambleista',
        i.id_asambleista,
        CONCAT('cedula=', i.cedula, '; nombre=', i.nombre,
               '; correo=', i.correo_institucional)
    FROM inserted i;
 
    INSERT INTO sys_log_auditoria (id_usuario, accion, tabla_afectada, registro_id, detalle)
    SELECT
        @id_usuario,
        'DELETE',
        'asambleista',
        d.id_asambleista,
        CONCAT('cedula=', d.cedula, '; nombre=', d.nombre,
               '; correo=', d.correo_institucional)
    FROM deleted d
    WHERE NOT EXISTS (SELECT 1 FROM inserted);
END;
GO
 
CREATE TRIGGER tg_auditoria_nombramiento
ON nombramiento
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
 
    DECLARE @id_usuario INT =
        TRY_CAST(CAST(SESSION_CONTEXT(N'usuario_id') AS NVARCHAR(20)) AS INT);
 
    INSERT INTO sys_log_auditoria (id_usuario, accion, tabla_afectada, registro_id, detalle)
    SELECT
        @id_usuario,
        CASE WHEN EXISTS (SELECT 1 FROM deleted) THEN 'UPDATE' ELSE 'INSERT' END,
        'nombramiento',
        i.id_nombramiento,
        CONCAT('id_asambleista=', i.id_asambleista,
               '; id_sector=', i.id_sector,
               '; fecha_inicio=', CONVERT(NVARCHAR(10), i.fecha_inicio, 23),
               '; estado=', i.estado)
    FROM inserted i;
 
    INSERT INTO sys_log_auditoria (id_usuario, accion, tabla_afectada, registro_id, detalle)
    SELECT
        @id_usuario,
        'DELETE',
        'nombramiento',
        d.id_nombramiento,
        CONCAT('id_asambleista=', d.id_asambleista,
               '; id_sector=', d.id_sector,
               '; fecha_inicio=', CONVERT(NVARCHAR(10), d.fecha_inicio, 23),
               '; estado=', d.estado)
    FROM deleted d
    WHERE NOT EXISTS (SELECT 1 FROM inserted);
END;
GO
 
 
/* 7.2 Trigger de traslape de nombramientos  (Frank - Issue #9) */
GO
CREATE TRIGGER tg_traslape_sector
ON nombramiento
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
 
    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN nombramiento n
          ON n.id_asambleista = i.id_asambleista
         AND n.id_nombramiento <> i.id_nombramiento
        WHERE
            i.fecha_inicio <= ISNULL(n.fecha_fin, '9999-12-31')
            AND n.fecha_inicio <= ISNULL(i.fecha_fin, '9999-12-31')
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50001, 'Traslape de nombramientos detectado para el asambleista.', 1;
    END
END;
GO
 
 
/* 7.3 Trigger de versionamiento normativo  (Josue - Issue #10) */
GO
CREATE TRIGGER tg_vigencia_normativa
ON elemento_normativo
INSTEAD OF INSERT
AS
BEGIN
    SET NOCOUNT ON;
 
    DECLARE @estado_vigente   INT = (SELECT id_estado_vigencia FROM catalogo_estado_vigencia WHERE nombre = 'Vigente');
    DECLARE @estado_historico INT = (SELECT id_estado_vigencia FROM catalogo_estado_vigencia WHERE nombre = 'Historico');
 
    UPDATE en
    SET en.id_estado_vigencia = @estado_historico,
        en.fecha_fin_vigencia = CAST(SYSUTCDATETIME() AS DATE)
    FROM elemento_normativo en
    JOIN inserted i
      ON  en.id_reglamento   = i.id_reglamento
      AND en.padre_norm      = ISNULL(i.id_elemento_padre, 0)
      AND en.numero_etiqueta = i.numero_etiqueta
    WHERE en.id_estado_vigencia = @estado_vigente
      AND i.id_estado_vigencia  = @estado_vigente;
 
    INSERT INTO elemento_normativo
        (id_reglamento, id_elemento_padre, id_nivel_reglamento,
         numero_etiqueta, contenido_texto, orden,
         fecha_inicio_vigencia, fecha_fin_vigencia,
         id_estado_vigencia, id_acuerdo_origen)
    SELECT
        id_reglamento, id_elemento_padre, id_nivel_reglamento,
        numero_etiqueta, contenido_texto, orden,
        fecha_inicio_vigencia, fecha_fin_vigencia,
        id_estado_vigencia, id_acuerdo_origen
    FROM inserted;
END;
GO
 
 
/* 7.4 Trigger atomico de foliado  (Arash - Issue #1) */
GO
CREATE TRIGGER tg_folio_secuencial
ON certificacion_emitida
INSTEAD OF INSERT
AS
BEGIN
    SET NOCOUNT ON;
 
    DECLARE @anio    INT = YEAR(SYSUTCDATETIME());
    DECLARE @prefijo NVARCHAR(10) = 'DAIR';
 
    IF NOT EXISTS (SELECT 1 FROM control_folio WHERE anio = @anio AND prefijo = @prefijo)
        INSERT INTO control_folio (anio, prefijo, ultimo_numero) VALUES (@anio, @prefijo, 0);
 
    DECLARE @id_asambleista INT, @folio NVARCHAR(30), @hash NVARCHAR(80),
            @fecha DATETIME2, @usuario INT, @siguiente INT;
 
    DECLARE cur CURSOR LOCAL FAST_FORWARD FOR
        SELECT id_asambleista, folio_unico, hash_seguridad, fecha_emision, usuario_secretaria
        FROM inserted;
 
    OPEN cur;
    FETCH NEXT FROM cur INTO @id_asambleista, @folio, @hash, @fecha, @usuario;
 
    WHILE @@FETCH_STATUS = 0
    BEGIN
        IF @folio IS NULL
        BEGIN
            SELECT @siguiente = ultimo_numero + 1
            FROM control_folio WITH (UPDLOCK, HOLDLOCK)
            WHERE anio = @anio AND prefijo = @prefijo;
 
            UPDATE control_folio
            SET ultimo_numero = @siguiente,
                fecha_actualizacion = SYSUTCDATETIME()
            WHERE anio = @anio AND prefijo = @prefijo;
 
            SET @folio = CONCAT(@prefijo, '-',
                                RIGHT('000' + CAST(@siguiente AS NVARCHAR(10)), 3), '-',
                                CAST(@anio AS NVARCHAR(4)));
        END
 
        INSERT INTO certificacion_emitida
            (id_asambleista, folio_unico, hash_seguridad, fecha_emision, usuario_secretaria)
        VALUES
            (@id_asambleista, @folio, @hash, ISNULL(@fecha, SYSUTCDATETIME()), @usuario);
 
        FETCH NEXT FROM cur INTO @id_asambleista, @folio, @hash, @fecha, @usuario;
    END
 
    CLOSE cur;
    DEALLOCATE cur;
END;
GO
 
 
/* 7.5 Trigger de cambio de identidad  (Arash - Issue #14) */
GO
CREATE TRIGGER tg_cambio_identidad
ON asambleista
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
 
    DECLARE @razon NVARCHAR(200) =
        CAST(SESSION_CONTEXT(N'razon_cambio') AS NVARCHAR(200));
 
    INSERT INTO bitacora_asambleistas
        (id_asambleista, cedula_anterior, nombre_anterior, razon_cambio, fecha_actualizacion)
    SELECT
        d.id_asambleista,
        d.cedula,
        d.nombre,
        @razon,
        SYSUTCDATETIME()
    FROM deleted d
    JOIN inserted i ON d.id_asambleista = i.id_asambleista
    WHERE ISNULL(d.cedula, '') <> ISNULL(i.cedula, '')
       OR ISNULL(d.nombre, '') <> ISNULL(i.nombre, '');
END;
GO
 
 
--8. DATOS SEMILLA
 
--8.1 Seguridad: roles y permisos (Frank - Issue #0) 
INSERT INTO sys_rol (nombre_rol) VALUES
    ('Administrador'), ('Secretaria AIR'), ('Consulta'), ('Asambleista');
GO
 
INSERT INTO sys_permiso (nombre_permiso, descripcion) VALUES
    ('GESTIONAR_USUARIOS',      'Crear, editar y eliminar usuarios del sistema'),
    ('REGISTRAR_ASAMBLEISTAS',  'Crear y modificar el padron de asambleistas'),
    ('EMITIR_CERTIFICACION',    'Generar certificaciones legales con folio'),
    ('CONSULTAR_NORMATIVA',     'Visualizar reglamentos y articulado vigente');
GO
 
INSERT INTO sys_rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM sys_rol r
CROSS JOIN sys_permiso p
WHERE r.nombre_rol = 'Administrador';
 
INSERT INTO sys_rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM sys_rol r
JOIN sys_permiso p ON p.nombre_permiso IN
    ('REGISTRAR_ASAMBLEISTAS', 'EMITIR_CERTIFICACION', 'CONSULTAR_NORMATIVA')
WHERE r.nombre_rol = 'Secretaria AIR';
 
INSERT INTO sys_rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM sys_rol r
JOIN sys_permiso p ON p.nombre_permiso = 'CONSULTAR_NORMATIVA'
WHERE r.nombre_rol = 'Consulta';
 
INSERT INTO sys_rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM sys_rol r
JOIN sys_permiso p ON p.nombre_permiso = 'CONSULTAR_NORMATIVA'
WHERE r.nombre_rol = 'Asambleista';
GO
 
INSERT INTO sys_usuario (username, password_hash, email, activo) VALUES
    ('admin',
     '$2a$12$Dml3HUlko1YRJBX5u1hzp.c.IoHYbsoDK9f3q5Pu7bKsggek8N7lu',
     'admin@itcr.ac.cr',
     1);
GO
 
INSERT INTO sys_usuario_rol (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM sys_usuario u, sys_rol r
WHERE u.username = 'admin' AND r.nombre_rol = 'Administrador';
GO
 
INSERT INTO sys_usuario (username, password_hash, email, activo) VALUES
    ('secretaria',
     '$2a$12$spaKNFen5SO6F9Bf2FZp8eTpGC6bMrAgzbyf3tIhpyfuoVgpLuZla',
     'secretaria@itcr.ac.cr', 1);
INSERT INTO sys_usuario (username, password_hash, email, activo) VALUES
    ('consulta',
     '$2a$12$0Z6P26oC9KM4UqdOdrccUe7BuXvfiSEvGg.GbhWroe7zlJqSawzrC',
     'consulta@itcr.ac.cr', 1);
GO
 
INSERT INTO sys_usuario (username, password_hash, email, activo) VALUES
    ('asambleista',
     '$2a$12$gUY3AuY5Mdr0szeI7CsUFuAGdxBSPFUye.zUweEQO5zaep4inMnYK',
     'asambleista@estudiantec.cr', 1);
    
INSERT INTO sys_usuario_rol (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM sys_usuario u, sys_rol r
WHERE u.username = 'secretaria' AND r.nombre_rol = 'Secretaria AIR';
 
INSERT INTO sys_usuario_rol (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM sys_usuario u, sys_rol r
WHERE u.username = 'consulta' AND r.nombre_rol = 'Consulta';
GO
 
INSERT INTO sys_usuario_rol (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM sys_usuario u, sys_rol r
WHERE u.username = 'asambleista' AND r.nombre_rol = 'Asambleista';
 
--8.2 Catalogo Maestro (Arash - Issue #1) 
INSERT INTO catalogo_maestro (grupo_catalogo, nombre) VALUES
    ('SECTOR', 'Docente'),
    ('SECTOR', 'Estudiantil'),
    ('SECTOR', 'Administrativo'),
    ('SECTOR', 'Oficio (Consejo Institucional)'),
 
    ('PUESTO', 'Asambleista'),
    ('PUESTO', 'Presidente Directorio'),
    ('PUESTO', 'Secretaria AIR'),
    ('PUESTO', 'Asistente'),
 
    ('TIPO_SESION', 'Ordinaria'),
    ('TIPO_SESION', 'Extraordinaria'),
 
    ('TIPO_MODALIDAD', 'Presencial'),
    ('TIPO_MODALIDAD', 'Virtual'),
    ('TIPO_MODALIDAD', 'Mixta'),
 
    ('ETAPA_PROPUESTA', 'Procedencia'),
    ('ETAPA_PROPUESTA', 'Aprobacion'),
 
    ('ESTADO_PROPUESTA', 'Pendiente de Revision'),
    ('ESTADO_PROPUESTA', 'En Discusion'),
    ('ESTADO_PROPUESTA', 'Aprobada'),
    ('ESTADO_PROPUESTA', 'Rechazada'),
 
    ('TIPO_MAYORIA', 'Simple 50 mas 1'),
    ('TIPO_MAYORIA', 'Calificada 2 tercios'),
 
    ('TIPO_REFORMA', 'Modificacion'),
    ('TIPO_REFORMA', 'Derogacion'),
    ('TIPO_REFORMA', 'Adicion'),
    ('TIPO_REFORMA', 'Texto Sustitutivo'),
 
    ('TIPO_TRAMITE', 'Informe'),
    ('TIPO_TRAMITE', 'Mocion'),
    ('TIPO_TRAMITE', 'Varios'),
 
    ('ROL_COMISION', 'Coordinador'),
    ('ROL_COMISION', 'Integrante'),
    ('ROL_COMISION', 'Secretario Comision'),
 
    ('TIPO_COMISION', 'Permanente'),
    ('TIPO_COMISION', 'Especial'),
 
    ('ESTADO_ASISTENCIA', 'Presente'),
    ('ESTADO_ASISTENCIA', 'Ausente'),
    ('ESTADO_ASISTENCIA', 'Justificado');
GO
 
 
/* 8.3 Catalogos de normativa (Josue - Issue #10) */
INSERT INTO catalogo_estado_vigencia (nombre) VALUES
    ('Vigente'), ('Historico'), ('Derogado');
GO
 
INSERT INTO catalogo_nivel_reglamento (nombre, orden) VALUES
    ('Titulo', 1), ('Capitulo', 2), ('Articulo', 3),
    ('Inciso', 4), ('Sub-inciso', 5);
GO
 
 
/* 8.4 Datos semilla del Estatuto Organico (Josue - Issue #10) */
INSERT INTO reglamento (nombre_normativa, sigla, emisor) VALUES
    ('Estatuto Organico del ITCR', 'EOITCR', 'AIR');
INSERT INTO reglamento (nombre_normativa, sigla, emisor) VALUES
    ('Reglamento de la AIR', 'RAIR', 'AIR');
 
DECLARE @id_reg INT = (SELECT id_reglamento FROM reglamento WHERE sigla = 'EOITCR');
 
DECLARE @niv_titulo   INT = (SELECT id_nivel_reglamento FROM catalogo_nivel_reglamento WHERE nombre = 'Titulo');
DECLARE @niv_capitulo INT = (SELECT id_nivel_reglamento FROM catalogo_nivel_reglamento WHERE nombre = 'Capitulo');
DECLARE @niv_articulo INT = (SELECT id_nivel_reglamento FROM catalogo_nivel_reglamento WHERE nombre = 'Articulo');
DECLARE @niv_inciso   INT = (SELECT id_nivel_reglamento FROM catalogo_nivel_reglamento WHERE nombre = 'Inciso');
DECLARE @est_vigente  INT = (SELECT id_estado_vigencia  FROM catalogo_estado_vigencia  WHERE nombre = 'Vigente');
 
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, NULL, @niv_titulo, 'II', 'De la Estructura Organica', 2, @est_vigente);
DECLARE @id_titulo2 INT = (
    SELECT id_elemento FROM elemento_normativo
    WHERE id_reglamento = @id_reg AND id_elemento_padre IS NULL AND numero_etiqueta = 'II');
 
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, @id_titulo2, @niv_capitulo, 'I', 'De la Asamblea Institucional Representativa', 1, @est_vigente);
DECLARE @id_cap1 INT = (
    SELECT id_elemento FROM elemento_normativo
    WHERE id_reglamento = @id_reg AND id_elemento_padre = @id_titulo2 AND numero_etiqueta = 'I');
 
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, @id_cap1, @niv_articulo, '18',
     'La Asamblea Institucional Representativa es el organo de mayor jerarquia del Instituto.',
     1, @est_vigente);
DECLARE @id_art18 INT = (
    SELECT id_elemento FROM elemento_normativo
    WHERE id_reglamento = @id_reg AND id_elemento_padre = @id_cap1 AND numero_etiqueta = '18');
 
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, @id_art18, @niv_inciso, 'a)', 'Aprobar las reformas al Estatuto Organico.', 1, @est_vigente),
    (@id_reg, @id_art18, @niv_inciso, 'b)', 'Conocer y resolver sobre los recursos de su competencia.', 2, @est_vigente),
    (@id_reg, @id_art18, @niv_inciso, 'c)', 'Las demas que le asigne el Estatuto Organico.', 3, @est_vigente);
 
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, @id_cap1, @niv_articulo, '19',
     'La Asamblea Institucional Representativa estara constituida segun lo defina el reglamento.',
     2, @est_vigente);
 
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, NULL, @niv_titulo, 'III', 'De los Organos de Gobierno', 3, @est_vigente);
DECLARE @id_titulo3 INT = (
    SELECT id_elemento FROM elemento_normativo
    WHERE id_reglamento = @id_reg AND id_elemento_padre IS NULL AND numero_etiqueta = 'III');
 
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, @id_titulo3, @niv_capitulo, 'I', 'Del Consejo Institucional', 1, @est_vigente);
DECLARE @id_cap3_1 INT = (
    SELECT id_elemento FROM elemento_normativo
    WHERE id_reglamento = @id_reg AND id_elemento_padre = @id_titulo3 AND numero_etiqueta = 'I');
 
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, @id_cap3_1, @niv_articulo, '20',
     'El Consejo Institucional es el organo directivo superior del Instituto.', 1, @est_vigente),
    (@id_reg, @id_cap3_1, @niv_articulo, '21',
     'El Consejo Institucional estara integrado conforme al Estatuto Organico.', 2, @est_vigente);
GO
 
 
/* 8.5 Asambleistas de ejemplo (Frank - Issue #9) */
 
DECLARE @sector_docente INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='SECTOR' AND nombre='Docente');
DECLARE @sector_estud   INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='SECTOR' AND nombre='Estudiantil');
DECLARE @sector_admin   INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='SECTOR' AND nombre='Administrativo');
DECLARE @puesto_asamb   INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='PUESTO' AND nombre='Asambleista');
DECLARE @puesto_pres    INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='PUESTO' AND nombre='Presidente Directorio');
DECLARE @id_admin_user  INT = (SELECT id_usuario FROM sys_usuario WHERE username='admin');
 
INSERT INTO asambleista (cedula, nombre, correo_institucional) VALUES
    ('1-1111-1111', 'Ana Rosa Ruiz Fernandez',  'aruiz@itcr.ac.cr');
DECLARE @id_a1 INT = SCOPE_IDENTITY();
 
INSERT INTO asambleista (cedula, nombre, correo_institucional) VALUES
    ('2-2222-2222', 'Carlos Jimenez Mora',      'cjimenez@itcr.ac.cr');
DECLARE @id_a2 INT = SCOPE_IDENTITY();
 
INSERT INTO asambleista (cedula, nombre, correo_institucional) VALUES
    ('3-3333-3333', 'Maria Solano Vargas',      'msolano@estudiantec.cr');
DECLARE @id_a3 INT = SCOPE_IDENTITY();
 
INSERT INTO asambleista (cedula, nombre, correo_institucional) VALUES
    ('4-4444-4444', 'Jose Pablo Castro Leon',   'jcastro@itcr.ac.cr');
DECLARE @id_a4 INT = SCOPE_IDENTITY();
 
INSERT INTO asambleista (cedula, nombre, correo_institucional) VALUES
    ('5-5555-5555', 'Laura Mendez Quesada',     'lmendez@itcr.ac.cr');
DECLARE @id_a5 INT = SCOPE_IDENTITY();
 
INSERT INTO nombramiento (id_asambleista, id_sector, id_puesto, fecha_inicio, fecha_fin, estado, id_usuario_registro) VALUES
    (@id_a1, @sector_docente, @puesto_asamb, '2022-01-01', '2023-12-31', 'Inactivo', @id_admin_user),
    (@id_a1, @sector_docente, @puesto_pres,  '2024-01-01', NULL,         'Vigente',  @id_admin_user),
    (@id_a2, @sector_docente, @puesto_asamb, '2023-06-01', NULL,         'Vigente',  @id_admin_user),
    (@id_a3, @sector_estud,   @puesto_asamb, '2024-03-01', NULL,         'Vigente',  @id_admin_user),
    (@id_a4, @sector_admin,   @puesto_asamb, '2021-01-01', '2022-12-31', 'Inactivo', @id_admin_user),
    (@id_a5, @sector_docente, @puesto_asamb, '2024-01-15', NULL,         'Vigente',  @id_admin_user);
GO
 
 
-- ISSUE #11: CONTROL DE QUORUM
-- Autor: Frank
-- Sprint: 3
-- Descripcion: Registro de sesiones de la AIR con asistencia
-- por asambleista y validacion del quorum legal minimo.
 
-- 11.1  Estados de asistencia en catalogo_maestro
IF NOT EXISTS (
    SELECT 1 FROM catalogo_maestro
    WHERE grupo_catalogo = 'ESTADO_ASISTENCIA'
)
BEGIN
    INSERT INTO catalogo_maestro (grupo_catalogo, nombre, activo) VALUES
        ('ESTADO_ASISTENCIA', 'Presente',    1),
        ('ESTADO_ASISTENCIA', 'Ausente',     1),
        ('ESTADO_ASISTENCIA', 'Justificado', 1);
END;
GO
 
-- 11.2  Tipos de sesion en catalogo_maestro
IF NOT EXISTS (
    SELECT 1 FROM catalogo_maestro
    WHERE grupo_catalogo = 'TIPO_SESION'
)
BEGIN
    INSERT INTO catalogo_maestro (grupo_catalogo, nombre, activo) VALUES
        ('TIPO_SESION', 'Ordinaria',     1),
        ('TIPO_SESION', 'Extraordinaria',1);
END;
GO
 
-- 11.3  Tabla: sesion
CREATE TABLE sesion (
    id_sesion         INT IDENTITY(1,1) PRIMARY KEY,
    numero_sesion     NVARCHAR(30)  NOT NULL,
    fecha_sesion      DATETIME2     NOT NULL,
    id_tipo_sesion    INT           NOT NULL,
    quorum_requerido  INT           NOT NULL,
    total_convocados  INT           NOT NULL,
    cerrada           BIT           NOT NULL DEFAULT 0,
    fecha_creacion    DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_sesion_numero UNIQUE (numero_sesion),
    CONSTRAINT fk_sesion_tipo
        FOREIGN KEY (id_tipo_sesion) REFERENCES catalogo_maestro(id_item),
    CONSTRAINT ck_quorum_valido
        CHECK (quorum_requerido > 0 AND quorum_requerido <= total_convocados)
);
GO
 
-- 11.4  Tabla: asistencia_sesion_plenaria
CREATE TABLE asistencia_sesion_plenaria (
    id_asistencia       INT IDENTITY(1,1) PRIMARY KEY,
    id_sesion           INT NOT NULL,
    id_asambleista      INT NOT NULL,
    id_estado_asistencia INT NOT NULL,
    fecha_registro      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_asistencia_sesion_asambleista UNIQUE (id_sesion, id_asambleista),
    CONSTRAINT fk_asistencia_sesion
        FOREIGN KEY (id_sesion) REFERENCES sesion(id_sesion),
    CONSTRAINT fk_asistencia_asambleista
        FOREIGN KEY (id_asambleista) REFERENCES asambleista(id_asambleista),
    CONSTRAINT fk_asistencia_estado
        FOREIGN KEY (id_estado_asistencia) REFERENCES catalogo_maestro(id_item)
);
GO
 
-- 11.5  Indices para queries de quorum y reportes
CREATE INDEX idx_asistencia_sesion ON asistencia_sesion_plenaria(id_sesion);
CREATE INDEX idx_asistencia_asambleista ON asistencia_sesion_plenaria(id_asambleista);
GO
 
-- 11.6  FK PENDIENTES DE ISSUE #10 PARTE II
-- Ahora que la tabla 'sesion' existe, activamos las FK de las
-- tablas 'acta' y 'punto_agenda' (Josue - Issue #10 Parte II)
-- que apuntan a 'sesion'.
ALTER TABLE acta
    ADD CONSTRAINT fk_acta_sesion FOREIGN KEY (id_sesion) REFERENCES sesion(id_sesion);
GO
 
ALTER TABLE punto_agenda
    ADD CONSTRAINT fk_punto_sesion FOREIGN KEY (id_sesion) REFERENCES sesion(id_sesion);
GO
 
-- FIN ISSUE #11
 
 
-- ISSUE #13: BITACORA DE AUDITORIA Y TRAZABILIDAD DE EMISIONES
-- Autor: Frank
-- Sprint: 3
 
-- 13.1  Tipos de accion en catalogo_maestro
IF NOT EXISTS (
    SELECT 1 FROM catalogo_maestro
    WHERE grupo_catalogo = 'ACCION_LOG_CERT'
)
BEGIN
    INSERT INTO catalogo_maestro (grupo_catalogo, nombre, activo) VALUES
        ('ACCION_LOG_CERT', 'EMISION',     1),
        ('ACCION_LOG_CERT', 'REIMPRESION', 1),
        ('ACCION_LOG_CERT', 'ANULACION',   1),
        ('ACCION_LOG_CERT', 'SUSTITUCION', 1),
        ('ACCION_LOG_CERT', 'CONSULTA',    1);
END;
GO
 
-- 13.2  Tabla: log_certificacion_emitida
CREATE TABLE log_certificacion_emitida (
    id_log             INT IDENTITY(1,1) PRIMARY KEY,
    id_certificacion   INT NOT NULL,
    folio_unico        NVARCHAR(30) NOT NULL,
    id_accion          INT NOT NULL,
    id_usuario         INT NOT NULL,
    fecha_evento       DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ip_origen          NVARCHAR(45) NULL,
    hash_documento     NVARCHAR(80) NULL,
    snapshot_json      NVARCHAR(MAX) NULL,
    observacion        NVARCHAR(500) NULL,
    CONSTRAINT fk_log_cert_certificacion
        FOREIGN KEY (id_certificacion) REFERENCES certificacion_emitida(id_certificacion),
    CONSTRAINT fk_log_cert_accion
        FOREIGN KEY (id_accion) REFERENCES catalogo_maestro(id_item),
    CONSTRAINT fk_log_cert_usuario
        FOREIGN KEY (id_usuario) REFERENCES sys_usuario(id_usuario)
);
GO
 
-- 13.3  Tabla: seguridad_log
CREATE TABLE seguridad_log (
    id_seguridad_log INT IDENTITY(1,1) PRIMARY KEY,
    id_usuario       INT NOT NULL,
    accion           NVARCHAR(80) NOT NULL,
    tabla_consultada NVARCHAR(80) NULL,
    registro_id      INT NULL,
    ip_origen        NVARCHAR(45) NULL,
    fecha_evento     DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    detalle          NVARCHAR(500) NULL,
    CONSTRAINT fk_seguridad_log_usuario
        FOREIGN KEY (id_usuario) REFERENCES sys_usuario(id_usuario)
);
GO
 
-- 13.4  Indices para queries de auditoria
CREATE INDEX idx_log_cert_certificacion ON log_certificacion_emitida(id_certificacion);
CREATE INDEX idx_log_cert_folio ON log_certificacion_emitida(folio_unico);
CREATE INDEX idx_log_cert_fecha ON log_certificacion_emitida(fecha_evento);
CREATE INDEX idx_seguridad_log_usuario ON seguridad_log(id_usuario);
CREATE INDEX idx_seguridad_log_fecha ON seguridad_log(fecha_evento);
GO
 
-- FIN ISSUE #13
 
 
-- ISSUE #12: MOTOR DE VOTACIONES
-- Autor: Frank
-- Sprint: 3
 
-- 12.1  Tipos de mayoria en catalogo_maestro
IF NOT EXISTS (
    SELECT 1 FROM catalogo_maestro
    WHERE grupo_catalogo = 'TIPO_MAYORIA'
)
BEGIN
    INSERT INTO catalogo_maestro (grupo_catalogo, nombre, activo) VALUES
        ('TIPO_MAYORIA', 'Simple',     1),
        ('TIPO_MAYORIA', 'Calificada', 1);
END;
GO
 
-- 12.2  Tipos de voto en catalogo_maestro
IF NOT EXISTS (
    SELECT 1 FROM catalogo_maestro
    WHERE grupo_catalogo = 'TIPO_VOTO'
)
BEGIN
    INSERT INTO catalogo_maestro (grupo_catalogo, nombre, activo) VALUES
        ('TIPO_VOTO', 'Nominal', 1),
        ('TIPO_VOTO', 'Secreto', 1);
END;
GO
 
-- 12.3  Sentido del voto en catalogo_maestro
IF NOT EXISTS (
    SELECT 1 FROM catalogo_maestro
    WHERE grupo_catalogo = 'SENTIDO_VOTO'
)
BEGIN
    INSERT INTO catalogo_maestro (grupo_catalogo, nombre, activo) VALUES
        ('SENTIDO_VOTO', 'A favor',    1),
        ('SENTIDO_VOTO', 'En contra',  1),
        ('SENTIDO_VOTO', 'Abstencion', 1);
END;
GO
 
-- 12.4  Estado de votacion en catalogo_maestro
IF NOT EXISTS (
    SELECT 1 FROM catalogo_maestro
    WHERE grupo_catalogo = 'ESTADO_VOTACION'
)
BEGIN
    INSERT INTO catalogo_maestro (grupo_catalogo, nombre, activo) VALUES
        ('ESTADO_VOTACION', 'Abierta',    1),
        ('ESTADO_VOTACION', 'Cerrada',    1),
        ('ESTADO_VOTACION', 'Aprobada',   1),
        ('ESTADO_VOTACION', 'Rechazada',  1);
END;
GO
 
-- 12.5  Tabla: votacion
CREATE TABLE votacion (
    id_votacion        INT IDENTITY(1,1) PRIMARY KEY,
    id_sesion          INT NOT NULL,
    titulo             NVARCHAR(300) NOT NULL,
    descripcion        NVARCHAR(MAX) NULL,
    id_tipo_voto       INT NOT NULL,
    id_tipo_mayoria    INT NOT NULL,
    id_estado_votacion INT NOT NULL,
    total_presentes    INT NULL,
    votos_favor        INT NOT NULL DEFAULT 0,
    votos_contra       INT NOT NULL DEFAULT 0,
    votos_abstencion   INT NOT NULL DEFAULT 0,
    fecha_apertura     DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    fecha_cierre       DATETIME2 NULL,
    CONSTRAINT fk_votacion_sesion
        FOREIGN KEY (id_sesion) REFERENCES sesion(id_sesion),
    CONSTRAINT fk_votacion_tipo_voto
        FOREIGN KEY (id_tipo_voto) REFERENCES catalogo_maestro(id_item),
    CONSTRAINT fk_votacion_tipo_mayoria
        FOREIGN KEY (id_tipo_mayoria) REFERENCES catalogo_maestro(id_item),
    CONSTRAINT fk_votacion_estado
        FOREIGN KEY (id_estado_votacion) REFERENCES catalogo_maestro(id_item),
    CONSTRAINT ck_conteo_no_negativo
        CHECK (votos_favor >= 0 AND votos_contra >= 0 AND votos_abstencion >= 0)
);
GO
 
-- 12.6  Tabla: voto
CREATE TABLE voto (
    id_voto         INT IDENTITY(1,1) PRIMARY KEY,
    id_votacion     INT NOT NULL,
    id_asambleista  INT NULL,
    id_sentido_voto INT NOT NULL,
    fecha_voto      DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_voto_votacion
        FOREIGN KEY (id_votacion) REFERENCES votacion(id_votacion),
    CONSTRAINT fk_voto_asambleista
        FOREIGN KEY (id_asambleista) REFERENCES asambleista(id_asambleista),
    CONSTRAINT fk_voto_sentido
        FOREIGN KEY (id_sentido_voto) REFERENCES catalogo_maestro(id_item)
);
GO
 
CREATE UNIQUE INDEX uq_voto_nominal_unico
ON voto(id_votacion, id_asambleista)
WHERE id_asambleista IS NOT NULL;
GO
 
-- 12.7  Tabla: resolucion (motor de votaciones de Frank)
CREATE TABLE resolucion (
    id_resolucion     INT IDENTITY(1,1) PRIMARY KEY,
    id_votacion       INT NOT NULL,
    numero_resolucion NVARCHAR(50) NOT NULL,
    descripcion       NVARCHAR(MAX) NOT NULL,
    fecha_emision     DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    es_firme          BIT NOT NULL DEFAULT 0,
    CONSTRAINT uq_resolucion_numero UNIQUE (numero_resolucion),
    CONSTRAINT fk_resolucion_votacion
        FOREIGN KEY (id_votacion) REFERENCES votacion(id_votacion)
);
GO
 
-- 12.8  Indices para queries de motor de votaciones
CREATE INDEX idx_voto_votacion ON voto(id_votacion);
CREATE INDEX idx_votacion_sesion ON votacion(id_sesion);
CREATE INDEX idx_resolucion_votacion ON resolucion(id_votacion);
GO
 
-- FIN ISSUE #12
 
 
-- ISSUE #15: GESTION DE ANULACIONES Y SUSTITUCIONES
-- Autor: Arash
-- Sprint: 3
-- Descripcion: Extiende certificacion_emitida para soportar
-- anulaciones sin reutilizacion de folios y emision de
-- certificaciones de sustitucion con trazabilidad legal.

-- 15.1  Estados de certificacion en catalogo_maestro
IF NOT EXISTS (
    SELECT 1 FROM catalogo_maestro
    WHERE grupo_catalogo = 'ESTADO_CERTIFICACION'
)
BEGIN
    INSERT INTO catalogo_maestro (grupo_catalogo, nombre, activo) VALUES
        ('ESTADO_CERTIFICACION', 'Activa',     1),
        ('ESTADO_CERTIFICACION', 'Anulada',    1),
        ('ESTADO_CERTIFICACION', 'Sustituida', 1);
END;
GO
 
-- 15.2  Columnas nuevas en certificacion_emitida
ALTER TABLE certificacion_emitida
ADD
    id_estado                  INT           NULL,
    motivo_anulacion           NVARCHAR(500) NULL,
    fecha_anulacion            DATETIME2     NULL,
    usuario_anulacion          INT           NULL,
    id_certificacion_sustituye INT           NULL;
GO
 
-- 15.3  Inicializar certificaciones existentes como 'Activa'
UPDATE certificacion_emitida
SET id_estado = (
    SELECT id_item FROM catalogo_maestro
    WHERE grupo_catalogo = 'ESTADO_CERTIFICACION' AND nombre = 'Activa'
)
WHERE id_estado IS NULL;
GO
 
-- 15.4  Hacer id_estado obligatorio + agregar constraints
ALTER TABLE certificacion_emitida
ALTER COLUMN id_estado INT NOT NULL;
GO
 
ALTER TABLE certificacion_emitida
ADD CONSTRAINT fk_certificacion_estado
    FOREIGN KEY (id_estado) REFERENCES catalogo_maestro(id_item);
GO
 
ALTER TABLE certificacion_emitida
ADD CONSTRAINT fk_certificacion_sustituye
    FOREIGN KEY (id_certificacion_sustituye)
    REFERENCES certificacion_emitida(id_certificacion);
GO
 
ALTER TABLE certificacion_emitida
ADD CONSTRAINT fk_certificacion_usuario_anulacion
    FOREIGN KEY (usuario_anulacion) REFERENCES sys_usuario(id_usuario);
GO
 
-- 15.5 CHECK constraint: coherencia entre estado y campos
ALTER TABLE certificacion_emitida
ADD CONSTRAINT ck_anulacion_coherente
    CHECK (
        (
            id_estado = 37
            AND motivo_anulacion IS NULL
            AND fecha_anulacion IS NULL
            AND usuario_anulacion IS NULL
        )
        OR
        (
            id_estado IN (38, 39)
            AND motivo_anulacion IS NOT NULL
            AND fecha_anulacion IS NOT NULL
            AND usuario_anulacion IS NOT NULL
        )
    );
GO
 
-- 15.6  STORED PROCEDURE: Anular certificacion
CREATE OR ALTER PROCEDURE sp_anular_certificacion
    @folio_unico       NVARCHAR(30),
    @motivo            NVARCHAR(500),
    @usuario_anulacion INT
AS
BEGIN
    SET NOCOUNT ON;
 
    DECLARE @id_certificacion INT;
    DECLARE @id_estado_actual INT;
    DECLARE @id_estado_activa INT;
    DECLARE @id_estado_anulada INT;
 
    SELECT @id_estado_activa = id_item
    FROM catalogo_maestro
    WHERE grupo_catalogo = 'ESTADO_CERTIFICACION' AND nombre = 'Activa';
 
    SELECT @id_estado_anulada = id_item
    FROM catalogo_maestro
    WHERE grupo_catalogo = 'ESTADO_CERTIFICACION' AND nombre = 'Anulada';
 
    SELECT
        @id_certificacion = id_certificacion,
        @id_estado_actual = id_estado
    FROM certificacion_emitida
    WHERE folio_unico = @folio_unico;
 
    IF @id_certificacion IS NULL
    BEGIN
        RAISERROR('No existe una certificacion con el folio %s.', 16, 1, @folio_unico);
        RETURN;
    END;
 
    IF @id_estado_actual <> @id_estado_activa
    BEGIN
        RAISERROR('Solo se pueden anular certificaciones en estado Activa. Folio: %s', 16, 1, @folio_unico);
        RETURN;
    END;
 
    IF LTRIM(RTRIM(ISNULL(@motivo, ''))) = ''
    BEGIN
        RAISERROR('El motivo de anulacion es obligatorio.', 16, 1);
        RETURN;
    END;
 
    UPDATE certificacion_emitida
    SET
        id_estado         = @id_estado_anulada,
        motivo_anulacion  = @motivo,
        fecha_anulacion   = SYSUTCDATETIME(),
        usuario_anulacion = @usuario_anulacion
    WHERE id_certificacion = @id_certificacion;
 
    PRINT 'Certificacion ' + @folio_unico + ' anulada correctamente.';
END;
GO
 
 
-- 15.7  STORED PROCEDURE: Emitir certificacion de sustitucion
CREATE OR ALTER PROCEDURE sp_emitir_sustitucion
    @folio_anterior        NVARCHAR(30),
    @motivo                NVARCHAR(500),
    @usuario_secretaria    INT,
    @nuevo_folio           NVARCHAR(30) OUTPUT,
    @nuevo_id_certificacion INT         OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
 
    DECLARE @id_cert_anterior INT;
    DECLARE @id_asambleista   INT;
    DECLARE @hash_anterior    NVARCHAR(80);
    DECLARE @id_estado_activa INT;
    DECLARE @id_estado_sustituida INT;
 
    SELECT @id_estado_activa = id_item
    FROM catalogo_maestro
    WHERE grupo_catalogo = 'ESTADO_CERTIFICACION' AND nombre = 'Activa';
 
    SELECT @id_estado_sustituida = id_item
    FROM catalogo_maestro
    WHERE grupo_catalogo = 'ESTADO_CERTIFICACION' AND nombre = 'Sustituida';
 
    SELECT
        @id_cert_anterior = id_certificacion,
        @id_asambleista   = id_asambleista,
        @hash_anterior    = hash_seguridad
    FROM certificacion_emitida
    WHERE folio_unico = @folio_anterior;
 
    IF @id_cert_anterior IS NULL
    BEGIN
        RAISERROR('No existe una certificacion con el folio %s.', 16, 1, @folio_anterior);
        RETURN;
    END;
 
    IF NOT EXISTS (
        SELECT 1 FROM certificacion_emitida
        WHERE id_certificacion = @id_cert_anterior AND id_estado = @id_estado_activa
    )
    BEGIN
        RAISERROR('Solo se pueden sustituir certificaciones en estado Activa.', 16, 1);
        RETURN;
    END;
 
    BEGIN TRY
        BEGIN TRANSACTION;
 
        UPDATE certificacion_emitida
        SET
            id_estado         = @id_estado_sustituida,
            motivo_anulacion  = @motivo,
            fecha_anulacion   = SYSUTCDATETIME(),
            usuario_anulacion = @usuario_secretaria
        WHERE id_certificacion = @id_cert_anterior;
 
        INSERT INTO certificacion_emitida (
            id_asambleista,
            folio_unico,
            hash_seguridad,
            usuario_secretaria,
            id_estado,
            id_certificacion_sustituye
        )
        VALUES (
            @id_asambleista,
            'PENDIENTE',
            NULL,
            @usuario_secretaria,
            @id_estado_activa,
            @id_cert_anterior
        );
 
        SET @nuevo_id_certificacion = SCOPE_IDENTITY();
        SET @nuevo_folio = 'PENDIENTE';
 
        COMMIT TRANSACTION;
 
        PRINT 'Sustitucion registrada. Folio anterior: ' + @folio_anterior;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
 
 
-- Issue 15  TRIGGER: tg_no_repudio_cert (no repudio)
CREATE OR ALTER TRIGGER tg_no_repudio_cert
ON certificacion_emitida
AFTER UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
 
    IF EXISTS (SELECT 1 FROM deleted) AND NOT EXISTS (SELECT 1 FROM inserted)
    BEGIN
        RAISERROR('No se permite eliminar certificaciones emitidas. Utilice sp_anular_certificacion.', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END;
 
    IF UPDATE(folio_unico) OR UPDATE(hash_seguridad)
       OR UPDATE(id_asambleista) OR UPDATE(fecha_emision)
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM deleted d
            INNER JOIN inserted i ON d.id_certificacion = i.id_certificacion
            WHERE d.folio_unico = 'PENDIENTE' AND i.folio_unico <> 'PENDIENTE'
        )
        BEGIN
            RAISERROR('Los campos folio_unico, hash_seguridad, id_asambleista y fecha_emision son inmutables despues de la emision.', 16, 1);
            ROLLBACK TRANSACTION;
            RETURN;
        END;
    END;
END;
GO
 
-- FIN ISSUE #15
 
 
-- 8.6  DATOS SEMILLA - ISSUE #10 PARTE II
-- Responsable: Josue
-- Demuestra el flujo completo del proceso legislativo:
-- 1. Dos sesiones (ordinaria y extraordinaria)
-- 2. Una propuesta base + una conciliada (recursion)
-- 3. Dos proponentes (autoria multiple N:M)
-- 4. Un punto de agenda
-- 5. Una resolucion_propuesta oficial
-- 6. Una reforma_aplicada que apunta al Articulo 18
-- 7. Asistencia a la sesion ordinaria
 
DECLARE @ts_ordinaria      INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='TIPO_SESION'       AND nombre='Ordinaria');
DECLARE @ts_extraordinaria INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='TIPO_SESION'       AND nombre='Extraordinaria');
DECLARE @mod_presencial    INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='TIPO_MODALIDAD'    AND nombre='Presencial');
DECLARE @mod_virtual       INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='TIPO_MODALIDAD'    AND nombre='Virtual');
DECLARE @etapa_aprobacion  INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='ETAPA_PROPUESTA'   AND nombre='Aprobacion');
DECLARE @estado_aprobada   INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='ESTADO_PROPUESTA'  AND nombre='Aprobada');
DECLARE @estado_endisc     INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='ESTADO_PROPUESTA'  AND nombre='En Discusion');
DECLARE @mayoria_calif     INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='TIPO_MAYORIA'      AND nombre='Calificada 2 tercios');
DECLARE @tipo_modif        INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='TIPO_REFORMA'      AND nombre='Modificacion');
DECLARE @est_asist_pres    INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='ESTADO_ASISTENCIA' AND nombre='Presente');
DECLARE @est_asist_aus     INT = (SELECT id_item FROM catalogo_maestro WHERE grupo_catalogo='ESTADO_ASISTENCIA' AND nombre='Ausente');
 
DECLARE @id_reg_eoitcr INT = (SELECT id_reglamento FROM reglamento WHERE sigla='EOITCR');
DECLARE @id_art_18 INT = (
    SELECT id_elemento FROM elemento_normativo
    WHERE id_reglamento=@id_reg_eoitcr AND numero_etiqueta='18' AND id_estado_vigencia=1);
 
DECLARE @asamb_ana    INT = (SELECT id_asambleista FROM asambleista WHERE cedula='1-1111-1111');
DECLARE @asamb_carlos INT = (SELECT id_asambleista FROM asambleista WHERE cedula='2-2222-2222');
DECLARE @asamb_maria  INT = (SELECT id_asambleista FROM asambleista WHERE cedula='3-3333-3333');
DECLARE @asamb_jose   INT = (SELECT id_asambleista FROM asambleista WHERE cedula='4-4444-4444');
DECLARE @asamb_laura  INT = (SELECT id_asambleista FROM asambleista WHERE cedula='5-5555-5555');
 
DECLARE @id_admin INT = (SELECT id_usuario FROM sys_usuario WHERE username='admin');
 
-- Sesion ordinaria (tabla 'sesion' de Frank, Issue #11)
INSERT INTO sesion (numero_sesion, fecha_sesion, id_tipo_sesion, quorum_requerido, total_convocados)
VALUES ('AIR-110-2024', '2024-09-25T09:00:00', @ts_ordinaria, 100, 150);
DECLARE @id_sesion1 INT = SCOPE_IDENTITY();
 
-- Sesion extraordinaria
INSERT INTO sesion (numero_sesion, fecha_sesion, id_tipo_sesion, quorum_requerido, total_convocados)
VALUES ('AIR-111-2024', '2024-10-15T09:00:00', @ts_extraordinaria, 100, 150);
 
-- Acta de la sesion ordinaria
INSERT INTO acta (id_sesion, id_tipo_modalidad, fecha_aprobacion, url_documento, link_acta, observaciones)
VALUES (@id_sesion1, @mod_presencial, '2024-10-15',
        'https://tec.cr/air/actas/AIR-110-2024.pdf',
        'https://tec.cr/air/actas/AIR-110-2024.pdf',
        'Acta aprobada en la sesion AIR-111-2024.');
 
-- Propuesta base
INSERT INTO propuesta (codigo_air, titulo, texto_sustitutivo, id_reglamento_base,
                       id_propuesta_padre, id_etapa_propuesta, id_estado_propuesta,
                       id_tipo_mayoria_requerida, link_documentacion)
VALUES ('AIR-99-2024',
        'Reforma al Articulo 18 del Estatuto Organico - Inclusion de criterios de equidad',
        'La Asamblea Institucional Representativa es el organo de mayor jerarquia del Instituto, con responsabilidad sobre la equidad institucional y la sostenibilidad academica.',
        @id_reg_eoitcr, NULL, @etapa_aprobacion, @estado_aprobada, @mayoria_calif,
        'https://tec.cr/air/propuestas/AIR-99-2024.pdf');
DECLARE @id_prop_base INT = SCOPE_IDENTITY();
 
-- Propuesta conciliada (recursividad)
INSERT INTO propuesta (codigo_air, titulo, texto_sustitutivo, id_reglamento_base,
                       id_propuesta_padre, id_etapa_propuesta, id_estado_propuesta,
                       id_tipo_mayoria_requerida, link_documentacion)
VALUES ('AIR-99-CONC-2024',
        'Conciliacion del AIR-99-2024',
        'Texto conciliado tras el analisis en comision.',
        @id_reg_eoitcr, @id_prop_base, @etapa_aprobacion, @estado_endisc, @mayoria_calif,
        'https://tec.cr/air/propuestas/AIR-99-CONC-2024.pdf');
 
-- Proponentes (autoria multiple N:M)
INSERT INTO proponente_propuesta (id_propuesta, id_asambleista) VALUES
    (@id_prop_base, @asamb_ana),
    (@id_prop_base, @asamb_carlos);
 
-- Bitacora del cambio de estado a 'Aprobada'
INSERT INTO bitacora_propuesta (id_propuesta, id_reglamento_base, id_etapa_propuesta,
                                id_estado_propuesta, titulo, codigo_air, usuario_modificacion)
VALUES (@id_prop_base, @id_reg_eoitcr, @etapa_aprobacion, @estado_aprobada,
        'Reforma al Articulo 18 del Estatuto Organico - Inclusion de criterios de equidad',
        'AIR-99-2024', @id_admin);
 
-- Punto de agenda
INSERT INTO punto_agenda (id_sesion, id_propuesta, orden, descripcion)
VALUES (@id_sesion1, @id_prop_base, 1, 'Discusion y votacion de la propuesta AIR-99-2024.');
DECLARE @id_punto1 INT = SCOPE_IDENTITY();
 
-- Resolucion oficial de la propuesta
INSERT INTO resolucion_propuesta (id_punto_agenda, numero_resolucion, fecha_emision)
VALUES (@id_punto1, 'AIR-RES-001-2024', '2024-09-25');
DECLARE @id_resprop1 INT = SCOPE_IDENTITY();
 
-- Reforma aplicada (conecta resolucion_propuesta con articulo 18)
INSERT INTO reforma_aplicada (id_resolucion_propuesta, id_elemento_normativo, id_tipo_reforma,
                              texto_anterior, texto_nuevo, fecha_inicio_vigencia)
VALUES (@id_resprop1, @id_art_18, @tipo_modif,
        'La Asamblea Institucional Representativa es el organo de mayor jerarquia del Instituto.',
        'La Asamblea Institucional Representativa es el organo de mayor jerarquia del Instituto, con responsabilidad sobre la equidad institucional y la sostenibilidad academica.',
        '2024-09-25');
 
-- Asistencia (tabla de Frank, Issue #11): 4 presentes, 1 ausente
INSERT INTO asistencia_sesion_plenaria (id_sesion, id_asambleista, id_estado_asistencia) VALUES
    (@id_sesion1, @asamb_ana,    @est_asist_pres),
    (@id_sesion1, @asamb_carlos, @est_asist_pres),
    (@id_sesion1, @asamb_maria,  @est_asist_pres),
    (@id_sesion1, @asamb_jose,   @est_asist_aus),
    (@id_sesion1, @asamb_laura,  @est_asist_pres);
GO
 
 
--FIN DEL SCRIPT proyecto-air.sql 
PRINT 'proyecto-air.sql ejecutado correctamente.';
GO


-- ============================================================
-- ISSUE #13: fn_generar_hash_sha256 - Funcion hash SHA-256
-- Consumida por Arash en el motor PDF (#17) para no repudio.
-- Uso: SELECT dbo.fn_generar_hash_sha256('texto')
-- ============================================================
CREATE OR ALTER FUNCTION dbo.fn_generar_hash_sha256
(
    @contenido NVARCHAR(MAX)
)
RETURNS NVARCHAR(64)
AS
BEGIN
    RETURN LOWER(CONVERT(NVARCHAR(64),
        HASHBYTES('SHA2_256', @contenido), 2));
END;
-- FIN ISSUE #13: fn_generar_hash_sha256

-- ============================================================
-- ISSUE #13: tg_auditoria_total - Triggers de auditoria sobre 6 tablas criticas
-- ============================================================
CREATE OR ALTER TRIGGER tg_auditoria_asambleista ON asambleista AFTER INSERT, UPDATE, DELETE AS BEGIN SET NOCOUNT ON; DECLARE @accion NVARCHAR(10); IF EXISTS (SELECT 1 FROM inserted) AND EXISTS (SELECT 1 FROM deleted) SET @accion = 'UPDATE'; ELSE IF EXISTS (SELECT 1 FROM inserted) SET @accion = 'INSERT'; ELSE SET @accion = 'DELETE'; DECLARE @id INT = COALESCE((SELECT TOP 1 id_asambleista FROM inserted),(SELECT TOP 1 id_asambleista FROM deleted)); INSERT INTO seguridad_log (id_usuario,accion,tabla_consultada,registro_id,ip_origen,fecha_evento,detalle) VALUES (TRY_CAST(SESSION_CONTEXT(N'usuario_id') AS INT),@accion,'asambleista',@id,'0.0.0.0',SYSUTCDATETIME(),'Auditoria automatica'); END;
GO
CREATE OR ALTER TRIGGER tg_auditoria_certificacion ON certificacion_emitida AFTER INSERT, UPDATE, DELETE AS BEGIN SET NOCOUNT ON; DECLARE @accion NVARCHAR(10); IF EXISTS (SELECT 1 FROM inserted) AND EXISTS (SELECT 1 FROM deleted) SET @accion = 'UPDATE'; ELSE IF EXISTS (SELECT 1 FROM inserted) SET @accion = 'INSERT'; ELSE SET @accion = 'DELETE'; DECLARE @id INT = COALESCE((SELECT TOP 1 id_certificacion FROM inserted),(SELECT TOP 1 id_certificacion FROM deleted)); INSERT INTO seguridad_log (id_usuario,accion,tabla_consultada,registro_id,ip_origen,fecha_evento,detalle) VALUES (TRY_CAST(SESSION_CONTEXT(N'usuario_id') AS INT),@accion,'certificacion_emitida',@id,'0.0.0.0',SYSUTCDATETIME(),'Auditoria automatica'); END;
GO
CREATE OR ALTER TRIGGER tg_auditoria_resolucion ON resolucion AFTER INSERT, UPDATE, DELETE AS BEGIN SET NOCOUNT ON; DECLARE @accion NVARCHAR(10); IF EXISTS (SELECT 1 FROM inserted) AND EXISTS (SELECT 1 FROM deleted) SET @accion = 'UPDATE'; ELSE IF EXISTS (SELECT 1 FROM inserted) SET @accion = 'INSERT'; ELSE SET @accion = 'DELETE'; DECLARE @id INT = COALESCE((SELECT TOP 1 id_resolucion FROM inserted),(SELECT TOP 1 id_resolucion FROM deleted)); INSERT INTO seguridad_log (id_usuario,accion,tabla_consultada,registro_id,ip_origen,fecha_evento,detalle) VALUES (TRY_CAST(SESSION_CONTEXT(N'usuario_id') AS INT),@accion,'resolucion',@id,'0.0.0.0',SYSUTCDATETIME(),'Auditoria automatica'); END;
GO
CREATE OR ALTER TRIGGER tg_auditoria_sesion ON sesion AFTER INSERT, UPDATE, DELETE AS BEGIN SET NOCOUNT ON; DECLARE @accion NVARCHAR(10); IF EXISTS (SELECT 1 FROM inserted) AND EXISTS (SELECT 1 FROM deleted) SET @accion = 'UPDATE'; ELSE IF EXISTS (SELECT 1 FROM inserted) SET @accion = 'INSERT'; ELSE SET @accion = 'DELETE'; DECLARE @id INT = COALESCE((SELECT TOP 1 id_sesion FROM inserted),(SELECT TOP 1 id_sesion FROM deleted)); INSERT INTO seguridad_log (id_usuario,accion,tabla_consultada,registro_id,ip_origen,fecha_evento,detalle) VALUES (TRY_CAST(SESSION_CONTEXT(N'usuario_id') AS INT),@accion,'sesion',@id,'0.0.0.0',SYSUTCDATETIME(),'Auditoria automatica'); END;
GO
CREATE OR ALTER TRIGGER tg_auditoria_voto ON voto AFTER INSERT, UPDATE, DELETE AS BEGIN SET NOCOUNT ON; DECLARE @accion NVARCHAR(10); IF EXISTS (SELECT 1 FROM inserted) AND EXISTS (SELECT 1 FROM deleted) SET @accion = 'UPDATE'; ELSE IF EXISTS (SELECT 1 FROM inserted) SET @accion = 'INSERT'; ELSE SET @accion = 'DELETE'; DECLARE @id INT = COALESCE((SELECT TOP 1 id_voto FROM inserted),(SELECT TOP 1 id_voto FROM deleted)); INSERT INTO seguridad_log (id_usuario,accion,tabla_consultada,registro_id,ip_origen,fecha_evento,detalle) VALUES (TRY_CAST(SESSION_CONTEXT(N'usuario_id') AS INT),@accion,'voto',@id,'0.0.0.0',SYSUTCDATETIME(),'Auditoria automatica'); END;
GO
CREATE OR ALTER TRIGGER tg_auditoria_votacion ON votacion AFTER INSERT, UPDATE, DELETE AS BEGIN SET NOCOUNT ON; DECLARE @accion NVARCHAR(10); IF EXISTS (SELECT 1 FROM inserted) AND EXISTS (SELECT 1 FROM deleted) SET @accion = 'UPDATE'; ELSE IF EXISTS (SELECT 1 FROM inserted) SET @accion = 'INSERT'; ELSE SET @accion = 'DELETE'; DECLARE @id INT = COALESCE((SELECT TOP 1 id_votacion FROM inserted),(SELECT TOP 1 id_votacion FROM deleted)); INSERT INTO seguridad_log (id_usuario,accion,tabla_consultada,registro_id,ip_origen,fecha_evento,detalle) VALUES (TRY_CAST(SESSION_CONTEXT(N'usuario_id') AS INT),@accion,'votacion',@id,'0.0.0.0',SYSUTCDATETIME(),'Auditoria automatica'); END;
-- FIN ISSUE #13: tg_auditoria_total
