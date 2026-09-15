-- See db/migration/mysql/V47__annotation_set_status_and_counts.sql for the canonical
-- description. H2 takes one ADD COLUMN per statement; the version numbers differ because the
-- H2 and MySQL migration streams are keyed independently.
ALTER TABLE ANNOTATION_SET
    ADD COLUMN STATUS VARCHAR(32) NULL;
ALTER TABLE ANNOTATION_SET
    ADD COLUMN FACTOR_COUNT INT NULL;
ALTER TABLE ANNOTATION_SET
    ADD COLUMN TAG_COUNT INT NULL;

UPDATE ANNOTATION_SET SET STATUS = 'PENDING' WHERE ROLE = 'PROPOSAL';
