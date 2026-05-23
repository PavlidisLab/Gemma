-- H2 mirror of mysql/V14__audit_event_trail_id_index.sql (HQL_SQL_AUDIT M3).
-- IF NOT EXISTS keeps this safe across baseline regenerations.

CREATE INDEX IF NOT EXISTS IDX_AUDIT_EVENT_TRAIL_ID
    ON AUDIT_EVENT (AUDIT_TRAIL_FK, ID);
