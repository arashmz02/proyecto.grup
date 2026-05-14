/* 
   PROYECTO-AIR.SQL
   Sistema de Gestion Legislativa AIR (SGL-AIR)
   Instituto Tecnologico de Costa Rica - Bases de Datos
   Sprint 2: Cimientos y Estructura de Datos

   Motor: Azure SQL Database (T-SQL)
   Estrategia: DROP & RECREATE - el script deja la BD en un
   estado limpio y reproducible en cada ejecucion.

   ORDEN DEL SCRIPT (importante por dependencias de FK):
     1. DROP de objetos (orden inverso a la creacion)
     2. Modulo de Seguridad y Roles        (Frank  - Issue #0)
     3. Catalogo Maestro                   (Arash  - Issue #1)
     4. Modulo de Identidad y Nombramientos(Frank  - Issue #9)
     5. Modulo de Jerarquia Normativa      (Josue  - Issue #10)
     6. Control de Folios y Certificaciones(Arash  - Issue #1)
     7. Triggers
     8. Datos semilla */


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
   Responsable: Arash - Issue #1
   Se crea antes del modulo de identidad porque 'nombramiento'
   referencia este catalogo (id_sector, id_puesto). */

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
    id_sector           INT NOT NULL,   -- FK a catalogo_maestro (grupo 'SECTOR')
    id_puesto           INT,            -- FK a catalogo_maestro (grupo 'PUESTO')
    resolucion_id       INT,            -- FK opcional a resolucion (Sprint 3)
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
    numero_etiqueta       NVARCHAR(20)  NOT NULL,   -- ej: '18', 'a)', 'i.'
    contenido_texto       NVARCHAR(MAX) NOT NULL,
    orden                 INT NOT NULL,
    fecha_inicio_vigencia DATE NOT NULL DEFAULT CAST(SYSUTCDATETIME() AS DATE),
    fecha_fin_vigencia    DATE,
    id_estado_vigencia    INT NOT NULL,
    id_acuerdo_origen     INT,  -- FK opcional a resolucion (Sprint 3)
    CONSTRAINT fk_elemento_reglamento FOREIGN KEY (id_reglamento)       REFERENCES reglamento(id_reglamento),
    CONSTRAINT fk_elemento_padre      FOREIGN KEY (id_elemento_padre)   REFERENCES elemento_normativo(id_elemento),
    CONSTRAINT fk_elemento_nivel      FOREIGN KEY (id_nivel_reglamento) REFERENCES catalogo_nivel_reglamento(id_nivel_reglamento),
    CONSTRAINT fk_elemento_estado     FOREIGN KEY (id_estado_vigencia)  REFERENCES catalogo_estado_vigencia(id_estado_vigencia)
);
GO

/* REGLA DE ORO: Partial Unique Index (Filtered Index en T-SQL)
   Garantiza que no existan dos elementos hermanos marcados como
   'Vigente' con la misma etiqueta dentro del mismo reglamento.

   Nota de migracion PostgreSQL -> T-SQL:
   - En PostgreSQL se usaba COALESCE(id_elemento_padre, 0) dentro
     del indice y una subconsulta en el WHERE.
   - T-SQL NO permite funciones (COALESCE) ni subconsultas en la
     definicion de un filtered index. Solucion:
       a) El filtro usa el literal del id de estado 'Vigente'.
          Como los datos semilla insertan los estados en orden
          fijo (Vigente=1, Historico=2, Derogado=3), el id 1
          corresponde a 'Vigente'.
       b) Para tratar los elementos raiz (id_elemento_padre NULL)
          como hermanos entre si, se agrega la columna calculada
          persistida 'padre_norm' que convierte NULL en 0. */

ALTER TABLE elemento_normativo
    ADD padre_norm AS (ISNULL(id_elemento_padre, 0)) PERSISTED;
GO

CREATE UNIQUE INDEX uq_etiqueta_vigente
    ON elemento_normativo (id_reglamento, padre_norm, numero_etiqueta)
    WHERE id_estado_vigencia = 1;   -- 1 = 'Vigente' (ver datos semilla)
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

/* 7.1 Trigger generico de auditoria  (Frank - Issue #0)
   Registra INSERT / UPDATE / DELETE sobre asambleista y
   nombramiento en sys_log_auditoria. */
