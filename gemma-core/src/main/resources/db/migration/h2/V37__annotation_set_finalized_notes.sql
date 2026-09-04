-- H2 mirror of mysql/V43__annotation_set_finalized_notes.sql; see that file for the reasoning.
ALTER TABLE ANNOTATION_SET ADD COLUMN FINALIZED_NOTES VARCHAR(2048) NULL;
