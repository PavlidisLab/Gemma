-- See db/migration/mysql/V25__publication_association.sql for the canonical description:
-- why the experiment<->publication link needed an evidence slot, why this is a new table
-- rather than columns on RELEVANT_PUBLICATIONS (the primary publication is not in that
-- table, and a rejected row parked there would show up in Gemma 1.32.x as an ordinary
-- publication of the dataset), and why precedence is a rank on SOURCE rather than an
-- exclusion list.
--
-- The version number differs from MySQL's because the H2 and MySQL migration streams are
-- keyed independently.

CREATE TABLE PUBLICATION_ASSOCIATION (
    ID                  BIGINT        NOT NULL AUTO_INCREMENT,
    INVESTIGATION_FK    BIGINT        NOT NULL,
    PUBLICATION_FK      BIGINT        NOT NULL,
    STATUS              VARCHAR(16)   NOT NULL,
    ROLE                VARCHAR(16)   NULL,
    SOURCE              VARCHAR(32)   NOT NULL,
    EVIDENCE            VARCHAR(1000) NULL,
    SUPPORTING_EVIDENCE CLOB          NULL,
    EVIDENCE_CODE       VARCHAR(255)  NULL,
    CONFIDENCE          DOUBLE        NULL,
    ASSERTED_BY         VARCHAR(255)  NULL,
    ASSERTED_AT         TIMESTAMP(3)  NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_PUBLICATION_ASSOCIATION_INVESTIGATION
        FOREIGN KEY (INVESTIGATION_FK) REFERENCES INVESTIGATION (ID)
        ON DELETE CASCADE,
    CONSTRAINT FK_PUBLICATION_ASSOCIATION_PUBLICATION
        FOREIGN KEY (PUBLICATION_FK) REFERENCES BIBLIOGRAPHIC_REFERENCE (ID),
    CONSTRAINT UK_PUBLICATION_ASSOCIATION_INVESTIGATION_PUBLICATION
        UNIQUE (INVESTIGATION_FK, PUBLICATION_FK)
);

CREATE INDEX IDX_PUBLICATION_ASSOCIATION_INVESTIGATION_STATUS
    ON PUBLICATION_ASSOCIATION (INVESTIGATION_FK, STATUS);

CREATE INDEX IDX_PUBLICATION_ASSOCIATION_PUBLICATION
    ON PUBLICATION_ASSOCIATION (PUBLICATION_FK);

CREATE INDEX IDX_PUBLICATION_ASSOCIATION_SOURCE
    ON PUBLICATION_ASSOCIATION (SOURCE);

-- No backfill here. The MySQL sibling seeds assertions for the links already in production;
-- an H2 database is created empty for a test run, so there is nothing to seed and an
-- INSERT...SELECT would only be a second, untested copy of the statement that matters.
