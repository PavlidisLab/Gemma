-- H2 mirror of mysql/V27__annotation_relation_status.sql; see that file for the reasoning.
ALTER TABLE ANNOTATION_RELATION
    ADD COLUMN STATUS VARCHAR(16) NOT NULL DEFAULT 'ASSERTED';
