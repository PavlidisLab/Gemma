-- ============================================================================
-- AclSemanticsContractTest fixture (H2 only).
-- ============================================================================
-- Lives under a non-default Flyway location ("db/migration/h2-acl-contract")
-- so it is only loaded by the contract test (which overrides the Flyway
-- locations to include both paths). Other BaseDatabaseTest5 subclasses see
-- the unmodified default schema, so their "fresh test DB" invariants hold.
--
-- Captures the four ACL situations that the planned EXISTS rewrite of
-- AclQueryUtils must preserve (per project_acl_exists_refactor.md):
--   PUBLIC           — anonymous READ
--   PRIVATE_OWNED    — only the owner SID has READ
--   PRIVATE_SHARED   — owner + a collaborator SID have READ
--   ADMIN_ONLY       — only GROUP_ADMIN has READ
--
-- All inserted IDs are >= 9000 to avoid collisions with V3__seed_data.sql.
-- V3 already seeded acl_class ids 1-2 (User, UserGroup), acl_sid ids 1-6,
-- acl_object_identity ids 1-5, acl_entry ids 1-8.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Test users (in addition to the V3-seeded administrator / gemmaAgent).
-- ----------------------------------------------------------------------------
INSERT INTO CONTACT (ID, class, USER_NAME, NAME, ENABLED)
VALUES (9001, 'User', 'testuser-owner',        'testuser-owner',        1),
       (9002, 'User', 'testuser-collaborator', 'testuser-collaborator', 1);

-- ----------------------------------------------------------------------------
-- ACL SIDs for the new test users. principal=1 -> AclPrincipalSid.
-- ----------------------------------------------------------------------------
INSERT INTO acl_sid (id, principal, sid)
VALUES (9001, 1, 'testuser-owner'),
       (9002, 1, 'testuser-collaborator');

-- ----------------------------------------------------------------------------
-- acl_class rows for the entity types we seed. V3 only inserted User /
-- UserGroup; the canonical EE / AD / BibRef / FactorValue / Gene /
-- CompositeSequence acl_class rows are normally created on-demand by
-- JdbcMutableAclService at first createAcl() — here we materialize them up
-- front so the fixture is self-contained.
-- ----------------------------------------------------------------------------
INSERT INTO acl_class (id, class) VALUES (9001, 'ubic.gemma.model.expression.experiment.ExpressionExperiment');
INSERT INTO acl_class (id, class) VALUES (9002, 'ubic.gemma.model.expression.arrayDesign.ArrayDesign');
INSERT INTO acl_class (id, class) VALUES (9003, 'ubic.gemma.model.common.description.BibliographicReference');

-- ----------------------------------------------------------------------------
-- Taxon — already seeded? No, V3 only seeds AUDIT_TRAIL + USER_GROUP + CONTACT
-- + acl_*. Hibernate baseline creates the schema; we must INSERT a Taxon.
-- ----------------------------------------------------------------------------
INSERT INTO TAXON (ID, IS_GENES_USABLE, NCBI_ID, COMMON_NAME, SCIENTIFIC_NAME)
VALUES (9001, 1, 9606, 'human-acl-fixture', 'Homo sapiens (acl fixture)');

-- ----------------------------------------------------------------------------
-- Audit trails + curation details for the 4 EEs and 3 ADs we will create.
-- ----------------------------------------------------------------------------
INSERT INTO AUDIT_TRAIL (ID) VALUES (9101), (9102), (9103), (9104),    -- EEs
                                    (9201), (9202), (9203),            -- ADs
                                    (9301), (9302), (9303);            -- BibRefs reuse no audit_trail (not NOT NULL on BIBLIOGRAPHIC_REFERENCE)
INSERT INTO CURATION_DETAILS (ID, NEEDS_ATTENTION, TROUBLED)
VALUES (9101, 0, 0), (9102, 0, 0), (9103, 0, 0), (9104, 0, 0),
       (9201, 0, 0), (9202, 0, 0), (9203, 0, 0);

-- ----------------------------------------------------------------------------
-- 3 ArrayDesigns spanning the public / private-owned / admin-only situations.
-- ----------------------------------------------------------------------------
INSERT INTO ARRAY_DESIGN (ID, AUDIT_TRAIL_FK, CURATION_DETAILS_FK, PRIMARY_TAXON_FK, NAME, SHORT_NAME, TECHNOLOGY_TYPE)
VALUES (9201, 9201, 9201, 9001, 'AD public',         'AD_ACL_PUBLIC',     'ONECOLOR'),
       (9202, 9202, 9202, 9001, 'AD private-owned',  'AD_ACL_OWNED',      'ONECOLOR'),
       (9203, 9203, 9203, 9001, 'AD admin-only',     'AD_ACL_ADMIN_ONLY', 'ONECOLOR');

