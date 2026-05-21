-- Drop redundant duplicate indexes identified by a live-gemd
-- INFORMATION_SCHEMA audit. The live-gemd perf probe (PERF_PROBE_REPORT.md,
-- merged at 5a434c8698) flagged BIO_ASSAY_DIMENSIONS2BIO_ASSAYS as having
-- two identical non-unique BTREE indexes on BIO_ASSAYS_FK; a follow-up
-- sweep across all gemd tables surfaced a second instance on
-- RELEVANT_PUBLICATIONS.
--
-- Audit query (run against prod gemd, READ-ONLY):
--   SELECT table_name, col_list, non_unique,
--          GROUP_CONCAT(index_name ORDER BY index_name SEPARATOR '|') AS index_names,
--          COUNT(*) AS n
--   FROM (
--     SELECT table_schema, table_name, index_name, non_unique,
--            GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') AS col_list
--     FROM information_schema.statistics
--     WHERE table_schema='gemd'
--     GROUP BY table_schema, table_name, index_name, non_unique
--   ) idx
--   GROUP BY table_schema, table_name, col_list, non_unique
--   HAVING COUNT(*) > 1;
--
-- For each group: kept the index that shares its name with the
-- corresponding FOREIGN KEY CONSTRAINT (MySQL/InnoDB treats that index as
-- the FK's supporting index, so dropping it would either fail or force
-- InnoDB to silently auto-create a replacement). The dropped index is the
-- redundant auto-index left over from an earlier Hibernate mapping
-- iteration.
--
-- After the DROP the surviving FK-named index still covers BIO_ASSAYS_FK
-- (or OTHER_RELEVANT_PUBLICATIONS_FK), so query plans are unchanged. Net
-- effect on BIO_ASSAY_DIMENSIONS2BIO_ASSAYS (~988k rows, ~95 MB index
-- footprint) is roughly a halving of the per-row index surface for that
-- FK; on RELEVANT_PUBLICATIONS (67 rows) the saving is symbolic.

ALTER TABLE BIO_ASSAY_DIMENSIONS2BIO_ASSAYS DROP INDEX BIO_ASSAY_DIMENSION_BIO_ASSAYS_FKC;

ALTER TABLE RELEVANT_PUBLICATIONS DROP INDEX INVESTIGATION_OTHER_RELEVANT_PUBLICATIONS_FKC;
