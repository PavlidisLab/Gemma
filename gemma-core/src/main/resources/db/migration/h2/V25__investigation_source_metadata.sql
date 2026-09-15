-- See db/migration/mysql/V24__investigation_source_metadata.sql for the canonical
-- description, including why this ADDS a column rather than renaming
-- PREBOARDED_IDENTIFYING_METADATA (production 1.32.x shares the database and maps that
-- column; INVESTIGATION is SINGLE_TABLE, so renaming it breaks the deployed 1.0 app on
-- any polymorphic Investigation query).
--
-- H2 takes the two statements separately. The version number differs from MySQL's because
-- the H2 and MySQL migration streams are keyed independently.
ALTER TABLE INVESTIGATION ADD COLUMN SOURCE_METADATA CLOB;

ALTER TABLE INVESTIGATION ADD COLUMN SOURCE_METADATA_SCHEMA_VERSION SMALLINT NULL;
