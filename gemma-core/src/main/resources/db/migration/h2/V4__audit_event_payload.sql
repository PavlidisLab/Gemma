-- H2 mirror of mysql/V2__audit_event_payload.sql. H2's JSON type is
-- intentionally permissive across versions; CLOB is the safest portable
-- choice and matches how the Hibernate mapping persists the field (raw
-- JSON string, not a structured column).
ALTER TABLE AUDIT_EVENT ADD COLUMN PAYLOAD CLOB NULL;
