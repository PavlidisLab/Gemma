-- Restore reverse compatibility for SINGLE_CELL_DIMENSION_EXPERIMENT (mysql/V7).
--
-- This branch runs against the same `gemd` database as the deployed production
-- Gemma (1.32.x) at the same time. That arrangement only holds if every schema
-- object this branch adds is invisible-but-harmless to the older code. V7
-- violated that: it created SINGLE_CELL_DIMENSION_EXPERIMENT with three plain
-- (RESTRICT) foreign keys, and production has no mapping for the table, so it
-- can neither see nor clean up the link rows. Any old-code delete of a parent
-- row is therefore rejected by InnoDB:
--
--   Cannot delete or update a parent row: a foreign key constraint fails
--   (`gemd`.`SINGLE_CELL_DIMENSION_EXPERIMENT`,
--    CONSTRAINT `FK_SCDE_QUANTITATION_TYPE` FOREIGN KEY (`QUANTITATION_TYPE_FK`)
--    REFERENCES `QUANTITATION_TYPE` (`ID`))
--
-- Observed on prod 2026-08-04 running `deleteExperiments -e GSE277430`: the
-- teardown got as far as removing the SC vectors and the SingleCellDimension,
-- then died deleting the QuantitationType and rolled the whole transaction
-- back. All three parent types (INVESTIGATION via EE deletion, QUANTITATION_TYPE,
-- SINGLE_CELL_DIMENSION) are routinely deleted by 1.32.x, so this blocked every
-- prod-side removal of single-cell data, not just whole-experiment deletion.
--
-- Fix: make the link rows cascade away with their parents, so the older code
-- does not need to know the table exists in order to delete through it. This is
-- a strict superset of what this branch already does — SingleCellDimensionExperimentDao
-- clears the rows explicitly first (removeByEE / removeByEEAndQt /
-- removeBySingleCellDimension, called from ExpressionExperimentDaoImpl) — so the
-- cascade is a no-op on this side and the DAO calls stay as the primary path.
-- The table is a denormalized cache of (EE, QT) -> SingleCellDimension derived
-- from SINGLE_CELL_EXPRESSION_DATA_VECTOR; losing a row when its parent goes
-- away is exactly the desired semantics, never a loss of source data.
--
-- Note the sibling table added by V20 already gets this right
-- (FK_ANNOTATION_SET_INVESTIGATION ... ON DELETE CASCADE); V7 is the outlier.
--
-- Caveat, deliberately accepted: SingleCellDimensionExperimentDao.countDistinctExperiments()
-- runs with setCacheable(true), and a database-level cascade bypasses Hibernate,
-- so that query-cache region is not invalidated when production deletes an
-- experiment. The home-page single-cell count can read stale until the region is
-- evicted. Cosmetic, and already true of every other change production makes to
-- this database.
--
-- This has to be a forward migration rather than an edit to V7: V7 is already
-- applied, so editing it would break Flyway checksum validation for anyone whose
-- history has it. (Prod `gemd` has no flyway_schema_history at all as of
-- 2026-08-04 — V7 was applied there by hand — so prod needs the two ALTERs below
-- run manually as well.)

ALTER TABLE SINGLE_CELL_DIMENSION_EXPERIMENT
    DROP FOREIGN KEY FK_SCDE_EXPRESSION_EXPERIMENT,
    DROP FOREIGN KEY FK_SCDE_QUANTITATION_TYPE,
    DROP FOREIGN KEY FK_SCDE_SINGLE_CELL_DIMENSION;

-- Dropping a foreign key in MySQL leaves its backing index in place, so the
-- re-added constraints reuse the existing indexes; no rebuild of the (528-row)
-- table happens here.
ALTER TABLE SINGLE_CELL_DIMENSION_EXPERIMENT
    ADD CONSTRAINT FK_SCDE_EXPRESSION_EXPERIMENT
        FOREIGN KEY (EXPRESSION_EXPERIMENT_FK) REFERENCES INVESTIGATION (ID)
        ON DELETE CASCADE,
    ADD CONSTRAINT FK_SCDE_QUANTITATION_TYPE
        FOREIGN KEY (QUANTITATION_TYPE_FK) REFERENCES QUANTITATION_TYPE (ID)
        ON DELETE CASCADE,
    ADD CONSTRAINT FK_SCDE_SINGLE_CELL_DIMENSION
        FOREIGN KEY (SINGLE_CELL_DIMENSION_FK) REFERENCES SINGLE_CELL_DIMENSION (ID)
        ON DELETE CASCADE;
