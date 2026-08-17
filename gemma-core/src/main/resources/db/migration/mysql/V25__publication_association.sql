-- Evidence and status for the experiment<->publication link.
--
-- The link was the one assertion in the model with no evidence slot. Gemma could record
-- "the publication for this experiment is X" and nothing else: not that Y had been
-- considered and ruled out, not that X came from GEO's own cross-link rather than from
-- someone reading the paper, not who decided or when. So every rejection had to be
-- remembered outside Gemma -- today in a hand-maintained exclusion file in the eval repo --
-- and every re-run of a publication finder re-proposed the paper a curator had already
-- thrown out. Annotations have carried EVIDENCE_CODE and (since V22) SUPPORTING_EVIDENCE
-- for years; this gives the publication link the same treatment.
--
-- ADDITIVE ONLY -- a bare CREATE TABLE plus a backfill INSERT. Nothing existing is altered,
-- renamed or dropped. Production Gemma 1.32.x shares this database and maps both
-- INVESTIGATION.PRIMARY_PUBLICATION_FK and RELEVANT_PUBLICATIONS; it never learns this
-- table exists and is unaffected in any deploy order. Same reasoning as V24.
--
-- WHY A NEW TABLE RATHER THAN COLUMNS ON RELEVANT_PUBLICATIONS. Two reasons, either one
-- decisive:
--   1. The primary publication is not in that table at all -- it is a plain FK column on
--      INVESTIGATION -- so the link that matters most, and the one GEO gets wrong, would
--      have had nowhere to hang evidence.
--   2. A rejected row must not be visible as a publication of the dataset. Parked in
--      RELEVANT_PUBLICATIONS with a STATUS column, Gemma 1.32.x -- which does not read
--      STATUS -- would list every rejected paper as a relevant publication of the
--      experiment. That is a semantic break no additive column can avoid.
--
-- HOW THE TWO HALVES RELATE. The legacy structures stay as they are and remain what Gemma
-- 1.x reads. This table is the full record; the service keeps the halves in step -- every
-- ACCEPTED row has a matching legacy link, every legacy link has a row, and REJECTED rows
-- live here only. ROLE records which slot an accepted row occupies, which is what makes the
-- legacy structures reconstructable from this table and therefore droppable at the 1.x
-- cutover.
--
-- PRECEDENCE IS A RANK, NOT A LIST. SOURCE carries an authority rank in the enum
-- (curator 40 > geo_submitter_link / external_import 30 > agent 20 > legacy 10) and a
-- writer may only displace an assertion it outranks. A nightly GEO re-fetch that
-- re-proposes a link a curator rejected is refused at the one point every writer passes
-- through, instead of being filtered out afterwards by a denylist that is only as good as
-- the number of code paths remembering to consult it. That missing property is why a
-- correction made on 2026-08-13 was silently reverted by a cache rebuild on 08-14.
--
-- EVIDENCE IS TWO COLUMNS ON PURPOSE. EVIDENCE is the one-line quotable basis, always safe
-- to show a curator as-is ("series title matches the paper title"). SUPPORTING_EVIDENCE is
-- the optional structured JSON array behind it, in the agents' FindingEvidence shape,
-- stored opaquely exactly as CHARACTERISTIC.SUPPORTING_EVIDENCE is -- Gemma never parses or
-- queries it, so the agents repo owns that schema and can evolve it without another
-- migration here.

