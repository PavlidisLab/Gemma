-- H2 sibling of mysql/V6__audit_trail_last_event_id.sql.
-- See that file for motivation and the perf-probe baseline.
--
-- H2 differences from the MySQL variant:
--   * Column / constraint / index added in separate ALTER statements (H2
--     doesn't accept the multi-clause ALTER form MySQL uses).
--   * UPDATE...JOIN syntax: H2 supports it in recent versions but the
--     safer cross-version form is a correlated subquery, which is what
--     hbm2ddl test schemas will encounter.

ALTER TABLE AUDIT_TRAIL ADD COLUMN LAST_EVENT_FK BIGINT NULL;

ALTER TABLE AUDIT_TRAIL ADD CONSTRAINT FK_AUDIT_TRAIL_LAST_EVENT
    FOREIGN KEY (LAST_EVENT_FK) REFERENCES AUDIT_EVENT(ID)
    ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS IDX_AUDIT_TRAIL_LAST_EVENT
    ON AUDIT_TRAIL (LAST_EVENT_FK);

-- Backfill. Correlated subquery form portable across H2 versions.
UPDATE AUDIT_TRAIL t
SET LAST_EVENT_FK = (
    SELECT MAX(ae.ID) FROM AUDIT_EVENT ae
    WHERE ae.AUDIT_TRAIL_FK = t.ID
)
WHERE EXISTS (
    SELECT 1 FROM AUDIT_EVENT ae2 WHERE ae2.AUDIT_TRAIL_FK = t.ID
);
