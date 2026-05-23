-- Composite (AUDIT_TRAIL_FK, ID) index on AUDIT_EVENT for cursor pagination.
-- AuditEventDaoImpl uses `where t = :at and e.id > :lastSeenId order by e.id`
-- which today plans as PK + FK intersect; the composite lets MySQL do a
-- single ref+range scan keyed on (trail, id). Index size is tiny relative
-- to the existing single-column FK index AUDIT_EVENT_AUDIT_TRAIL_FKC.
--
-- HQL_SQL_AUDIT.md M3.

ALTER TABLE AUDIT_EVENT
    ADD INDEX IDX_AUDIT_EVENT_TRAIL_ID (AUDIT_TRAIL_FK, ID);
