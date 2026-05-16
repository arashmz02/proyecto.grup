
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

CREATE UNIQUE INDEX uq_etiqueta_vigente
    ON elemento_normativo (id_reglamento, padre_norm, numero_etiqueta)
    WHERE id_estado_vigencia = 1;   -- 1 = 'Vigente' (ver datos semilla)
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
