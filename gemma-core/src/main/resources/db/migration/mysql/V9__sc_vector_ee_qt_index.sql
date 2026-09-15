-- PERF_PROBE_ROUND4 finding: SINGLE_CELL_EXPRESSION_DATA_VECTOR is missing a
-- composite (EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK) index, so
-- SingleCellExpressionDataVectorDaoImpl.findByExpressionExperimentAndQuantitationType
-- (Hibernate-generated `... where ee_fk = ? and qt_fk = ?`) falls back to
-- MySQL index_merge(intersect) across the two single-column indexes -- same
-- shape as the RAW vector finding fixed in V5__raw_vector_ee_qt_index.sql.
--
-- Live gemd EXPLAIN (EE_FK=88732, QT_FK=619436, ~28k rows) BEFORE:
--     type:          index_merge
--     key:           SINGLE_CELL_EXPRESSION_DATA_VECTOR_QUANTITATION_TYPE_FKC,
--                    SINGLE_CELL_DATA_VECTOR_EXPRESSION_EXPERIMENT_FKC
--     Extra:         Using intersect(...); Using where
--
-- index_merge intersect probes BOTH indexes and bitmap-ANDs the result --
-- twice the index work and prevents the optimizer from using a single
-- composite index for a key-ordered range scan, which is the hot pattern
-- for vector retrieval.
--
-- Naming follows the RAW sibling experimentRawVectorByQt(EE_FK, QT_FK) and
-- the PROCESSED sibling experimentProcessedVectorProbes(EE_FK, DE_FK).
--
-- Live-gemd footprint at the time of probe: ~23.15M SCEDV rows, ~13.9k
-- distinct QT_FK values, ~14.1k distinct EE_FK index-entry rows. Expected
-- post-index plan: type=ref, key=experimentSingleCellVectorByQt, single-index
-- lookup.

CREATE INDEX experimentSingleCellVectorByQt
    ON SINGLE_CELL_EXPRESSION_DATA_VECTOR (EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK);
