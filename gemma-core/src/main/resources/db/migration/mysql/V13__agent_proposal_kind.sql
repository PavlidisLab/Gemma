-- Step 1 of the AgentCuration unification.
--
-- Adds a KIND discriminator on AGENT_PROPOSAL so the same table can carry both
-- forward-looking agent proposals (default) and post-hoc agent audits. The
-- entity name remains AgentProposal in this step; the rename to AgentCuration
-- is deferred to step 5.
--
-- The unique key (INVESTIGATION_FK, RUN_ID) is replaced by
-- (INVESTIGATION_FK, KIND, RUN_ID) so a single run_id can produce one row of
-- each kind on the same investigation. Safe to swap as a single migration:
-- all existing rows have kind='PROPOSAL' so no key collisions can occur.
--
-- The values stored in KIND are the uppercase enum name() form
-- (matches Hibernate EnumType useNamed=true, the repo's convention for
-- VARCHAR-mapped enums — see Ticket / TicketType for precedent).

ALTER TABLE AGENT_PROPOSAL
    ADD COLUMN KIND VARCHAR(16) NOT NULL DEFAULT 'PROPOSAL';

ALTER TABLE AGENT_PROPOSAL
    DROP KEY UK_AGENT_PROPOSAL_INVESTIGATION_RUN;

ALTER TABLE AGENT_PROPOSAL
    ADD CONSTRAINT UK_AGENT_PROPOSAL_INVESTIGATION_KIND_RUN
        UNIQUE (INVESTIGATION_FK, KIND, RUN_ID);

-- Supports the typical listing query: newest proposals/audits first per
-- (investigation, kind).
CREATE INDEX IDX_AGENT_PROPOSAL_INVESTIGATION_KIND_RAN_AT
    ON AGENT_PROPOSAL (INVESTIGATION_FK, KIND, RAN_AT DESC);
