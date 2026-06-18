-- See db/migration/mysql/V22__tag_supporting_evidence.sql for the canonical
-- description. H2 ADD COLUMN syntax is the same; the version number differs
-- because the H2 + MySQL migration streams are keyed independently.
ALTER TABLE CHARACTERISTIC
    ADD COLUMN SUPPORTING_EVIDENCE TEXT NULL;
