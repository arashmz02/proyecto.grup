-- ============================================
-- BITACORA DE CAMBIOS DE CEDULAS
-- ============================================

CREATE TABLE bitacora_cedula (
    id_bitacora_cedula  SERIAL PRIMARY KEY,
    tabla_origen        VARCHAR(60) NOT NULL,
    registro_id         INT NOT NULL,
    cedula_anterior     VARCHAR(20),
    cedula_nueva        VARCHAR(20),
    motivo_cambio       VARCHAR(200),
    id_usuario_cambio   INT,
    fecha_cambio        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);