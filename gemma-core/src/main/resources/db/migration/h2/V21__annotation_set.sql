-- H2 sibling of mysql/V20__annotation_set.sql. See that file for the design
-- rationale.
--
-- Differences from the MySQL variant:
--   * H2 lacks the native JSON type; PAYLOAD_JSON is CLOB. Hibernate's
--     MaterializedClobType reads/writes the same String against either
--     column type, so the Java side is dialect-agnostic.
--   * H2 lacks the inline INDEX shorthand inside CREATE TABLE; uses
--     CREATE INDEX statements after.
--   * H2 unique key syntax uses CONSTRAINT ... UNIQUE (...) outside the
--     UNIQUE KEY shorthand.

CREATE TABLE ANNOTATION_SET (
    ID                  BIGINT       NOT NULL AUTO_INCREMENT,
    INVESTIGATION_FK    BIGINT       NOT NULL,
    ROLE                VARCHAR(32)  NOT NULL,
    SOURCE              VARCHAR(32)  NOT NULL,
    KIND                VARCHAR(32)  NULL,
    RUN_ID              VARCHAR(255) NOT NULL,
    PARENT_FK           BIGINT       NULL,
    CREATED_BY          VARCHAR(255) NULL,
    CREATED_AT          DATETIME(3)  NOT NULL,
    UPDATED_AT          DATETIME(3)  NOT NULL,
    FINALIZED_AT        DATETIME(3)  NULL,
    FINALIZED_BY        VARCHAR(255) NULL,
    AGENT_VERSION       VARCHAR(255) NULL,
    MODEL               VARCHAR(255) NULL,
    RAN_AT              DATETIME     NULL,
    PAYLOAD_JSON        CLOB         NULL,
    PARKED_ELEMENTS     CLOB         NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_ANNOTATION_SET_INVESTIGATION
        FOREIGN KEY (INVESTIGATION_FK) REFERENCES INVESTIGATION (ID)
        ON DELETE CASCADE,
    CONSTRAINT FK_ANNOTATION_SET_PARENT
        FOREIGN KEY (PARENT_FK) REFERENCES ANNOTATION_SET (ID)
        ON DELETE SET NULL,
    CONSTRAINT UK_ANNOTATION_SET_INVESTIGATION_ROLE_RUN
        UNIQUE (INVESTIGATION_FK, ROLE, RUN_ID)
);

CREATE INDEX IF NOT EXISTS IDX_ANNOTATION_SET_INVESTIGATION_ROLE
    ON ANNOTATION_SET (INVESTIGATION_FK, ROLE);
CREATE INDEX IF NOT EXISTS IDX_ANNOTATION_SET_PARENT
    ON ANNOTATION_SET (PARENT_FK);
CREATE INDEX IF NOT EXISTS IDX_ANNOTATION_SET_RUN
    ON ANNOTATION_SET (RUN_ID);
