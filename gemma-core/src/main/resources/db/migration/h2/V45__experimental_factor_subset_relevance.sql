-- Curator/agent hint about whether a differential expression analysis should SUBSET by this factor.
-- H2 twin of mysql V52. See that file for why the advice belongs on the factor rather than on the
-- TGEMO_00022 SUBSET experiment tag.
--
-- 🛑 The two trees are separate and neither builds the other. mysql/V52 shipped without this twin and
-- every BaseDatabaseTest5 subclass broke on "Column SUBSET_RELEVANCE not found" -- the entity mapping
-- names a column Hibernate then selects on every read of the table, so it is not a partial failure.
-- Adding a column to an entity means BOTH trees, in the same commit.
--
-- TEXT is CLOB here, as in the V8 baseline-relevance twin.

ALTER TABLE EXPERIMENTAL_FACTOR
    ADD COLUMN SUBSET_RELEVANCE VARCHAR(32) NULL;
ALTER TABLE EXPERIMENTAL_FACTOR
    ADD COLUMN SUBSET_RELEVANCE_REASON CLOB NULL;
