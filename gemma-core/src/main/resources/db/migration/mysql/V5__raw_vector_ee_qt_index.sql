-- PERF_PROBE_ROUND3 hotspot A2: RAW_EXPRESSION_DATA_VECTOR is missing a
-- composite (EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK) index, so
-- RawExpressionDataVectorDaoImpl.findByExpressionExperimentAndQuantitationType
-- (Hibernate-generated `... where ee_fk = ? and qt_fk = ?`) falls back to
-- MySQL index_merge(intersect) across the two single-column indexes:
--
--   EXPLAIN ... WHERE EE_FK = 30438 AND QT_FK = 575609   -- BEFORE
--     type:          index_merge
--     key:           FK1F432A68D0CC06B4,RAW_EXPRESSION_DATA_VECTOR_EXPRESSION_EXPERIMENT_FKC
--     Extra:         Using intersect(FK1F432A68D0CC06B4,RAW_EXPRESSION_DATA_VECTOR_EXPRESSION_EXPERIMENT_FKC); Using where
--
-- index_merge intersect probes BOTH indexes and bitmap-ANDs the result --
-- twice the index work and (more importantly) prevents the optimizer from
-- using a single composite index for a key-ordered range scan, which is the
-- hot pattern for vector retrieval.
--
-- Naming follows the PROCESSED_EXPRESSION_DATA_VECTOR sibling, which already
-- has the matching composite at experimentProcessedVectorProbes(EE_FK, DE_FK).
-- We use experimentRawVectorByQt(EE_FK, QT_FK) -- DE_FK isn't part of the
-- hot WHERE on the raw side; QT_FK is what discriminates between the
-- per-platform raw vectors stored side-by-side on the same EE.
--
-- Live-gemd footprint at the time of probe: ~2.4B RAW rows, ~2.49M distinct
-- QT_FK values, ~2.35M distinct EE_FK rows-per-index-entry. Expected
-- post-index plan: type=ref, key=experimentRawVectorByQt, single-index lookup.

CREATE INDEX experimentRawVectorByQt
    ON RAW_EXPRESSION_DATA_VECTOR (EXPRESSION_EXPERIMENT_FK, QUANTITATION_TYPE_FK);
