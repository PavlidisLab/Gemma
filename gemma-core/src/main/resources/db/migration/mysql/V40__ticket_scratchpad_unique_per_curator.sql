-- One scratchpad per curator, enforced by the database rather than by a query-then-create race.
--
-- getOrCreateScratchpad looks for the curator's SCRATCHPAD ticket and mints one if absent. Two
-- concurrent first calls can both run the SELECT before either commits, and both insert. The service
-- makes that survivable -- findScratchpad is oldest-id-wins, so the identity can never SPLIT, and a
-- duplicate is a stray row rather than a scratchpad that flips between two identities -- but survivable
-- is not prevented.
--
-- 🛑 The obvious constraint is wrong: UNIQUE (TYPE, REPORTER_FK) would forbid a reporter having two
-- ordinary tickets of the same type, which is normal and common. The constraint has to apply to
-- SCRATCHPAD rows only.
--
-- A virtual generated column does that: it is REPORTER_FK for a scratchpad and NULL for everything
-- else, and a UNIQUE index ignores NULLs, so ordinary tickets are unconstrained however many a
-- reporter has. VIRTUAL rather than STORED because it is derived on read and never needs a row
-- rewrite; MySQL 5.7.8+ indexes virtual columns in InnoDB (production is 5.7.44).
--
-- Safe to apply: there are no SCRATCHPAD tickets yet, so no existing row can collide.
ALTER TABLE TICKET
    ADD COLUMN SCRATCHPAD_OWNER_FK BIGINT
        GENERATED ALWAYS AS (CASE WHEN TYPE = 'SCRATCHPAD' THEN REPORTER_FK ELSE NULL END) VIRTUAL,
    ADD UNIQUE INDEX TICKET_ONE_SCRATCHPAD_PER_CURATOR (SCRATCHPAD_OWNER_FK);
