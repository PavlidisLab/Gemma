-- TICKET_TARGET gains PAYLOAD and PAYLOAD_SCHEMA_VERSION -- the task for THIS experiment.
--
-- Everything on a ticket that can carry structure is one per TICKET: BODY, and PAYLOAD (V41). A ticket
-- holding three experiments can describe all three in its body. A ticket holding a thousand cannot, and
-- a curator who opens the 734th of them has nothing saying why it is on the list -- the detail they need
-- is per-experiment, and almost every finding our audits produce is: this factor value is bound to a
-- term that means something else, this tag disagrees with its URI (frinkbro, 2026-09-11).
--
-- Paul, 2026-09-11: "the information on the ticket isn't visible in the individual experiments, so it's
-- a pain. there needs to be a 'proposal' filled in for each experiment" and "the ticket shouldn't have a
-- lengthy preamble on top of it saying something for every experiment, there could be 1000s of them in a
-- ticket. Each item has to have its task associated with it directly."
--
-- 🛑 OPAQUE TO GEMMA, exactly as TICKET.PAYLOAD (V41) is. Nothing parses, validates, filters or indexes
-- it; the schema belongs to the producing agent. JSON as the column type buys storage-level
-- well-formedness and nothing more.
--
-- Not SCREENING_RESULT_REASON (V35): that field is bound to the screening verdict and is a reason for a
-- decision already taken. A task to perform is the opposite direction, and overloading the one free-text
-- field to carry both would make neither readable.
--
-- Both NULL for every existing row, and no path writes them unless a caller supplies one, so every
-- ticket that exists today behaves exactly as it did.
ALTER TABLE TICKET_TARGET
    ADD COLUMN PAYLOAD JSON NULL AFTER SCREENING_RESULT_REASON,
    ADD COLUMN PAYLOAD_SCHEMA_VERSION INT NULL AFTER PAYLOAD;