GO
CREATE TRIGGER tg_auditoria_asambleista
ON asambleista
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @id_usuario INT =
        TRY_CAST(CAST(SESSION_CONTEXT(N'usuario_id') AS NVARCHAR(20)) AS INT);

    -- INSERT o UPDATE: hay filas en INSERTED
    INSERT INTO sys_log_auditoria (id_usuario, accion, tabla_afectada, registro_id, detalle)
    SELECT
        @id_usuario,
        CASE WHEN EXISTS (SELECT 1 FROM deleted) THEN 'UPDATE' ELSE 'INSERT' END,
        'asambleista',
        i.id_asambleista,
        CONCAT('cedula=', i.cedula, '; nombre=', i.nombre,
               '; correo=', i.correo_institucional)
    FROM inserted i;

    -- DELETE: hay filas en DELETED pero no en INSERTED
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


/* 7.2 Trigger de traslape de nombramientos  (Frank - Issue #9)
   Impide registrar dos nombramientos del mismo asambleista cuyo
   rango de fechas se traslape. Se dispara en INSERT y UPDATE.

   Migracion: el trigger original era BEFORE ... FOR EACH ROW.
   En T-SQL no existe BEFORE; se usa un trigger AFTER que valida
   el conjunto recien insertado contra el resto y hace ROLLBACK
   si detecta traslape. */
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
            --dos rangos se traslapan si cada uno empieza antes
            --de que el otro termine (NULL = vigente = fecha maxima)
            i.fecha_inicio <= ISNULL(n.fecha_fin, '9999-12-31')
            AND n.fecha_inicio <= ISNULL(i.fecha_fin, '9999-12-31')
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50001, 'Traslape de nombramientos detectado para el asambleista.', 1;
    END
END;
GO


/* 7.3 Trigger de versionamiento normativo  (Josue - Issue #10)
   Al insertar un elemento marcado como 'Vigente', marca como
   'Historico' la version anterior con la misma etiqueta y mismo
   padre dentro del mismo reglamento, y le pone fecha_fin_vigencia.

   Migracion: el original era BEFORE INSERT FOR EACH ROW.

   IMPORTANTE - por que INSTEAD OF y no AFTER:
   El Partial Unique Index uq_etiqueta_vigente se valida DURANTE
   el INSERT. Un trigger AFTER correria DESPUES de esa validacion,
   asi que el indice rechazaria la reforma antes de que el trigger
   pudiera archivar la version anterior. La unica forma de que el
   versionamiento funcione es archivar la version vieja ANTES de
   insertar la nueva. Eso exige INSTEAD OF INSERT: el trigger
   toma el control, primero marca como Historico lo anterior, y
   recien entonces hace el INSERT real de las filas nuevas. */
GO
CREATE TRIGGER tg_vigencia_normativa
ON elemento_normativo
INSTEAD OF INSERT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @estado_vigente   INT = (SELECT id_estado_vigencia FROM catalogo_estado_vigencia WHERE nombre = 'Vigente');
    DECLARE @estado_historico INT = (SELECT id_estado_vigencia FROM catalogo_estado_vigencia WHERE nombre = 'Historico');

    --Paso 1: archivar las versiones vigentes anteriores que seran
    --reemplazadas por una fila nueva tambien marcada como Vigente.
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

    --Paso 2: insertar realmente las filas nuevas. Para entonces
    --el indice uq_etiqueta_vigente ya no encuentra conflicto.
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


/* 7.4 Trigger atomico de foliado  (Arash - Issue #1)
   Cuando se inserta una certificacion sin folio, genera el
   siguiente consecutivo del anio con formato DAIR-001-2026.

   Migracion / concurrencia:
   - PostgreSQL usaba SELECT ... FOR UPDATE para bloquear la
     fila del anio. En T-SQL el equivalente es leer la fila con
     los hints (UPDLOCK, HOLDLOCK), que bloquea esa fila (o el
     rango) hasta el fin de la transaccion del trigger,
     serializando los INSERT concurrentes.
   - Importante: este trigger soporta INSERT de varias filas.
     Recorre las certificaciones sin folio una por una con un
     cursor para asignar consecutivos correlativos. Para los
     volumenes de este proyecto el costo es despreciable y la
     correccion es prioritaria.*/
GO
CREATE TRIGGER tg_folio_secuencial
ON certificacion_emitida
INSTEAD OF INSERT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @anio    INT = YEAR(SYSUTCDATETIME());
    DECLARE @prefijo NVARCHAR(10) = 'DAIR';

    -- Asegura que exista la fila de control del anio actual
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
            --Bloqueo de la fila de control para serializar concurrencia
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


