-- H2 sibling of mysql/V5__raw_vector_ee_qt_index.sql.
-- See that file for motivation and EXPLAIN delta against live gemd.
-- IF NOT EXISTS keeps this safe across baseline regenerations.

CREATE INDEX IF NOT EXISTS experimentRawVectorByQt
    ON RAW_EXPRESSION_DATA_VECTOR (EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK);
