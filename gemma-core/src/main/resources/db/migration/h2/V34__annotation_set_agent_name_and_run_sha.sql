-- H2 twin of mysql/V33__annotation_set_agent_name_and_run_sha.sql -- see that file for why these
-- two columns were missing from the migration path while every hbm2ddl-built database had them.
--
-- H2 supports ADD COLUMN IF NOT EXISTS, so the idempotency the MySQL side spells out with
-- information_schema and PREPARE is one keyword here.

ALTER TABLE ANNOTATION_SET ADD COLUMN IF NOT EXISTS RUN_SHA VARCHAR(255) NULL;
ALTER TABLE ANNOTATION_SET ADD COLUMN IF NOT EXISTS AGENT_NAME VARCHAR(255) NULL;