/* 7.5 Trigger de cambio de identidad  (Arash - Issue #14)
   Cualquier UPDATE que cambie cedula o nombre en asambleista
   genera automaticamente un registro en bitacora_asambleistas
   con los valores anteriores.

   Migracion: el original comparaba OLD vs NEW fila por fila.
   En T-SQL se hace JOIN entre DELETED (valores previos) e
   INSERTED (valores nuevos) por id_asambleista. */
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
    ('Administrador'), ('Secretaria AIR'), ('Consulta');
GO

INSERT INTO sys_permiso (nombre_permiso, descripcion) VALUES
    ('GESTIONAR_USUARIOS',      'Crear, editar y eliminar usuarios del sistema'),
    ('REGISTRAR_ASAMBLEISTAS',  'Crear y modificar el padron de asambleistas'),
    ('EMITIR_CERTIFICACION',    'Generar certificaciones legales con folio'),
    ('CONSULTAR_NORMATIVA',     'Visualizar reglamentos y articulado vigente');
GO

/* Asignacion rol -> permiso
   Administrador: todos. Secretaria AIR: registrar y certificar y
   consultar. Consulta: solo consultar normativa. */
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
GO

/* Usuario administrador inicial.
   IMPORTANTE: el hash de abajo es un placeholder. La aplicacion
   Java debe, en el primer arranque, reemplazarlo por un hash
   BCrypt real, o crear el usuario admin desde el codigo. NUNCA
   guardar contrasenas en texto plano.
   Hash de ejemplo correspondiente a la palabra 'cambiar123'
   generado con BCrypt (coste 10). Cambiarlo de inmediato. */
INSERT INTO sys_usuario (username, password_hash, email, activo) VALUES
    ('admin',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'admin@itcr.ac.cr',
     1);
GO

INSERT INTO sys_usuario_rol (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM sys_usuario u, sys_rol r
WHERE u.username = 'admin' AND r.nombre_rol = 'Administrador';
GO


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


/* 8.3 Catalogos de normativa (Josue - Issue #10) 
   El ORDEN de estos INSERT es critico: el filtered index
   uq_etiqueta_vigente depende de que 'Vigente' tenga id = 1.
   Por eso 'Vigente' se inserta primero. */
INSERT INTO catalogo_estado_vigencia (nombre) VALUES
    ('Vigente'), ('Historico'), ('Derogado');
GO

INSERT INTO catalogo_nivel_reglamento (nombre, orden) VALUES
    ('Titulo', 1), ('Capitulo', 2), ('Articulo', 3),
    ('Inciso', 4), ('Sub-inciso', 5);
GO


/* 8.4 Datos semilla del Estatuto Organico (Josue - Issue #10)
   Arbol de ejemplo.
   IMPORTANTE: la tabla elemento_normativo tiene un trigger
   INSTEAD OF INSERT, por lo que SCOPE_IDENTITY() NO funciona aqui
   (devolveria NULL: el INSERT real ocurre dentro del trigger, en
   otro scope). Por eso cada id se recupera con un SELECT por su
   clave natural: reglamento + etiqueta + padre. Es mas verboso
   pero es la forma correcta y robusta de sembrar el arbol. */

INSERT INTO reglamento (nombre_normativa, sigla, emisor) VALUES
    ('Estatuto Organico del ITCR', 'EOITCR', 'AIR');
INSERT INTO reglamento (nombre_normativa, sigla, emisor) VALUES
    ('Reglamento de la AIR', 'RAIR', 'AIR');

DECLARE @id_reg INT = (SELECT id_reglamento FROM reglamento WHERE sigla = 'EOITCR');

--ids de niveles y estado
DECLARE @niv_titulo   INT = (SELECT id_nivel_reglamento FROM catalogo_nivel_reglamento WHERE nombre = 'Titulo');
DECLARE @niv_capitulo INT = (SELECT id_nivel_reglamento FROM catalogo_nivel_reglamento WHERE nombre = 'Capitulo');
DECLARE @niv_articulo INT = (SELECT id_nivel_reglamento FROM catalogo_nivel_reglamento WHERE nombre = 'Articulo');
DECLARE @niv_inciso   INT = (SELECT id_nivel_reglamento FROM catalogo_nivel_reglamento WHERE nombre = 'Inciso');
DECLARE @est_vigente  INT = (SELECT id_estado_vigencia  FROM catalogo_estado_vigencia  WHERE nombre = 'Vigente');

--Titulo II (raiz, sin padre)
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, NULL, @niv_titulo, 'II', 'De la Estructura Organica', 2, @est_vigente);
DECLARE @id_titulo2 INT = (
    SELECT id_elemento FROM elemento_normativo
    WHERE id_reglamento = @id_reg AND id_elemento_padre IS NULL AND numero_etiqueta = 'II');

