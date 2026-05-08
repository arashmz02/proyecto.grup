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



--TRIGGER: Versionamiento Normativo

CREATE OR REPLACE FUNCTION fn_vigencia_normativa()
RETURNS TRIGGER AS $$
DECLARE
    estado_vigente_id INT;
    estado_historico_id INT;
BEGIN
--Obtén los IDs de los estados
    SELECT id_estado_vigencia INTO estado_vigente_id
    FROM catalogo_estado_vigencia WHERE nombre = 'Vigente';

    SELECT id_estado_vigencia INTO estado_historico_id
    FROM catalogo_estado_vigencia WHERE nombre = 'Historico';

--Si el nuevo elemento se inserta como VIGENTE
    IF NEW.id_estado_vigencia = estado_vigente_id THEN
--Busca si hay una versión anterior vigente
        UPDATE elemento_normativo
        SET id_estado_vigencia = estado_historico_id,
            fecha_fin_vigencia = CURRENT_DATE
        WHERE id_reglamento = NEW.id_reglamento
--Mismo padre (si el padre es NULL, ambos deben ser NULL)
          AND COALESCE(id_elemento_padre, 0) = COALESCE(NEW.id_elemento_padre, 0)
--Mismo número de etiqueta
          AND numero_etiqueta = NEW.numero_etiqueta
--Que esté vigente actualmente
          AND id_estado_vigencia = estado_vigente_id
--Que no sea el mismo elemento (para UPDATEs)
          AND id_elemento <> COALESCE(NEW.id_elemento, -1);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--Crear el trigger
CREATE TRIGGER tg_vigencia_normativa
BEFORE INSERT ON elemento_normativo
FOR EACH ROW EXECUTE FUNCTION fn_vigencia_normativa();