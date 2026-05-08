--MODULO DE JERARQUIA NORMATIVA

--Tabla 1: Los niveles que pueden existir
CREATE TABLE catalogo_nivel_reglamento (id_nivel_reglamento SERIAL PRIMARY KEY,nombre VARCHAR(40) UNIQUE NOT NULL,
orden INT NOT NULL);

--Llena la tabla con los niveles
INSERT INTO catalogo_nivel_reglamento (nombre, orden) VALUES
    ('Titulo', 1),
    ('Capitulo', 2),
    ('Articulo', 3),
    ('Inciso', 4),
    ('Sub-inciso', 5);


--Tabla 2: Los ESTADOS que puede tener un elemento
CREATE TABLE catalogo_estado_vigencia (id_estado_vigencia SERIAL PRIMARY KEY,nombre VARCHAR(20) UNIQUE NOT NULL);

--Llena la tabla con los estados
INSERT INTO catalogo_estado_vigencia (nombre) VALUES
    ('Vigente'),
    ('Historico'),
    ('Derogado');


--Tabla 3: El REGLAMENTO en general
CREATE TABLE reglamento (id_reglamento SERIAL PRIMARY KEY,nombre_normativa  VARCHAR(150) NOT NULL,
    sigla VARCHAR(20) UNIQUE NOT NULL,emisor VARCHAR(10) CHECK (emisor IN ('AIR', 'CI')));


--Tabla 4: LA MAS IMPORTANTE - Los ELEMENTOS que forman el árbol
--Un elemento puede ser: título, capítulo, artículo, inciso, etc.
CREATE TABLE elemento_normativo (id_elemento SERIAL PRIMARY KEY, id_reglamento INT NOT NULL REFERENCES reglamento(id_reglamento),
    id_elemento_padre INT REFERENCES elemento_normativo(id_elemento),
    id_nivel_reglamento INT NOT NULL REFERENCES catalogo_nivel_reglamento(id_nivel_reglamento), numero_etiqueta VARCHAR(20) NOT NULL,   
    contenido_texto TEXT NOT NULL,orden INT NOT NULL,fecha_inicio_vigencia DATE NOT NULL DEFAULT CURRENT_DATE, fecha_fin_vigencia DATE,                
    id_estado_vigencia INT NOT NULL REFERENCES catalogo_estado_vigencia(id_estado_vigencia),id_acuerdo_origen INT
);
--Nota: id_elemento_padre es NULL si es un Título (raíz del árbol), 



--Este es un índice que impide duplicados vigentes
--Si ya existe un "Artículo 18" vigente, no permite crear otro "Artículo 18" vigente
CREATE UNIQUE INDEX uq_etiqueta_vigente
ON elemento_normativo (COALESCE(id_elemento_padre, 0),id_reglamento, numero_etiqueta)
WHERE id_estado_vigencia = (SELECT id_estado_vigencia FROM catalogo_estado_vigencia WHERE nombre = 'Vigente');