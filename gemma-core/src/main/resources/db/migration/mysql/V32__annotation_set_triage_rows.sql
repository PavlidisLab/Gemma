-- ANNOTATION_SET_TRIAGE -- one row per judgement, replacing V30's three columns.
--
-- V30 put the verdict, the judge and the timestamp on ANNOTATION_SET itself. That shape
-- hard-codes the set of judges into the schema: agent and curator today, and every further
-- kind of judge -- a second-opinion reviewer, a QC gate, an external collaborator -- is
-- another migration. Rows make the judge data instead, and they answer questions columns
-- cannot: has any human ever ruled on this set, do two curators disagree, which agent build
-- gets overruled most often.
--
-- The columns are DROPPED rather than left in place. V30 landed the same day and no row ever
-- carried a value, so there is nothing to migrate. Leaving them would give triage two homes,
-- and the one that is easier to SELECT is the one that would silently win.
--
-- NO 'Pending' VERDICT. An un-triaged set has no row here; absence is the state. A stored
-- pending would be a second spelling of the same thing and would put an OR into every query
-- on the triage queue.
--
-- UNIQUE (ANNOTATION_SET_FK, JUDGED_BY) -- one STANDING judgement per judge, upserted when
-- that judge changes their mind. The question a queue asks is what a judge currently thinks,
-- not how many times they toggled. Drop this constraint if the toggle history ever becomes
-- the point; nothing else depends on it.
--
-- THE EFFECTIVE VERDICT IS THE MOST RECENT ROW, not "curator outranks agent". A curator
-- ruling after the agent already wins by recency, and when two curators disagree the later
-- one wins -- the same answer the rest of this workflow gives to contention. JUDGE_KIND still
-- records who, so ranking by role stays available without the schema having decided it.
--
-- IDX (ANNOTATION_SET_FK, JUDGED_AT) is what makes that per-set "latest" lookup cheap. It is
-- the cost of rows over columns: the effective verdict is a subquery rather than a column
-- read, and a corpus-wide triage queue over the whole ANNOTATION_SET table will feel it.
-- Measure before adding more.
--
-- JUDGE_KIND is stored rather than inferred from JUDGED_BY. The identity is a username for a
-- person and a run id for an agent, and telling those apart by shape works until someone's
-- username looks like a UUID.
--
-- NOTE is where a WontFix says what it is declining to fix. The verdict records the decision;
-- without this the reason is lost, and the reason is what a later reader needs.
--
-- ON DELETE CASCADE: a triage ruling on a deleted annotation set is not a thing.
CREATE TABLE ANNOTATION_SET_TRIAGE
(
    ID                BIGINT        NOT NULL AUTO_INCREMENT,
    ANNOTATION_SET_FK BIGINT        NOT NULL,
    TRIAGE            VARCHAR(16)   NOT NULL,
    JUDGED_BY         VARCHAR(255)  NOT NULL,
    JUDGE_KIND        VARCHAR(16)   NOT NULL,
    JUDGED_AT         DATETIME(3)   NOT NULL,
    NOTE              VARCHAR(1024) NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_ANNOTATION_SET_TRIAGE_SET
        FOREIGN KEY (ANNOTATION_SET_FK) REFERENCES ANNOTATION_SET (ID)
        ON DELETE CASCADE,
    CONSTRAINT UK_ANNOTATION_SET_TRIAGE_SET_JUDGE
        UNIQUE KEY (ANNOTATION_SET_FK, JUDGED_BY),
    INDEX IDX_ANNOTATION_SET_TRIAGE_SET_JUDGED (ANNOTATION_SET_FK, JUDGED_AT)
) ENGINE = InnoDB;

ALTER TABLE ANNOTATION_SET
    DROP COLUMN TRIAGE,
    DROP COLUMN TRIAGED_BY,
    DROP COLUMN TRIAGED_AT;
