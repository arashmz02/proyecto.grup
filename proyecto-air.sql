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
