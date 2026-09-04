-- ANNOTATION_SET_DISPOSITION -- one curator ruling on ONE finding inside an audit set.
--
-- The gap this closes, measured by uib on 2026-09-03: a curator working an audit on Gemma
-- has nowhere to record accept / dismiss / needs_more_info per finding. PATCH
-- /annotation-sets/{id} is envelope-only, and /triage is the whole set -- V30's own comment
-- says it is deliberately not this field. The curation UI's remote mode currently REFUSES
-- the write rather than sending it to the local store, because store review ids and Gemma
-- set ids are independent numbers and a write would land on a curation_review row that
-- merely shares the integer.
--
-- PER-FINDING IS A CONSTRAINT, NOT A PREFERENCE. A curator routinely accepts one finding and
-- rejects another on the SAME target -- set 2564 carries three findings on the strain tags
-- alone. One set-level verdict cannot express that outcome, which is why this cannot be a
-- column on ANNOTATION_SET or another value on ANNOTATION_SET_TRIAGE.
--
-- APPEND-ONLY, LATEST WINS -- the opposite choice from V32, and deliberately. There, one
-- standing row per (set, judge) answers "what does this judge currently think about this
-- set", and the toggle history was explicitly not the point. Here the sequence IS part of
-- the record: a finding accepted and then dismissed after a second look is a different
-- history from one dismissed outright, and the rulings outlive the run that produced them
-- because gold edits key off them. So no UNIQUE constraint, and the read folds to the newest
-- row per TARGET_ID.
--
-- TARGET_ID IS OPAQUE. It is the producer's own target_id from the payload, and the payload
-- is stored as JSON text that Gemma never parses. A ruling on a target_id naming no finding
-- is accepted here; only the producer can tell. Making this an FK would mean parsing the
-- payload into rows, which is the design this column exists to avoid.
--
-- THE VOCABULARY IS THE CURATION STORE'S, ADOPTED NOT RE-INVENTED: accepted | dismissed |
-- needs_more_info, as filed by the producing side and already written down in V30's comment.
-- A fourth Gemma-only value would split one vocabulary across two systems whose rows have to
-- be compared. There is no stored 'pending' -- an un-ruled finding has no row, the same rule
-- V32 applies to un-triaged sets.
--
-- JUDGE_KIND reuses the triage vocabulary (agent | curator) rather than declaring a second
-- two-value enum. It is stored rather than inferred from DECIDED_BY for the reason V32 gives:
-- telling a person from an agent by the shape of the identity works until someone's username
-- looks like a UUID.
--
-- REASON is what the agent needs in order to stop emitting a finding. The disposition records
-- the decision; without the reason a dismissal does not say what was wrong with it.
--
-- IDX (ANNOTATION_SET_FK, TARGET_ID, DECIDED_AT) is what makes the per-target "latest" fold
-- cheap; it is the cost of append-only over upsert.
--
-- DECIDED_AT is DATETIME(3) to match CREATED_AT / UPDATED_AT / FINALIZED_AT on ANNOTATION_SET,
-- so "was this ruled before or after the set was finalized" is answerable at one precision on
-- both sides of the comparison.
--
-- ON DELETE CASCADE: a ruling on a finding in a deleted annotation set is not a thing.
CREATE TABLE ANNOTATION_SET_DISPOSITION
(
    ID                BIGINT        NOT NULL AUTO_INCREMENT,
    ANNOTATION_SET_FK BIGINT        NOT NULL,
    TARGET_ID         VARCHAR(255)  NOT NULL,
    DISPOSITION       VARCHAR(16)   NOT NULL,
    DECIDED_BY        VARCHAR(255)  NOT NULL,
    JUDGE_KIND        VARCHAR(16)   NOT NULL,
    DECIDED_AT        DATETIME(3)   NOT NULL,
    REASON            VARCHAR(1024) NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_ANNOTATION_SET_DISPOSITION_SET
        FOREIGN KEY (ANNOTATION_SET_FK) REFERENCES ANNOTATION_SET (ID)
        ON DELETE CASCADE,
    INDEX IDX_ANNOTATION_SET_DISPOSITION_SET_TARGET (ANNOTATION_SET_FK, TARGET_ID, DECIDED_AT)
) ENGINE = InnoDB;
