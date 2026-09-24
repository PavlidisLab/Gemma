-- TICKET gains PAYLOAD and PAYLOAD_SCHEMA_VERSION -- what the screen that produced the ticket asked.
--
-- A ticket's title, body and targets say WHICH experiments need work. They do not say what question was
-- put to the curator: the screen's summary, the window it scraped, the verbs on its buttons ("Confirm /
-- Reject" rather than "Include / Exclude"), and which computed fields the producing agent wants shown per
-- candidate. Without a slot for it a screening ticket can only ever render as the fixed GEO-scrape table,
-- and the generic screens -- pub-finder, TF-perturbation, cell-line -- degrade to blank columns
-- (uib, 2026-09-03).
--
-- 🛑 OPAQUE TO GEMMA. Nothing here is parsed, validated, filtered or indexed. The schema belongs to the
-- agents repo, exactly as Investigation.SOURCE_METADATA does; the moment a query touches this column the
-- schema stops being theirs to change. JSON as the column type buys storage-level well-formedness and
-- nothing more.
--
-- Mirrors TICKET_EVENT.PAYLOAD (V3), which is the same decision one level down: an opaque JSON string at
-- the entity layer, decoded by callers that want typed access. This is that, per ticket rather than per
-- event, because the screen's definition describes the ticket and not any single thing that happened to it.
--
-- PAYLOAD_SCHEMA_VERSION ships WITH the payload and not later. Investigation.SOURCE_METADATA is the
-- cautionary case: it had a version column from the start, the version was not put on the wire, and a
-- consumer holding a blob could not tell a v1 document from a null-version one except by guessing at its
-- keys -- which is the guessing the version exists to prevent. Null is a real value meaning "the writer
-- declared none", not a missing one.
--
-- Both NULL for every existing row, and no ticket path writes them unless a caller supplies one, so
-- behaviour is unchanged for every ticket that exists today.
ALTER TABLE TICKET
    ADD COLUMN PAYLOAD JSON NULL AFTER ACCEPTS_TARGETS,
    ADD COLUMN PAYLOAD_SCHEMA_VERSION INT NULL AFTER PAYLOAD;
