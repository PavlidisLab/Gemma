-- Mirror of mysql/V50__audit_event_on_behalf_of.sql for the H2 test schema.
-- H2 supports the IF NOT EXISTS form directly, so the mysql script's prepared-statement guard is
-- not needed here. Same intent: applying twice is a no-op.
ALTER TABLE AUDIT_EVENT
    ADD COLUMN IF NOT EXISTS ON_BEHALF_OF VARCHAR(255) NULL;
