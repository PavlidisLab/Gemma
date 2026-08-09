-- See db/migration/mysql/V23__investigation_source_metadata.sql for the canonical
-- description. H2 spells the rename differently (MySQL needs CHANGE COLUMN to stay
-- 5.7-compatible) and takes the two statements separately; the version number differs
-- because the H2 + MySQL migration streams are keyed independently.
ALTER TABLE INVESTIGATION ALTER COLUMN PREBOARDED_IDENTIFYING_METADATA RENAME TO SOURCE_METADATA;

ALTER TABLE INVESTIGATION ADD COLUMN SOURCE_METADATA_SCHEMA_VERSION SMALLINT NULL;
