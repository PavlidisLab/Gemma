-- ANNOTATION_RELATION -- one queryable home for "term A stands in relation R to term B".
--
-- Gemma holds this knowledge in four places and can query it from none of them:
--
--   CURATED   a curator already wrote it as a Statement. 10,040 datasets carry a
--             GENO_0000222 has_genotype statement, 1,829 an RO_0002573 has modifier,
--             469 a TGEMO_00171 induced by. The triple is right there in
--             CHARACTERISTIC.PREDICATE/OBJECT and nothing can ask "which genotypes are
--             asserted against Leigh syndrome?" because the only index is per-experiment.
--   ONTOLOGY  a loaded ontology asserts it. CLO says MCF7 derives from a patient having
--             DOID_3458 and is a disease model for DOID_299; MONDO says which taxon a
--             disease is restricted to; CL says which UBERON part a cell type belongs to.
--             OntologyTerm.getRestrictions() has always been able to read these and is
--             called from nowhere.
--   EXTERNAL  a third-party resource asserts it (MGI_DO, Cellosaurus).
--   CORPUS    nobody asserts it, but our own curation attests it by co-occurrence, and
--             the specificity of that co-occurrence is measurable.
--
-- Those are ranked, best first, and the ranking is the point: an assertion beats an
-- attestation. A co-occurrence count is what you fall back on when nothing states the fact.
--
-- CORROBORATION IS THE READ-TIME RULE. The same triple can be attested by several bases at
-- once, on purpose: one row per basis, aggregated at read into a relation carrying the set
-- of bases that support it. A CORPUS-only relation is reported as uncorroborated and ranks
-- below one that any other basis also states, because co-occurrence on its own is a weak
-- link. Where MGI and our corpus agree, confidence rises; where they disagree, the
-- disagreement is usually MGI listing other diseases for the same gene rather than
-- contradicting ours, which is why the read reports the bases instead of collapsing them to
-- a score.
--
-- The CORPUS basis is SELF-CONSUMING, and this is the reason it must never be the only one.
-- The co-occurrence exists because we were overtagging: a whole-experiment disease tag was
-- written beside a genotype that already implied it. The moment that redundant tag stops
-- being written -- which is exactly what the curation agents want to do on the grounds that
-- it is inferable -- the evidence for the inference stops accruing. So this basis is a
-- snapshot of past curation practice, not a signal that keeps growing, and anything relying
-- on it alone gets weaker every time curation improves.
--
-- NOT A DISEASE TABLE. Nothing in this schema names disease, genotype or cell line. The
-- genotype->disease case is one (SUBJECT_CATEGORY_URI, PREDICATE_URI, OBJECT_CATEGORY_URI)
-- combination among many; cell line->organism part, cell type->organism part,
-- cell line->species and disease->taxon are the same rows with different terms in them.
--
-- ADDITIVE ONLY -- a bare CREATE TABLE. Production Gemma 1.32.x shares this database and
-- must be able to ignore it completely, which it can: nothing existing is altered and no
-- existing reader joins to it.
--
-- REBUILT, NEVER UPSERTED. Every row is derived, so a run deletes the rows for the basis
-- (optionally for one experiment) and re-inserts them. This is deliberate and is why there
-- is no unique key: EXPRESSION_EXPERIMENT2CHARACTERISTIC upserts on a natural key and has
-- 1,008 rows that a full rebuild could not correct, because an upsert can only fix rows the
-- new query still produces -- a row whose source annotation was deleted survives forever.
-- Delete-then-insert has no such failure mode. It also dodges the MySQL trap that makes the
-- natural key unusable here anyway: most of these columns are nullable (a value with no URI
-- is ordinary), and MySQL allows duplicate NULLs in a UNIQUE key, so ON DUPLICATE KEY UPDATE
-- would silently insert instead of updating.

