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

-- ============================================
-- TRIGGER PARA REGISTRAR CAMBIOS DE CEDULA
-- ============================================

CREATE OR REPLACE FUNCTION fn_bitacora_cedula_asambleista()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.cedula IS DISTINCT FROM NEW.cedula THEN
        INSERT INTO bitacora_cedula (
            tabla_origen,
            registro_id,
            cedula_anterior,
            cedula_nueva,
            motivo_cambio,
            id_usuario_cambio
        )
        VALUES (
            'asambleista',
            OLD.id_asambleista,
            OLD.cedula,
            NEW.cedula,
            'Cambio de cedula registrado',
            NULL
        );
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_bitacora_cedula_asambleista
AFTER UPDATE OF cedula ON asambleista
FOR EACH ROW
EXECUTE FUNCTION fn_bitacora_cedula_asambleista();

