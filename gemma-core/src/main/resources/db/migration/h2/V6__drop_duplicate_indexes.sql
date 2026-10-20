-- H2 sibling of mysql/V4__drop_duplicate_indexes.sql. The redundant
-- indexes BIO_ASSAY_DIMENSION_BIO_ASSAYS_FKC and
-- INVESTIGATION_OTHER_RELEVANT_PUBLICATIONS_FKC do NOT exist in the
-- Hibernate-generated H2 baseline (V1__hibernate_baseline.sql only
-- declares the FK constraints, not their auto-companion indexes), so
-- these DROPs are no-ops here. IF EXISTS guards keep the migration
-- safe across baseline regenerations.

DROP INDEX IF EXISTS BIO_ASSAY_DIMENSION_BIO_ASSAYS_FKC;

DROP INDEX IF EXISTS INVESTIGATION_OTHER_RELEVANT_PUBLICATIONS_FKC;
