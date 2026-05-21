-- PERF_PROBE_ROUND3 hotspot B fix: denormalise the "last event per audit
-- trail" pointer so whole-corpus "last event of type T" lookups
-- (AuditEventDaoImpl#getLastEvents(Class<T>, Class<? extends AuditEventType>))
-- collapse from O(events) to O(trails).
--
-- Round-3 probe baseline: 8.7s pulling 1.5M AUDIT_EVENT rows for the
-- ExpressionExperiment dashboard. Commit 0570c46416 rewrote to a SQL-side
-- per-trail MAX(date) + MAX(id) aggregate (5.3s); this migration adds the
-- denormalised FK that lets the rewritten query JOIN through a single
-- index-resolved column per trail (~50k row reads on a typical EE corpus).
--
-- Maintenance: writers that append to AuditTrail.events also repoint
-- AuditTrail.lastEvent (AuditTrailServiceImpl#doAddUpdateEvent +
-- AuditTrailEventListener#emitLifecycleEvent). ON DELETE SET NULL keeps the
-- column safe if an AuditEvent row is hand-deleted; cascade-delete of an
-- Auditable removes the trail and its events together, so the SET NULL path
-- is a defensive backstop rather than a hot path.

ALTER TABLE AUDIT_TRAIL
    ADD COLUMN LAST_EVENT_FK BIGINT NULL,
    ADD CONSTRAINT FK_AUDIT_TRAIL_LAST_EVENT
        FOREIGN KEY (LAST_EVENT_FK) REFERENCES AUDIT_EVENT(ID)
        ON DELETE SET NULL,
    ADD INDEX IDX_AUDIT_TRAIL_LAST_EVENT (LAST_EVENT_FK);

-- One-shot backfill of existing trails. Trails with zero events stay NULL
-- (the LEFT JOIN below filters them out; UPDATE...JOIN is an inner join
-- shape under MySQL so non-matching rows are simply not touched).
--
-- Tie-breaker: MAX(ID) on equal date — matches the prior SQL-side MAX
-- rewrite's convention (AuditEventDaoImpl getLastEvents(Class, Class)
-- comment: "MAX(id) wins on tied date"). The per-trail "latest" is
-- date-max + id-max-as-secondary, but for trails with monotonically
-- increasing IDs (the common case: bulk insert → id grows with date)
-- MAX(ID) is equivalent. The migration uses MAX(ID) for simplicity; the
-- application-level writers compute (date desc, id desc) directly.
UPDATE AUDIT_TRAIL t
    INNER JOIN (
        SELECT AUDIT_TRAIL_FK AS trail_fk, MAX(ID) AS last_event_id
        FROM AUDIT_EVENT
        WHERE AUDIT_TRAIL_FK IS NOT NULL
        GROUP BY AUDIT_TRAIL_FK
    ) m ON m.trail_fk = t.ID
SET t.LAST_EVENT_FK = m.last_event_id;
