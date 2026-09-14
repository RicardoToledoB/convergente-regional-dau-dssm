USE convergente_dau_dssm_db;

-- Índice recomendado para GET /api/patient/{rut}.
-- Ejecutar sólo si no existe y si en producción JPA_DDL_AUTO no está en update.
CREATE INDEX idx_atencion_run_dv
    ON dau_atenciones_consolidadas (run, dv);
