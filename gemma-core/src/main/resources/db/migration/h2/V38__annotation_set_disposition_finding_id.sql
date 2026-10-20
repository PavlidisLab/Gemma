-- H2 mirror of mysql/V44__annotation_set_disposition_finding_id.sql; see that file for the
-- reasoning.
ALTER TABLE ANNOTATION_SET_DISPOSITION ADD COLUMN FINDING_ID VARCHAR(255) NULL;
