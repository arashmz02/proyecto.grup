-- ============================================
-- TRAZABILIDAD DE ACCIONES DEL SISTEMA
-- ============================================

CREATE TABLE trazabilidad_accion (
    id_trazabilidad      SERIAL PRIMARY KEY,
    entidad_afectada     VARCHAR(60) NOT NULL,
    registro_id          INT NOT NULL,
    accion               VARCHAR(20) NOT NULL,
    valor_anterior       TEXT,
    valor_nuevo          TEXT,
    id_usuario           INT,
    fecha_accion         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_origen            VARCHAR(45)
);

-- ============================================
-- FUNCION GENERICA DE TRAZABILIDAD
-- ============================================

CREATE OR REPLACE FUNCTION fn_trazabilidad_general()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO trazabilidad_accion (
        entidad_afectada,
        registro_id,
        accion,
        valor_anterior,
        valor_nuevo,
        id_usuario
    )
    VALUES (
        TG_TABLE_NAME,
        COALESCE(NEW.id_asambleista, OLD.id_asambleista),
        TG_OP,
        row_to_json(OLD)::TEXT,
        row_to_json(NEW)::TEXT,
        NULL
    );

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- TRIGGERS DE TRAZABILIDAD
-- ============================================

CREATE TRIGGER tg_trazabilidad_asambleista
AFTER INSERT OR UPDATE OR DELETE ON asambleista
FOR EACH ROW
EXECUTE FUNCTION fn_trazabilidad_general();

CREATE TRIGGER tg_trazabilidad_nombramiento
AFTER INSERT OR UPDATE OR DELETE ON nombramiento
FOR EACH ROW
EXECUTE FUNCTION fn_trazabilidad_general();

