-- ============================================
-- CATALOGO MAESTRO
-- ============================================

CREATE TABLE catalogo_maestro (
    id_catalogo        SERIAL PRIMARY KEY,
    tipo_catalogo      VARCHAR(50) NOT NULL,
    codigo             VARCHAR(30),
    nombre             VARCHAR(100) NOT NULL,
    descripcion        VARCHAR(200),
    activo             BOOLEAN DEFAULT TRUE
);

-- ============================================
-- DATOS SEMILLA CATALOGO
-- ============================================

INSERT INTO catalogo_maestro
(tipo_catalogo, codigo, nombre)
VALUES
('SECTOR', 'DOC', 'Docente'),
('SECTOR', 'ADM', 'Administrativo'),
('SECTOR', 'EST', 'Estudiantil'),

('PUESTO', 'PRES', 'Presidente'),
('PUESTO', 'SEC', 'Secretario'),
('PUESTO', 'VOC', 'Vocal'),

('ESTADO', 'VIG', 'Vigente'),
('ESTADO', 'INA', 'Inactivo');

-- ============================================
-- FOLIADO Y CONSECUTIVOS INSTITUCIONALES
-- ============================================

CREATE TABLE consecutivo_institucional (
    id_consecutivo      SERIAL PRIMARY KEY,
    tipo_documento      VARCHAR(30) NOT NULL,
    anio                INT NOT NULL,
    ultimo_numero       INT NOT NULL DEFAULT 0,
    prefijo             VARCHAR(20) NOT NULL DEFAULT 'DAIR',
    activo              BOOLEAN DEFAULT TRUE,
    CONSTRAINT uq_consecutivo_tipo_anio UNIQUE (tipo_documento, anio)
);

CREATE TABLE folio_documento (
    id_folio            SERIAL PRIMARY KEY,
    tipo_documento      VARCHAR(30) NOT NULL,
    anio                INT NOT NULL,
    numero              INT NOT NULL,
    folio               VARCHAR(30) UNIQUE NOT NULL,
    fecha_generacion    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_usuario_genero   INT,
    observacion         TEXT,
    CONSTRAINT uq_folio_tipo_anio_numero UNIQUE (tipo_documento, anio, numero)
);

