-- TICKET_TARGET gains SCREENING_RESULT -- the include/reject/undecided call a curator or agent
-- records for one target while screening.
--
-- One ticket system, and it is Gemma (Paul, 2026-08-29): the decision lives on the ticket here,
-- not in a parallel store. The value is deliberately generic and read by the ticket's situation --
-- admit/reject on an inclusion screen, act/skip on a ticket forwarding experiments to another
-- process -- so it is not modelled as a routing vocabulary. What the result then drives (a load, a
-- blacklist entry, a re-run) is exported and handled by CLI tools, not encoded here.
--
-- NULL-able and uncoupled from STATUS on purpose: a REJECT meaning "does not need this process" is
-- not the target being DONE, so the two columns move independently. NULL = no decision recorded.
--
-- VARCHAR(16) matching STATUS, holding the @Enumerated(STRING) constant name (INCLUDE / REJECT /
-- UNDECIDED).
-- ⚠️ PROVISIONAL (Paul, 2026-08-29): landed to unblock the curation workflow, not as the settled
-- screening model. The open question is where PRE-IMPORT candidates live -- a screening ticket is
-- mostly about GEO accessions with no Gemma id, which no TicketTargetType holds today; those come in
-- as PRELOADs. Revisit before treating screening tickets as the system of record for inclusion.
ALTER TABLE TICKET_TARGET
    ADD COLUMN SCREENING_RESULT VARCHAR(16) NULL AFTER STATUS,
    ADD COLUMN SCREENING_RESULT_REASON TEXT NULL AFTER SCREENING_RESULT;