CREATE TABLE PUBLICATION_ASSOCIATION (
    ID                  BIGINT        NOT NULL AUTO_INCREMENT,
    INVESTIGATION_FK    BIGINT        NOT NULL,
    PUBLICATION_FK      BIGINT        NOT NULL,
    STATUS              VARCHAR(16)   NOT NULL,
    ROLE                VARCHAR(16)   NULL,
    SOURCE              VARCHAR(32)   NOT NULL,
    EVIDENCE            VARCHAR(1000) NULL,
    SUPPORTING_EVIDENCE TEXT          NULL,
    EVIDENCE_CODE       VARCHAR(255)  NULL,
    CONFIDENCE          DOUBLE        NULL,
    ASSERTED_BY         VARCHAR(255)  NULL,
    ASSERTED_AT         DATETIME(3)   NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_PUBLICATION_ASSOCIATION_INVESTIGATION
        FOREIGN KEY (INVESTIGATION_FK) REFERENCES INVESTIGATION (ID)
        ON DELETE CASCADE,
    -- deliberately NOT cascading: a BibliographicReference is shared across experiments and
    -- must not be removable out from under a live assertion.
    CONSTRAINT FK_PUBLICATION_ASSOCIATION_PUBLICATION
        FOREIGN KEY (PUBLICATION_FK) REFERENCES BIBLIOGRAPHIC_REFERENCE (ID),
    CONSTRAINT UK_PUBLICATION_ASSOCIATION_INVESTIGATION_PUBLICATION
        UNIQUE KEY (INVESTIGATION_FK, PUBLICATION_FK),
    INDEX IDX_PUBLICATION_ASSOCIATION_INVESTIGATION_STATUS (INVESTIGATION_FK, STATUS),
    INDEX IDX_PUBLICATION_ASSOCIATION_PUBLICATION (PUBLICATION_FK),
    INDEX IDX_PUBLICATION_ASSOCIATION_SOURCE (SOURCE)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Backfill 1/2: existing primary publications.
--
-- A GEO-accessioned dataset that carries a primary publication almost certainly got it from
-- GEO's !Series_pubmed_id: the GEO importer is the only writer that sets one without a
-- human in the loop. "Almost certainly" is not "verified", and the difference is recorded
-- rather than papered over -- EVIDENCE_CODE is IIA (inferred from imported annotation:
-- present in imported data, evidence in the original source unknown), not TAS, and the
-- EVIDENCE text says in words that the row was inferred from the import path and not
-- checked against GEO. A curator who replaced the link by hand is mislabelled by this, and
-- the practical cost of that is nil: a curator ruling still outranks GEO, and the GEO
-- refresh path only writes when there is no primary at all.
--
-- Everything else -- non-GEO datasets, and any dataset whose accession we cannot resolve --
-- is LEGACY: the lowest rank, no evidence, no claim made. That is the honest description of
-- a bare FK, and it makes "which links still have no recorded basis?" one WHERE clause.
INSERT INTO PUBLICATION_ASSOCIATION
    (INVESTIGATION_FK, PUBLICATION_FK, STATUS, ROLE, SOURCE, EVIDENCE, EVIDENCE_CODE, ASSERTED_AT)
SELECT i.ID,
       i.PRIMARY_PUBLICATION_FK,
       'ACCEPTED',
       'PRIMARY',
       CASE WHEN ed.NAME = 'GEO' THEN 'GEO_SUBMITTER_LINK' ELSE 'LEGACY' END,
       CASE WHEN ed.NAME = 'GEO'
            THEN 'Backfilled 2026-08-17: dataset was imported from GEO and carries a primary publication, and the GEO importer is the only writer that sets one automatically, so this is taken to be GEO''s !Series_pubmed_id. Inferred from the import path, not verified against GEO, and not distinguishable here from a link a curator set by hand.'
            ELSE NULL END,
       CASE WHEN ed.NAME = 'GEO' THEN 'IIA' ELSE NULL END,
       NOW(3)
FROM INVESTIGATION i
         LEFT JOIN DATABASE_ENTRY de ON de.ID = i.ACCESSION_FK
         LEFT JOIN EXTERNAL_DATABASE ed ON ed.ID = de.EXTERNAL_DATABASE_FK
WHERE i.PRIMARY_PUBLICATION_FK IS NOT NULL;

-- Backfill 2/2: existing other-relevant publications, all as LEGACY.
--
-- Not given the GEO treatment above even for GEO datasets, and the asymmetry is deliberate.
-- GEO does land here -- convertPubMedIds puts the second and later !Series_pubmed_id values
-- in this set -- but so does every curator who has ever attached a follow-up paper through
-- the UI, and there is nothing in the row to tell the two apart. For the primary slot the
-- automated writer dominates; for this one it does not, so the honest source is "unknown".
--
-- NOT EXISTS guards the unique key: a reference that is both the primary and an
-- other-relevant row (the current write path prevents it, older data may not have) already
-- has its assertion from the insert above, and the primary reading is the truer one.
INSERT INTO PUBLICATION_ASSOCIATION
    (INVESTIGATION_FK, PUBLICATION_FK, STATUS, ROLE, SOURCE, ASSERTED_AT)
SELECT DISTINCT rp.INVESTIGATIONS_FK,
       rp.OTHER_RELEVANT_PUBLICATIONS_FK,
       'ACCEPTED',
       'OTHER_RELEVANT',
       'LEGACY',
       NOW(3)
FROM RELEVANT_PUBLICATIONS rp
WHERE NOT EXISTS (SELECT 1
                  FROM PUBLICATION_ASSOCIATION pa
                  WHERE pa.INVESTIGATION_FK = rp.INVESTIGATIONS_FK
                    AND pa.PUBLICATION_FK = rp.OTHER_RELEVANT_PUBLICATIONS_FK);
