-- See db/migration/mysql/V26__annotation_relation.sql for the canonical description: why
-- Gemma holds this knowledge in four places and can query it from none of them, why the
-- bases are ranked (an assertion beats an attestation), why corroboration is decided at read
-- rather than baked into a score, why the CORPUS basis is self-consuming, and why the table
-- is rebuilt rather than upserted.
--
-- The version number differs from MySQL's because the H2 and MySQL migration streams are
-- keyed independently.
--
-- `VALUE`, `LEVEL` and `OBJECT` are reserved words; the columns here are named so that no
-- quoting is needed on either engine, which is why they read SUBJECT_VALUE / OBJECT_VALUE
-- rather than the bare names EE2C uses.

CREATE TABLE ANNOTATION_RELATION (
    ID                                    BIGINT       NOT NULL AUTO_INCREMENT,
    SUBJECT_VALUE                         VARCHAR(255) NOT NULL,
    SUBJECT_VALUE_URI                     VARCHAR(255) NULL,
    SUBJECT_CATEGORY                      VARCHAR(255) NULL,
    SUBJECT_CATEGORY_URI                  VARCHAR(255) NULL,
    PREDICATE                             VARCHAR(255) NULL,
    PREDICATE_URI                         VARCHAR(255) NULL,
    OBJECT_VALUE                          VARCHAR(255) NOT NULL,
    OBJECT_VALUE_URI                      VARCHAR(255) NULL,
    OBJECT_CATEGORY                       VARCHAR(255) NULL,
    OBJECT_CATEGORY_URI                   VARCHAR(255) NULL,
    TAXON_FK                              BIGINT       NULL,
    BASIS                                 VARCHAR(16)  NOT NULL,
    SOURCE                                VARCHAR(64)  NULL,
    SOURCE_VERSION                        VARCHAR(64)  NULL,
    EVIDENCE_CODE                         VARCHAR(255) NULL,
    EXPRESSION_EXPERIMENT_FK              BIGINT       NULL,
    LEVEL                                 VARCHAR(255) NULL,
    ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK INT          NOT NULL DEFAULT 0,
    GENERATED_AT                          TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_ANNOTATION_RELATION_EE
        FOREIGN KEY (EXPRESSION_EXPERIMENT_FK) REFERENCES INVESTIGATION (ID)
        ON DELETE CASCADE,
    CONSTRAINT FK_ANNOTATION_RELATION_TAXON
        FOREIGN KEY (TAXON_FK) REFERENCES TAXON (ID)
);

CREATE INDEX IDX_ANNOTATION_RELATION_SUBJECT ON ANNOTATION_RELATION (SUBJECT_VALUE_URI, PREDICATE_URI);
CREATE INDEX IDX_ANNOTATION_RELATION_OBJECT ON ANNOTATION_RELATION (OBJECT_VALUE_URI, PREDICATE_URI);
CREATE INDEX IDX_ANNOTATION_RELATION_SUBJECT_VALUE ON ANNOTATION_RELATION (SUBJECT_VALUE);
CREATE INDEX IDX_ANNOTATION_RELATION_OBJECT_VALUE ON ANNOTATION_RELATION (OBJECT_VALUE);
CREATE INDEX IDX_ANNOTATION_RELATION_BASIS ON ANNOTATION_RELATION (BASIS, EXPRESSION_EXPERIMENT_FK);
CREATE INDEX IDX_ANNOTATION_RELATION_EE ON ANNOTATION_RELATION (EXPRESSION_EXPERIMENT_FK);