CREATE TABLE ANNOTATION_RELATION (
    ID                                    BIGINT       NOT NULL AUTO_INCREMENT,

    -- The subject term. Grain note: this is the annotation VALUE as curated, not a gene.
    -- `Myc overexpression` and `Myc knockdown` accompany different diseases and must not
    -- collapse; `APP/PS1`, `5xFAD` and `trisomy 21` name no gene at all and would otherwise
    -- be unrepresentable.
    SUBJECT_VALUE                         VARCHAR(255) NOT NULL,
    SUBJECT_VALUE_URI                     VARCHAR(255) NULL,
    SUBJECT_CATEGORY                      VARCHAR(255) NULL,
    SUBJECT_CATEGORY_URI                  VARCHAR(255) NULL,

    -- From Relation.terms.txt, which is the authoritative vocabulary. Null only for CORPUS
    -- rows the producer declines to name a verb for: co-occurrence on its own proves
    -- association, and asserting a specific relation from it would overstate the evidence.
    PREDICATE                             VARCHAR(255) NULL,
    PREDICATE_URI                         VARCHAR(255) NULL,

    OBJECT_VALUE                          VARCHAR(255) NOT NULL,
    OBJECT_VALUE_URI                      VARCHAR(255) NULL,
    OBJECT_CATEGORY                       VARCHAR(255) NULL,
    OBJECT_CATEGORY_URI                   VARCHAR(255) NULL,

    -- Part of the grain, not decoration: a mouse carrying a Mecp2 null is a MODEL of Rett
    -- syndrome, a human line carrying LRRK2 G2019S HAS Parkinson disease. Same relation,
    -- different predicate, decided by taxon. Null means unknown, which reads as the weaker
    -- claim.
    TAXON_FK                              BIGINT       NULL,

    BASIS                                 VARCHAR(16)  NOT NULL,
    -- Which ontology or resource asserted it, and at which version, so the row is
    -- invalidated with its source. Null for CURATED and CORPUS, whose source is Gemma.
    SOURCE                                VARCHAR(64)  NULL,
    SOURCE_VERSION                        VARCHAR(64)  NULL,
    EVIDENCE_CODE                         VARCHAR(255) NULL,

    -- Set on rows that some experiment attests (CURATED, CORPUS); null on rows asserted
    -- independently of our holdings (ONTOLOGY, EXTERNAL).
    --
    -- One row per attesting experiment, NOT a stored count. Support has to be counted at
    -- read behind the caller's ACL, because a count cannot be ACL-filtered after the fact:
    -- a public-only count understates for a curator, and a whole-corpus count leaks the
    -- existence of private datasets through the denominator. The mask is carried here for
    -- the same reason EE2C carries it, and is read with the same
    -- EE2CAclQueryUtils.formNativeAclRestrictionClause.
    EXPRESSION_EXPERIMENT_FK              BIGINT       NULL,
    LEVEL                                 VARCHAR(255) NULL,
    ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK INT          NOT NULL DEFAULT 0,

    GENERATED_AT                          DATETIME(3)  NOT NULL,

    PRIMARY KEY (ID),

    -- ON DELETE CASCADE: a derived row about a deleted experiment is garbage, and waiting
    -- for the next rebuild to notice would leave it answering queries in the meantime.
    CONSTRAINT FK_ANNOTATION_RELATION_EE
        FOREIGN KEY (EXPRESSION_EXPERIMENT_FK) REFERENCES INVESTIGATION (ID)
        ON DELETE CASCADE,
    CONSTRAINT FK_ANNOTATION_RELATION_TAXON
        FOREIGN KEY (TAXON_FK) REFERENCES TAXON (ID),

    -- Both directions are first-class. "What models Leigh syndrome?" and "what does this
    -- genotype model?" are the same rows read from opposite ends, and the experiment page
    -- asks the second one while the browse selector asks the first.
    INDEX IDX_ANNOTATION_RELATION_SUBJECT (SUBJECT_VALUE_URI, PREDICATE_URI),
    INDEX IDX_ANNOTATION_RELATION_OBJECT (OBJECT_VALUE_URI, PREDICATE_URI),
    -- Values with no URI are ordinary here (`aortic banding` has none), so the un-grounded
    -- legs need their own index rather than riding the URI ones.
    INDEX IDX_ANNOTATION_RELATION_SUBJECT_VALUE (SUBJECT_VALUE),
    INDEX IDX_ANNOTATION_RELATION_OBJECT_VALUE (OBJECT_VALUE),
    -- Rebuild deletes by basis, incremental rebuild deletes by basis + experiment.
    INDEX IDX_ANNOTATION_RELATION_BASIS (BASIS, EXPRESSION_EXPERIMENT_FK),
    INDEX IDX_ANNOTATION_RELATION_EE (EXPRESSION_EXPERIMENT_FK)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
