-- See db/migration/mysql/V19__ticket_mode_and_target_status.sql for the canonical
-- description. H2 ADD COLUMN syntax is the same; the version number differs
-- because the H2 + MySQL migration streams are keyed independently.

ALTER TABLE TICKET
    ADD COLUMN MODE VARCHAR(16) NOT NULL DEFAULT 'MANUAL';

ALTER TABLE TICKET_TARGET
    ADD COLUMN STATUS VARCHAR(16) NOT NULL DEFAULT 'NOT_DONE';
