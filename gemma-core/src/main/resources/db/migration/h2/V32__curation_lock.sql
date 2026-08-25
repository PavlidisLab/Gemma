-- H2 mirror of mysql/V31__curation_lock.sql; see that file for the reasoning.
-- Differences from the MySQL original, both mechanical:
--   * no ENGINE = InnoDB clause;
--   * CONSTRAINT names kept, so a failure reports the same name on both engines.
CREATE TABLE CURATION_LOCK (
    INVESTIGATION_FK BIGINT       NOT NULL,
    LOCKED_BY        VARCHAR(255) NOT NULL,
    LOCKED_AT        DATETIME(3)  NOT NULL,
    EXPIRES_AT       DATETIME(3)  NOT NULL,
    STOLEN_FROM      VARCHAR(255) NULL,
    STOLEN_AT        DATETIME(3)  NULL,
    PRIMARY KEY (INVESTIGATION_FK),
    CONSTRAINT FK_CURATION_LOCK_INVESTIGATION
        FOREIGN KEY (INVESTIGATION_FK) REFERENCES INVESTIGATION (ID) ON DELETE CASCADE
);