-- ----------------------------------------------------------------------------
-- 4 BibliographicReferences linked from EEs below via PRIMARY_PUBLICATION_FK.
-- The 4th bibref (9304) is NOT linked to any EE — so it is unreachable via
-- the BibRef ACL callsites (which all join on EE), establishing the
-- "non-zero-but-equal-to-zero" baseline.
-- (BibRefs must come BEFORE INVESTIGATION because of the FK on
-- PRIMARY_PUBLICATION_FK.)
-- ----------------------------------------------------------------------------
INSERT INTO BIBLIOGRAPHIC_REFERENCE (ID, TITLE, AUTHOR_LIST, PUBLICATION)
VALUES (9301, 'BibRef for EE_PUBLIC',     'Pavlidis et al', 'Test J'),
       (9302, 'BibRef for EE_OWNED',      'Pavlidis et al', 'Test J'),
       (9303, 'BibRef for EE_ADMIN_ONLY', 'Pavlidis et al', 'Test J'),
       (9304, 'BibRef unattached',        'Pavlidis et al', 'Test J');

-- ----------------------------------------------------------------------------
-- 4 ExpressionExperiments spanning all four ACL situations.
-- ----------------------------------------------------------------------------
INSERT INTO INVESTIGATION (ID, class, AUDIT_TRAIL_FK, CURATION_DETAILS_FK, NAME, SHORT_NAME, TAXON_FK,
                           NUMBER_OF_SAMPLES, NUMBER_OF_DATA_VECTORS, PRIMARY_PUBLICATION_FK)
VALUES (9101, 'ExpressionExperiment', 9101, 9101, 'EE public',         'EE_ACL_PUBLIC',     9001, 0, 0, 9301),
       (9102, 'ExpressionExperiment', 9102, 9102, 'EE private-owned',  'EE_ACL_OWNED',      9001, 0, 0, 9302),
       (9103, 'ExpressionExperiment', 9103, 9103, 'EE private-shared', 'EE_ACL_SHARED',     9001, 0, 0, NULL),
       (9104, 'ExpressionExperiment', 9104, 9104, 'EE admin-only',     'EE_ACL_ADMIN_ONLY', 9001, 0, 0, 9303);

-- ----------------------------------------------------------------------------
-- Experimental design + factor values (2 FVs per EE = 8 FVs total).
-- ----------------------------------------------------------------------------
INSERT INTO EXPERIMENTAL_DESIGN (ID, NAME) VALUES (9101, 'ED for EE_PUBLIC'),
                                                  (9102, 'ED for EE_OWNED'),
                                                  (9103, 'ED for EE_SHARED'),
                                                  (9104, 'ED for EE_ADMIN_ONLY');

-- Wire ED back onto the EE (INVESTIGATION.EXPERIMENTAL_DESIGN_FK is unique).
UPDATE INVESTIGATION SET EXPERIMENTAL_DESIGN_FK = 9101 WHERE ID = 9101;
UPDATE INVESTIGATION SET EXPERIMENTAL_DESIGN_FK = 9102 WHERE ID = 9102;
UPDATE INVESTIGATION SET EXPERIMENTAL_DESIGN_FK = 9103 WHERE ID = 9103;
UPDATE INVESTIGATION SET EXPERIMENTAL_DESIGN_FK = 9104 WHERE ID = 9104;

INSERT INTO EXPERIMENTAL_FACTOR (ID, EXPERIMENTAL_DESIGN_FK, NAME, TYPE)
VALUES (9101, 9101, 'EF for EE_PUBLIC',     'CATEGORICAL'),
       (9102, 9102, 'EF for EE_OWNED',      'CATEGORICAL'),
       (9103, 9103, 'EF for EE_SHARED',     'CATEGORICAL'),
       (9104, 9104, 'EF for EE_ADMIN_ONLY', 'CATEGORICAL');

INSERT INTO FACTOR_VALUE (ID, EXPERIMENTAL_FACTOR_FK, "VALUE")
VALUES (9101, 9101, 'fv-pub-a'),     (9102, 9101, 'fv-pub-b'),
       (9103, 9102, 'fv-own-a'),     (9104, 9102, 'fv-own-b'),
       (9105, 9103, 'fv-shr-a'),     (9106, 9103, 'fv-shr-b'),
       (9107, 9104, 'fv-adm-a'),     (9108, 9104, 'fv-adm-b');

-- ----------------------------------------------------------------------------
-- CompositeSequences under the public AD only (keeps things simple).
-- ----------------------------------------------------------------------------
INSERT INTO COMPOSITE_SEQUENCE (ID, ARRAY_DESIGN_FK, NAME)
VALUES (9101, 9201, 'cs-pub-1'),
       (9102, 9201, 'cs-pub-2'),
       (9103, 9201, 'cs-pub-3');

-- ----------------------------------------------------------------------------
-- A handful of Genes (CHROMOSOME_FEATURE with class='Gene').
-- ----------------------------------------------------------------------------
INSERT INTO CHROMOSOME_FEATURE (ID, class, TAXON_FK, OFFICIAL_SYMBOL, NCBI_GENE_ID)
VALUES (9101, 'Gene', 9001, 'ACLTEST1', 99000001),
       (9102, 'Gene', 9001, 'ACLTEST2', 99000002),
       (9103, 'Gene', 9001, 'ACLTEST3', 99000003);

