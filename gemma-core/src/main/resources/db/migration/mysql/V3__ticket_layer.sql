-- Phase B-1 of the audit-as-workflow plan (AUDIT_AS_WORKFLOW_RECCE.md).
-- Introduces the Ticket layer that will progressively replace CurationDetails
-- (Decision 1): TICKET, TICKET_TARGET, TICKET_EVENT.
--
-- Tickets are themselves Auditable (Decision 6), so TICKET carries an
-- AUDIT_TRAIL_FK in the same way INVESTIGATION / ARRAY_DESIGN / USER_GROUP do.
--
-- TICKET_TARGET.TARGET_ID is a bare FK — intentionally NOT constrained at
-- the DB level so a single composite index over (TARGET_TYPE, TARGET_ID)
-- can serve the "tickets for this entity" lookup across heterogeneous
-- target tables without coupling the ticket schema to any one of them.

CREATE TABLE TICKET (
    ID                          BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    AUDIT_TRAIL_FK              BIGINT NOT NULL UNIQUE,
    NAME                        VARCHAR(255) NOT NULL,                  -- ticket title (inherited Describable.name)
    DESCRIPTION                 TEXT NULL,
    TYPE                        VARCHAR(64) NOT NULL,                   -- TicketType enum
    STATE                       VARCHAR(32) NOT NULL,                   -- TicketState enum
    PRIORITY                    VARCHAR(16) NOT NULL,                   -- TicketPriority enum
    DUE_DATE                    DATETIME(3) NULL,
    REPORTER_FK                 BIGINT NOT NULL,
    ASSIGNEE_FK                 BIGINT NULL,
    CREATED_AT                  DATETIME(3) NOT NULL,
    UPDATED_AT                  DATETIME(3) NOT NULL,
    EXTERNAL_ISSUE_URL          VARCHAR(512) NULL,
    EXTERNAL_ISSUE_SYNC_STATE   VARCHAR(16) NOT NULL DEFAULT 'NONE',
    CONSTRAINT TICKET_AUDIT_TRAIL_FKC      FOREIGN KEY (AUDIT_TRAIL_FK) REFERENCES AUDIT_TRAIL (ID),
    CONSTRAINT TICKET_REPORTER_FKC         FOREIGN KEY (REPORTER_FK)    REFERENCES CONTACT (ID),
    CONSTRAINT TICKET_ASSIGNEE_FKC         FOREIGN KEY (ASSIGNEE_FK)    REFERENCES CONTACT (ID),
    INDEX TICKET_NAME  (NAME),
    INDEX TICKET_TYPE  (TYPE),
    INDEX TICKET_STATE (STATE)
) ENGINE=InnoDB;

CREATE TABLE TICKET_TARGET (
    ID            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    TICKET_FK     BIGINT NOT NULL,
    TARGET_TYPE   VARCHAR(32) NOT NULL,                                 -- TicketTargetType enum
    TARGET_ID     BIGINT NOT NULL,                                      -- bare FK, intentionally not constrained
    CONSTRAINT TICKET_TARGET_TICKET_FKC FOREIGN KEY (TICKET_FK) REFERENCES TICKET (ID),
    UNIQUE KEY TICKET_TARGET_UNIQUE (TICKET_FK, TARGET_TYPE, TARGET_ID),
    INDEX TICKET_TARGET_LOOKUP (TARGET_TYPE, TARGET_ID)
) ENGINE=InnoDB;

CREATE TABLE TICKET_EVENT (
    ID            BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    TICKET_FK     BIGINT NOT NULL,
    ACTOR_FK      BIGINT NOT NULL,
    OCCURRED_AT   DATETIME(3) NOT NULL,
    TYPE          VARCHAR(64) NOT NULL,                                 -- TicketEventType enum
    PAYLOAD       JSON NULL,                                            -- same shape as AUDIT_EVENT.PAYLOAD
    CONSTRAINT TICKET_EVENT_TICKET_FKC FOREIGN KEY (TICKET_FK) REFERENCES TICKET (ID),
    CONSTRAINT TICKET_EVENT_ACTOR_FKC  FOREIGN KEY (ACTOR_FK)  REFERENCES CONTACT (ID),
    INDEX TICKET_EVENT_OCCURRED_AT (OCCURRED_AT)
) ENGINE=InnoDB;
