/* =====================================================================
   Correcciones Sprint 3 - Modulo de Certificaciones (Issues #13 / #15)
   Ejecutar DESPUES de proyecto-air.sql. Idempotente.

     1) certificacion_emitida.id_estado sin default + trigger INSTEAD OF
        que no lo setea -> toda emision fallaba por NOT NULL.
     2) CHECK ck_anulacion_coherente usaba 37/38/39 (acciones de log) en
        vez de los estados reales 51/52/53 (ESTADO_CERTIFICACION).
     3) Permiso ANULAR_CERTIFICACION (requerido por AnulacionController)
        no existia en el seed ni estaba asignado a ningun rol.
   ===================================================================== */

/* 1) Estado por defecto "Activa" (51) para certificaciones nuevas. */
IF NOT EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_cert_estado')
    ALTER TABLE certificacion_emitida
        ADD CONSTRAINT DF_cert_estado DEFAULT 51 FOR id_estado;
GO

/* 2) Constraint de coherencia con los ids reales 51/52/53. */
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_anulacion_coherente')
    ALTER TABLE certificacion_emitida DROP CONSTRAINT ck_anulacion_coherente;
GO
ALTER TABLE certificacion_emitida ADD CONSTRAINT ck_anulacion_coherente CHECK (
    (id_estado = 51
        AND motivo_anulacion  IS NULL
        AND fecha_anulacion   IS NULL
        AND usuario_anulacion IS NULL)
    OR
    ((id_estado = 52 OR id_estado = 53)
        AND motivo_anulacion  IS NOT NULL
        AND fecha_anulacion   IS NOT NULL
        AND usuario_anulacion IS NOT NULL)
);
GO

/* 3) Permiso ANULAR_CERTIFICACION + asignacion a Administrador y Secretaria AIR. */
IF NOT EXISTS (SELECT 1 FROM sys_permiso WHERE nombre_permiso = 'ANULAR_CERTIFICACION')
    INSERT INTO sys_permiso (nombre_permiso, descripcion)
    VALUES ('ANULAR_CERTIFICACION', 'Permite anular certificaciones emitidas');
GO
INSERT INTO sys_rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM sys_rol r
CROSS JOIN sys_permiso p
WHERE p.nombre_permiso = 'ANULAR_CERTIFICACION'
  AND r.nombre_rol IN ('Administrador', 'Secretaria AIR')
  AND NOT EXISTS (
        SELECT 1 FROM sys_rol_permiso rp
        WHERE rp.id_rol = r.id_rol AND rp.id_permiso = p.id_permiso
  );
GO

PRINT 'Correcciones Sprint 3 aplicadas correctamente.';