--
-- Phase 3 cleanup: drop orphaned Coexpression subsystem tables.
--
-- Phase 1c deleted the entire Coexpression / Link / NodeDegree / SupportDetails
-- subsystem from the Gemma Java code. The DB tables were intentionally left
-- behind in Phase 2 as a safety net. This migration removes them.
--
-- Audit (vs the prod baseline V1__prod_baseline.sql and Gemma 1.34.0 source):
--
--   Table                              Code refs (excl. db/migration)
--   ---------------------------------- ------------------------------
--   COEXP_CORRELATION_DISTRIBUTION     0  (only legacy db.0.0.6.sql)
--   COEXPRESSION_NODE_DEGREE           0  (only legacy db.0.0.8.sql)
--   GENE_COEX_GENES                    0  (only legacy db.0.0.8.sql)
--   GENE_COEX_TESTED_IN                0  (only legacy db.0.0.8.sql)
--   HUMAN_EXPERIMENT_COEXPRESSION      0  (only legacy db.0.0.8.sql)
--   HUMAN_GENE_COEXPRESSION            0  (only legacy db.0.0.8.sql)
--   HUMAN_LINK_SUPPORT_DETAILS         0  (only legacy db.0.0.8.sql)
--   MOUSE_EXPERIMENT_COEXPRESSION      0  (only legacy db.0.0.8.sql)
--   MOUSE_GENE_COEXPRESSION            0  (only legacy db.0.0.8.sql)
--   MOUSE_LINK_SUPPORT_DETAILS         0  (only legacy db.0.0.8.sql)
--   OTHER_EXPERIMENT_COEXPRESSION      0  (only legacy db.0.0.8.sql)
--   OTHER_GENE_COEXPRESSION            0  (only legacy db.0.0.8.sql)
--   OTHER_LINK_SUPPORT_DETAILS         0  (only legacy db.0.0.8.sql)
--   RAT_EXPERIMENT_COEXPRESSION        0  (only legacy db.0.0.8.sql)
--   RAT_GENE_COEXPRESSION              0  (only legacy db.0.0.8.sql)
--   RAT_LINK_SUPPORT_DETAILS           0  (only legacy db.0.0.8.sql)
--   USER_PROBE_CO_EXPRESSION           0  (only legacy db.0.0.5.sql)
--
-- The only "code reference" each table has is its own historical creation
-- migration; no Hibernate mapping, HQL, native query, entity class, or
-- controller still touches them. They are safe to drop.
--
-- NOT dropped here (still live and unrelated to the deleted Coexpression
-- subsystem):
--   * SAMPLE_COEXPRESSION_MATRIX -- per-sample QC correlation heatmap,
--     mapped by ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionMatrix
--     and referenced by SAMPLE_COEXPRESSION_MATRIX_(RAW|REG)_FK on ANALYSIS.
--
-- Also intentionally NOT dropped here (left for a separate ALTER TABLE pass
-- on ANALYSIS so this migration's blast radius stays "drop dead tables only"):
--   * ANALYSIS.COEXPRESSION_MATRIX            longblob       (unmapped)
--   * ANALYSIS.COEXP_CORRELATION_DISTRIBUTION_FK + FKF19622DC6BCD8439
--     (FK constraint blocks dropping COEXP_CORRELATION_DISTRIBUTION below;
--      handled in the first step of this script).
--
-- Production cutover: run this AFTER ops sign-off per FLYWAY_PROD_FOLLOWUP.md.
-- Do NOT execute against prod from a workstation. The CI/Flyway path is the
-- canonical applier.
--

-- ====================================================================
-- 1. Drop FK from ANALYSIS that pins COEXP_CORRELATION_DISTRIBUTION.
-- ====================================================================
-- The column itself (COEXP_CORRELATION_DISTRIBUTION_FK) is unmapped by
-- Hibernate; dropping the FK leaves the column as a dead bigint that a
-- future cleanup can ALTER TABLE ... DROP COLUMN. We keep it for now so
-- this migration only removes tables, not columns.
ALTER TABLE ANALYSIS DROP FOREIGN KEY FKF19622DC6BCD8439;

-- ====================================================================
-- 2. Drop the orphaned tables.
-- ====================================================================
-- Order matters only insofar as the *_GENE_COEXPRESSION tables FK into
-- *_LINK_SUPPORT_DETAILS; drop the gene-link tables first, then the
-- support-details tables. Everything else is independent.

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS HUMAN_EXPERIMENT_COEXPRESSION;
DROP TABLE IF EXISTS MOUSE_EXPERIMENT_COEXPRESSION;
DROP TABLE IF EXISTS RAT_EXPERIMENT_COEXPRESSION;
DROP TABLE IF EXISTS OTHER_EXPERIMENT_COEXPRESSION;

DROP TABLE IF EXISTS HUMAN_GENE_COEXPRESSION;
DROP TABLE IF EXISTS MOUSE_GENE_COEXPRESSION;
DROP TABLE IF EXISTS RAT_GENE_COEXPRESSION;
DROP TABLE IF EXISTS OTHER_GENE_COEXPRESSION;

DROP TABLE IF EXISTS HUMAN_LINK_SUPPORT_DETAILS;
DROP TABLE IF EXISTS MOUSE_LINK_SUPPORT_DETAILS;
DROP TABLE IF EXISTS RAT_LINK_SUPPORT_DETAILS;
DROP TABLE IF EXISTS OTHER_LINK_SUPPORT_DETAILS;

DROP TABLE IF EXISTS COEXPRESSION_NODE_DEGREE;
DROP TABLE IF EXISTS COEXP_CORRELATION_DISTRIBUTION;
DROP TABLE IF EXISTS GENE_COEX_GENES;
DROP TABLE IF EXISTS GENE_COEX_TESTED_IN;
DROP TABLE IF EXISTS USER_PROBE_CO_EXPRESSION;

SET FOREIGN_KEY_CHECKS = 1;
