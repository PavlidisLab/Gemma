-- H2 mirror of mysql/V29__widen_bib_ref_annotation_term.sql; see that file for the reasoning.
ALTER TABLE BIB_REF_ANNOTATION
    ALTER COLUMN TERM SET DATA TYPE TEXT;
ALTER TABLE BIB_REF_ANNOTATION
    ALTER COLUMN TERM SET NOT NULL;