--Capitulo I dentro del Titulo II
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, @id_titulo2, @niv_capitulo, 'I', 'De la Asamblea Institucional Representativa', 1, @est_vigente);
DECLARE @id_cap1 INT = (
    SELECT id_elemento FROM elemento_normativo
    WHERE id_reglamento = @id_reg AND id_elemento_padre = @id_titulo2 AND numero_etiqueta = 'I');

--Articulo 18 dentro del Capitulo I
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, @id_cap1, @niv_articulo, '18',
     'La Asamblea Institucional Representativa es el organo de mayor jerarquia del Instituto.',
     1, @est_vigente);
DECLARE @id_art18 INT = (
    SELECT id_elemento FROM elemento_normativo
    WHERE id_reglamento = @id_reg AND id_elemento_padre = @id_cap1 AND numero_etiqueta = '18');

--Incisos del Articulo 18
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, @id_art18, @niv_inciso, 'a)', 'Aprobar las reformas al Estatuto Organico.', 1, @est_vigente),
    (@id_reg, @id_art18, @niv_inciso, 'b)', 'Conocer y resolver sobre los recursos de su competencia.', 2, @est_vigente),
    (@id_reg, @id_art18, @niv_inciso, 'c)', 'Las demas que le asigne el Estatuto Organico.', 3, @est_vigente);

--Articulo 19 dentro del Capitulo I
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, @id_cap1, @niv_articulo, '19',
     'La Asamblea Institucional Representativa estara constituida segun lo defina el reglamento.',
     2, @est_vigente);

--Titulo III (segundo titulo raiz, para cumplir el minimo del plan)
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, NULL, @niv_titulo, 'III', 'De los Organos de Gobierno', 3, @est_vigente);
DECLARE @id_titulo3 INT = (
    SELECT id_elemento FROM elemento_normativo
    WHERE id_reglamento = @id_reg AND id_elemento_padre IS NULL AND numero_etiqueta = 'III');

--Capitulo I dentro del Titulo III
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, @id_titulo3, @niv_capitulo, 'I', 'Del Consejo Institucional', 1, @est_vigente);
DECLARE @id_cap3_1 INT = (
    SELECT id_elemento FROM elemento_normativo
    WHERE id_reglamento = @id_reg AND id_elemento_padre = @id_titulo3 AND numero_etiqueta = 'I');

--Articulos dentro del Capitulo I del Titulo III
INSERT INTO elemento_normativo
    (id_reglamento, id_elemento_padre, id_nivel_reglamento, numero_etiqueta, contenido_texto, orden, id_estado_vigencia)
VALUES
    (@id_reg, @id_cap3_1, @niv_articulo, '20',
     'El Consejo Institucional es el organo directivo superior del Instituto.', 1, @est_vigente),
    (@id_reg, @id_cap3_1, @niv_articulo, '21',
     'El Consejo Institucional estara integrado conforme al Estatuto Organico.', 2, @est_vigente);
GO


/* 8.5 Asambleistas de ejemplo (Frank - Issue #9) 
   Cinco asambleistas con nombramientos diversos para la
   validacion final del Dia 10. Se capturan los ids con
   SCOPE_IDENTITY para enlazar los nombramientos. */

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

--Nombramientos (sin traslapes entre si para un mismo asambleista)
INSERT INTO nombramiento (id_asambleista, id_sector, id_puesto, fecha_inicio, fecha_fin, estado, id_usuario_registro) VALUES
    (@id_a1, @sector_docente, @puesto_asamb, '2022-01-01', '2023-12-31', 'Inactivo', @id_admin_user),
    (@id_a1, @sector_docente, @puesto_pres,  '2024-01-01', NULL,         'Vigente',  @id_admin_user),
    (@id_a2, @sector_docente, @puesto_asamb, '2023-06-01', NULL,         'Vigente',  @id_admin_user),
    (@id_a3, @sector_estud,   @puesto_asamb, '2024-03-01', NULL,         'Vigente',  @id_admin_user),
    (@id_a4, @sector_admin,   @puesto_asamb, '2021-01-01', '2022-12-31', 'Inactivo', @id_admin_user),
    (@id_a5, @sector_docente, @puesto_asamb, '2024-01-15', NULL,         'Vigente',  @id_admin_user);
GO

--FIN DEL SCRIPT proyecto-air.sql 
PRINT 'proyecto-air.sql ejecutado correctamente.';
GO
