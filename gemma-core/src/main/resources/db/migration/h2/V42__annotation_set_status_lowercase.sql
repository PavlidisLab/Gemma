-- See db/migration/mysql/V48__annotation_set_status_lowercase.sql for the canonical description.
-- The version numbers differ because the H2 and MySQL migration streams are keyed independently.
UPDATE ANNOTATION_SET SET STATUS = LOWER(STATUS) WHERE STATUS IS NOT NULL;