-- ============================================================================
-- ACL ENTRIES — the heart of the fixture.
-- ============================================================================
-- For each of the 7 secured entities (4 EEs + 3 ADs) we materialize:
--   - one acl_object_identity row owned by sid=9001 (testuser-owner)
--   - the appropriate acl_entry rows for the four ACL situations
--
-- BibRefs are NOT independently secured in Gemma (they ride on the EE that
-- references them); no AOI / entries for them. The BibRef ACL callsites all
-- restrict on the OWNING EE's id.
-- ============================================================================

-- Object identities. acl_class FK: 9001=ExpressionExperiment, 9002=ArrayDesign.
-- owner_sid: V3-seeded sid id=1 is GROUP_ADMIN; testuser-owner is sid 9001.
INSERT INTO acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting)
VALUES (9101, 9001, 9101, NULL, 9001, 0),  -- EE_PUBLIC,     owner=testuser-owner
       (9102, 9001, 9102, NULL, 9001, 0),  -- EE_OWNED,      owner=testuser-owner
       (9103, 9001, 9103, NULL, 9001, 0),  -- EE_SHARED,     owner=testuser-owner
       (9104, 9001, 9104, NULL,    1, 0),  -- EE_ADMIN_ONLY, owner=GROUP_ADMIN
       (9201, 9002, 9201, NULL, 9001, 0),  -- AD_PUBLIC,     owner=testuser-owner
       (9202, 9002, 9202, NULL, 9001, 0),  -- AD_OWNED,      owner=testuser-owner
       (9203, 9002, 9203, NULL,    1, 0);  -- AD_ADMIN_ONLY, owner=GROUP_ADMIN

-- ACL entries (mask=1 = READ per BasePermission.READ).
-- sid id mapping (V3 + fixture):
--   1 = GROUP_ADMIN (granted authority)
--   4 = IS_AUTHENTICATED_ANONYMOUSLY
--   9001 = testuser-owner (principal)
--   9002 = testuser-collaborator (principal)
--
-- For each AOI we also grant GROUP_ADMIN ADMIN (mask=16) so admin lookups
-- behave like in real seeded data; this matches the V3 baseline shape on
-- USER/USER_GROUP AOIs (mask=16 on GROUP_ADMIN, mask=16 on owner principal).

-- ---- EE_PUBLIC (aoi=9101): GROUP_ADMIN admin + anonymous READ + owner READ
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
VALUES (9101, 9101, 1,    1, 16, 1, 0, 0),  -- GROUP_ADMIN ADMIN
       (9102, 9101, 2,    4,  1, 1, 0, 0),  -- IS_AUTHENTICATED_ANONYMOUSLY READ
       (9103, 9101, 3, 9001,  1, 1, 0, 0);  -- testuser-owner READ

-- ---- EE_OWNED (aoi=9102): GROUP_ADMIN admin + owner READ (no anonymous)
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
VALUES (9111, 9102, 1,    1, 16, 1, 0, 0),
       (9112, 9102, 2, 9001,  1, 1, 0, 0);

-- ---- EE_SHARED (aoi=9103): GROUP_ADMIN admin + owner READ + collaborator READ
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
VALUES (9121, 9103, 1,    1, 16, 1, 0, 0),
       (9122, 9103, 2, 9001,  1, 1, 0, 0),
       (9123, 9103, 3, 9002,  1, 1, 0, 0);

-- ---- EE_ADMIN_ONLY (aoi=9104): GROUP_ADMIN admin only (no READ ACE -> nobody but admin can read)
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
VALUES (9131, 9104, 1,    1, 16, 1, 0, 0);

-- ---- AD_PUBLIC (aoi=9201)
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
VALUES (9201, 9201, 1,    1, 16, 1, 0, 0),
       (9202, 9201, 2,    4,  1, 1, 0, 0),
       (9203, 9201, 3, 9001,  1, 1, 0, 0);

-- ---- AD_OWNED (aoi=9202)
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
VALUES (9211, 9202, 1,    1, 16, 1, 0, 0),
       (9212, 9202, 2, 9001,  1, 1, 0, 0);

-- ---- AD_ADMIN_ONLY (aoi=9203)
INSERT INTO acl_entry (id, acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
VALUES (9221, 9203, 1,    1, 16, 1, 0, 0);

-- ============================================================================
-- GROUP MEMBERSHIP — make testuser-owner a member of GROUP_USER so the
-- "non-admin authenticated user" path through the ACL clause has at least
-- one group-derived SID to match. GROUP_USER (id=2) was seeded by V3.
-- Also wire up GROUP_AUTHORITY rows so GROUP_USER actually carries the
-- 'USER' authority that maps to sid 'GROUP_USER' in the join.
-- ============================================================================
INSERT INTO GROUP_MEMBERS (GROUP_MEMBERS_FK, USER_GROUPS_FK)
VALUES (9001, 2);  -- testuser-owner -> Users group

INSERT INTO GROUP_AUTHORITY (ID, GROUP_FK, AUTHORITY)
VALUES (9001, 1, 'ADMIN'),
       (9002, 2, 'USER'),
       (9003, 3, 'AGENT');
