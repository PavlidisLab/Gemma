-- CURATION_DECISION -- a curator's standing ruling that a change must NOT be made.
--
-- THE GAP, MEASURED. Of 821 well-formed rows in the curation ledger, 29 are refusals --
-- reject_add / reject_factor / reject_drop / reject_all -- and no API verb expresses any of
-- them. A refusal has nothing to commit: the whole content is the "no". Until now a curator
-- who ruled against a proposed edit could only DELETE the proposal, losing the fact that they
-- had considered it, or leave it, where it reads as pending forever. Same hole
-- V30 described for a whole annotation set, one layer down and with no proposal to attach to.
--
-- KEYED ON CONTENT, NEVER ON A PROPOSED ITEM'S ID. The purpose is to stop the same edit being
-- proposed again next quarter, and next quarter's proposal is a new item with a new id -- so a
-- ruling keyed on the item in front of the curator can never match the thing it exists to
-- prevent, and would decay into an audit record while looking like a gate. This is the same
-- lesson V44 paid for one layer up, where TARGET_ID turned out to name a target and not a
-- finding.
--
-- DECISION_KEY IS OPAQUE, like ANNOTATION_SET_DISPOSITION.TARGET_ID. The side that understands
-- the curation vocabulary computes and matches it, which keeps that vocabulary out of this
-- schema so a new kind of refusal is not a new migration.
-- ⇒ GEMMA RECORDS A REFUSAL AND CANNOT ENFORCE ONE. A commit that violates a standing refusal
-- is NOT rejected here. Enforcement belongs where the meaning lives, on the proposing side.
-- Building the gate here would mean parsing curation content in the schema, which is the
-- design this column exists to avoid.
--
-- DECISION_SCOPE carries what separates the four verbs: ITEM is one tag or one deletion
-- (reject_add, reject_drop); KEY gates everything under a key INCLUDING siblings not yet
-- proposed, which is what reject_factor means and why a target alone will not do; PROPOSAL
-- answers a whole annotation set at once (reject_all) and names it rather than a key.
-- The scope is part of what a ruling supersedes: a ruling on one item does not reverse a
-- ruling on the whole key it belongs to, nor the other way round.
--
-- NOT 'SCOPE'. The column is DECISION_SCOPE because SCOPE is a keyword in enough SQL contexts
-- that quoting it would become someone else's problem in a hand-written query.
--
-- DECISION IS REFUSED | ALLOWED, one column rather than two tables. An approval of an edit
-- that never landed (20 more ledger rows) is a refusal with the polarity flipped: a decision a
-- curator made, about a change, that produced no artifact to hang it off. If approvals retire
-- once curation happens in Gemma directly -- where the commit IS the approval -- nothing is
-- written with that value and the column costs nothing.
--
-- REASON IS NOT NULL, unlike a disposition's. A refusal has no other content; without it the
-- row says a change was rejected and gives a later reader nothing to judge whether the
-- rejection still applies, which is the entire reason it is kept.
--
-- PER EXPERIMENT. A ruling that applies corpus-wide is a CONVENTION -- "a strain is not a
-- genotype" -- and belongs in the curation rules the agent reads. It has no dataset to attach
-- to, no ACL scope, and nothing in Gemma would read it back; a table here would be write-only.
-- 77 ledger rows are conventions and they are deliberately NOT migrated into this table.
--
-- APPEND-ONLY, LATEST WINS, as ANNOTATION_SET_DISPOSITION is. A curator who lifts a refusal
-- writes an ALLOWED row rather than deleting the "no", so why it was refused in the first
-- place survives the reversal -- which is the point of keeping refusals at all.
--
-- ANNOTATION_SET_FK is NULLABLE and most rows will not have one: only 21 of the 821 ledger
-- rulings name an originating proposal, the rest coming from chat, corpus sweeps and recovery.
-- A decision stands on its own without one.
--
-- Both FKs cascade on delete: a ruling about a deleted experiment, or answering a deleted
-- proposal, is not a thing.
CREATE TABLE CURATION_DECISION
(
    ID                BIGINT        NOT NULL AUTO_INCREMENT,
    INVESTIGATION_FK  BIGINT        NOT NULL,
    DECISION          VARCHAR(16)   NOT NULL,
    DECISION_SCOPE    VARCHAR(16)   NOT NULL,
    DECISION_KEY      VARCHAR(255)  NULL,
    ANNOTATION_SET_FK BIGINT        NULL,
    REASON            VARCHAR(1024) NOT NULL,
    DECIDED_BY        VARCHAR(255)  NOT NULL,
    JUDGE_KIND        VARCHAR(16)   NOT NULL,
    DECIDED_AT        DATETIME(3)   NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_CURATION_DECISION_INVESTIGATION
        FOREIGN KEY (INVESTIGATION_FK) REFERENCES INVESTIGATION (ID)
        ON DELETE CASCADE,
    CONSTRAINT FK_CURATION_DECISION_ANNOTATION_SET
        FOREIGN KEY (ANNOTATION_SET_FK) REFERENCES ANNOTATION_SET (ID)
        ON DELETE CASCADE,
    INDEX IDX_CURATION_DECISION_INV_KEY (INVESTIGATION_FK, DECISION_KEY, DECIDED_AT)
) ENGINE = InnoDB;
