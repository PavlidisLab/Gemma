-- See db/migration/mysql/V46__factor_and_factor_value_supporting_evidence.sql for the
-- canonical description. H2 ADD COLUMN syntax is the same; the version number differs
-- because the H2 + MySQL migration streams are keyed independently.
ALTER TABLE EXPERIMENTAL_FACTOR
    ADD COLUMN SUPPORTING_EVIDENCE TEXT NULL;

ALTER TABLE FACTOR_VALUE
    ADD COLUMN SUPPORTING_EVIDENCE TEXT NULL;
