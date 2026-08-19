/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.common.description;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.common.description.PublicationAssociation;
import ubic.gemma.model.common.description.PublicationAssociationRole;
import ubic.gemma.model.common.description.PublicationAssociationSource;
import ubic.gemma.model.common.description.PublicationAssociationStatus;
import ubic.gemma.model.expression.experiment.PreboardedExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;

/**
 * Integration test for the {@code PUBLICATION_ASSOCIATION} schema migration (MySQL V25 / H2 V26) and
 * the {@link PublicationAssociation} entity + service, against the real MySQL {@code gemdtest}.
 *
 * <p>Two things are being pinned. The first is ordinary: the mapping matches the DDL and every
 * enum, the opaque evidence blob and the unique key survive a round-trip.</p>
 *
 * <p>The second is the reason the table exists. A rejection recorded by a curator has to survive a
 * later writer of lower authority re-proposing the same paper — that is the whole difference between
 * this and the exclusion file it replaces, and it is the thing that will quietly stop working if
 * someone "simplifies" the rank comparison into a last-write-wins upsert. {@link #rejectionOutranksALaterAgent()}
 * and {@link #curatorOverridesAnEarlierRejection()} are the pair: the first proves a lower authority
 * is refused, the second proves the refusal is about rank and not about immutability.</p>
 *
 * <p>Class-level {@link Transactional} rolls each test back, so no cleanup is needed.</p>
 */
@Transactional
public class PublicationAssociationPersistenceIT extends BaseIntegrationTest5 {

    @Autowired
    private PublicationAssociationService publicationAssociationService;

    @Autowired
    private PublicationAssociationDao publicationAssociationDao;

    @Autowired
    private SessionFactory sessionFactory;

    private PreboardedExperiment experiment;
    private BibliographicReference paperA;
    private BibliographicReference paperB;

    @BeforeEach
    public void seed() {
        experiment = new PreboardedExperiment();
        experiment.setAccession( "GSE-pa-it-" + UUID.randomUUID() );
        experiment.setSource( "GEO" );
        experiment.setName( "PublicationAssociationIT experiment" );
        experiment.setWorkflowState( WorkflowState.Preboarded );
        sessionFactory.getCurrentSession().persist( experiment );

        paperA = persistReference( "38088204" );
        paperB = persistReference( "38165001" );
        sessionFactory.getCurrentSession().flush();
    }

    private BibliographicReference persistReference( String pubMedId ) {
        ExternalDatabase pubmed = ( ExternalDatabase ) sessionFactory.getCurrentSession()
                .createQuery( "from ExternalDatabase where name = :n" )
                .setParameter( "n", ExternalDatabases.PUBMED )
                .setMaxResults( 1 )
                .uniqueResult();
        if ( pubmed == null ) {
            pubmed = ExternalDatabase.Factory.newInstance();
            pubmed.setName( ExternalDatabases.PUBMED );
            sessionFactory.getCurrentSession().persist( pubmed );
        }
        DatabaseEntry accession = DatabaseEntry.Factory.newInstance();
        accession.setAccession( pubMedId + "-" + UUID.randomUUID() );
        accession.setExternalDatabase( pubmed );
        BibliographicReference ref = BibliographicReference.Factory.newInstance();
        ref.setTitle( "Paper " + pubMedId );
        ref.setPubAccession( accession );
        sessionFactory.getCurrentSession().persist( accession );
        sessionFactory.getCurrentSession().persist( ref );
        return ref;
    }

    private void flushAndClear() {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
    }

    @Test
    @DisplayName("every column round-trips, including the opaque evidence blob")
    public void acceptedAssertionRoundTrips() {
        publicationAssociationService.assertAccepted( experiment,
                new PublicationAssertion( paperA, PublicationAssociationSource.GEO_SUBMITTER_LINK,
                        "GEO !Series_pubmed_id", "[{\"quote\":\"pubmed 38088204\",\"source\":\"soft\"}]",
                        GOEvidenceCode.TAS, 0.5, "geo-import" ),
                PublicationAssociationRole.PRIMARY );
        flushAndClear();

        PublicationAssociation reloaded = publicationAssociationDao
                .findByInvestigationAndPublication( experiment, paperA );
        assertNotNull( reloaded );
        assertEquals( PublicationAssociationStatus.ACCEPTED, reloaded.getStatus() );
        assertEquals( PublicationAssociationRole.PRIMARY, reloaded.getRole() );
        assertEquals( PublicationAssociationSource.GEO_SUBMITTER_LINK, reloaded.getSource() );
        assertEquals( "GEO !Series_pubmed_id", reloaded.getEvidence() );
        assertEquals( "[{\"quote\":\"pubmed 38088204\",\"source\":\"soft\"}]", reloaded.getSupportingEvidence() );
        assertEquals( GOEvidenceCode.TAS, reloaded.getEvidenceCode() );
        assertEquals( 0.5, reloaded.getConfidence() );
        assertEquals( "geo-import", reloaded.getAssertedBy() );
        assertNotNull( reloaded.getAssertedAt() );
    }

    @Test
    @DisplayName("a curator's rejection refuses a later agent proposing the same paper")
    public void rejectionOutranksALaterAgent() {
        publicationAssociationService.assertRejected( experiment,
                new PublicationAssertion( paperA, PublicationAssociationSource.CURATOR,
                        "GEO links this, but the series title names a different paper by the same lab",
                        null, GOEvidenceCode.IC, null, "rachel" ) );
        flushAndClear();

        PublicationAssociationConflictException refused = assertThrows( PublicationAssociationConflictException.class,
                () -> publicationAssociationService.assertAccepted( experiment,
                        new PublicationAssertion( paperA, PublicationAssociationSource.AGENT ),
                        PublicationAssociationRole.PRIMARY ) );
        assertEquals( PublicationAssociationSource.CURATOR, refused.getStanding().getSource() );
        // The refusal quotes the standing evidence, so a writer that is turned away can say why
        // without going back to the database for it.
        assertTrue( refused.getMessage().contains( "the series title names a different paper" ),
                "the conflict should carry the curator's reason: " + refused.getMessage() );

        // Same refusal through the set-replace path, which is what the REST endpoint uses.
        assertThrows( PublicationAssociationConflictException.class,
                () -> publicationAssociationService.reconcile( experiment,
                        new PublicationAssertion( paperA, PublicationAssociationSource.GEO_SUBMITTER_LINK ),
                        Collections.emptyList(), Collections.emptyList() ) );

        flushAndClear();
        PublicationAssociation standing = publicationAssociationDao
                .findByInvestigationAndPublication( experiment, paperA );
        assertEquals( PublicationAssociationStatus.REJECTED, standing.getStatus(),
                "a refused write must leave the rejection exactly as it was" );
        assertNull( standing.getRole(), "a rejected row occupies no slot" );
    }

    @Test
    @DisplayName("a curator can overturn their own earlier rejection — the rule is rank, not immutability")
    public void curatorOverridesAnEarlierRejection() {
        publicationAssociationService.assertRejected( experiment,
                new PublicationAssertion( paperA, PublicationAssociationSource.CURATOR,
                        "looked like data reuse", null, GOEvidenceCode.IC, null, "rachel" ) );
        flushAndClear();

        publicationAssociationService.assertAccepted( experiment,
                new PublicationAssertion( paperA, PublicationAssociationSource.CURATOR,
                        "re-read it: this is the source paper after all", null, GOEvidenceCode.IC, null, "rachel" ),
                PublicationAssociationRole.PRIMARY );
        flushAndClear();

        PublicationAssociation reloaded = publicationAssociationDao
                .findByInvestigationAndPublication( experiment, paperA );
        assertEquals( PublicationAssociationStatus.ACCEPTED, reloaded.getStatus() );
        assertEquals( "re-read it: this is the source paper after all", reloaded.getEvidence() );
    }

    @Test
    @DisplayName("a lower authority re-asserting an accepted link does not rewrite its stated basis")
    public void lowerAuthorityDoesNotRestateEvidence() {
        publicationAssociationService.assertAccepted( experiment,
                new PublicationAssertion( paperB, PublicationAssociationSource.CURATOR,
                        "the paper cites this accession under Data Availability", null, GOEvidenceCode.IC, null, "rachel" ),
                PublicationAssociationRole.PRIMARY );
        flushAndClear();

        publicationAssociationService.assertAccepted( experiment,
                new PublicationAssertion( paperB, PublicationAssociationSource.GEO_SUBMITTER_LINK,
                        "GEO !Series_pubmed_id", null, GOEvidenceCode.TAS, null, "geo-import" ),
                PublicationAssociationRole.PRIMARY );
        flushAndClear();

        PublicationAssociation reloaded = publicationAssociationDao
                .findByInvestigationAndPublication( experiment, paperB );
        assertEquals( PublicationAssociationSource.CURATOR, reloaded.getSource(),
                "GEO must not take the credit for a link a curator reasoned out" );
        assertEquals( "the paper cites this accession under Data Availability", reloaded.getEvidence() );
    }

    @Test
    @DisplayName("reconcile retracts assertions for publications it no longer names")
    public void reconcileRetractsDroppedPublications() {
        publicationAssociationService.reconcile( experiment,
                new PublicationAssertion( paperA, PublicationAssociationSource.CURATOR ),
                Collections.singletonList( new PublicationAssertion( paperB, PublicationAssociationSource.CURATOR ) ),
                Collections.emptyList() );
        flushAndClear();
        assertEquals( 2, publicationAssociationService.findByInvestigation( experiment, null ).size() );

        // paperB drops out of the set entirely: the claim goes with it, rather than being left behind
        // as an ACCEPTED row describing a link that is gone.
        publicationAssociationService.reconcile( experiment,
                new PublicationAssertion( paperA, PublicationAssociationSource.CURATOR ),
                Collections.emptyList(), Collections.emptyList() );
        flushAndClear();

        List<PublicationAssociation> remaining = publicationAssociationService.findByInvestigation( experiment, null );
        assertEquals( 1, remaining.size() );
        assertEquals( paperA.getId(), remaining.get( 0 ).getPublication().getId() );
    }

    @Test
    @DisplayName("a reconcile that says nothing about rejections leaves the standing ones alone")
    public void nullRejectedLeavesStandingRejectionsAlone() {
        // GSE227854, in miniature. paperB is the primary a curator reasoned out; paperA is GEO's own
        // !Series_pubmed_id, ruled out because it names a different paper by the same lab.
        publicationAssociationService.reconcile( experiment,
                new PublicationAssertion( paperB, PublicationAssociationSource.CURATOR,
                        "the series title names this paper almost verbatim", null, GOEvidenceCode.IC, null, "rachel" ),
                Collections.emptyList(),
                Collections.singletonList( new PublicationAssertion( paperA, PublicationAssociationSource.CURATOR,
                        "GEO cross-linked the wrong one of the submitter's two papers", null, GOEvidenceCode.IC, null, "rachel" ) ) );
        flushAndClear();
        assertEquals( 1, publicationAssociationService
                .findByInvestigation( experiment, PublicationAssociationStatus.REJECTED ).size() );

        // A client re-sends the accepted set it just read back. It cannot have seen the rejection --
        // the read path hides rejections unless asked -- so its silence carries no information about
        // them and must not be read as a retraction.
        publicationAssociationService.reconcile( experiment,
                new PublicationAssertion( paperB, PublicationAssociationSource.CURATOR ),
                Collections.emptyList(), null );
        flushAndClear();

        List<PublicationAssociation> stillRejected = publicationAssociationService
                .findByInvestigation( experiment, PublicationAssociationStatus.REJECTED );
        assertEquals( 1, stillRejected.size(),
                "a write that never mentioned rejections must not delete one" );
        assertEquals( paperA.getId(), stillRejected.get( 0 ).getPublication().getId() );
        assertEquals( "GEO cross-linked the wrong one of the submitter's two papers",
                stillRejected.get( 0 ).getEvidence(), "and must not blank its reasoning either" );

        // The point of keeping it: the standing rejection is the only thing that refuses the next GEO
        // refresh. Deleting it above would let rank 30 install the wrong paper against no opposition.
        assertThrows( PublicationAssociationConflictException.class,
                () -> publicationAssociationService.assertAccepted( experiment,
                        new PublicationAssertion( paperA, PublicationAssociationSource.GEO_SUBMITTER_LINK ),
                        PublicationAssociationRole.PRIMARY ) );
    }

    @Test
    @DisplayName("an empty rejected list still clears them, so a curator can change their mind")
    public void emptyRejectedStillClearsStandingRejections() {
        publicationAssociationService.assertRejected( experiment,
                new PublicationAssertion( paperA, PublicationAssociationSource.CURATOR,
                        "wrong paper", null, GOEvidenceCode.IC, null, "rachel" ) );
        flushAndClear();

        // Present-but-empty is the caller speaking: it has considered the rejections and wants none.
        // Absent is the caller silent. Only the first retracts, or there would be no way back.
        publicationAssociationService.reconcile( experiment, null,
                Collections.emptyList(), Collections.emptyList() );
        flushAndClear();

        assertTrue( publicationAssociationService
                .findByInvestigation( experiment, PublicationAssociationStatus.REJECTED ).isEmpty() );
    }

    @Test
    @DisplayName("a publication cannot be accepted and rejected in the same request")
    public void acceptedAndRejectedTogetherIsRejected() {
        assertThrows( IllegalArgumentException.class,
                () -> publicationAssociationService.reconcile( experiment,
                        new PublicationAssertion( paperA, PublicationAssociationSource.CURATOR ),
                        Collections.emptyList(),
                        Collections.singletonList( new PublicationAssertion( paperA, PublicationAssociationSource.CURATOR ) ) ) );
    }

    @Test
    @DisplayName("the publication is fetch-joined, so the rows are safe to use after the session closes")
    public void publicationIsFetchJoined() {
        publicationAssociationService.assertRejected( experiment,
                new PublicationAssertion( paperA, PublicationAssociationSource.CURATOR,
                        "wrong paper", null, GOEvidenceCode.IC, null, "rachel" ) );
        flushAndClear();

        // gemma-rest has no open-session-in-view, and the read path builds a value object per rejected
        // publication after the service transaction has closed. If this join is ever dropped the
        // symptom is a LazyInitializationException in production and nowhere else, so pin it here.
        List<PublicationAssociation> rejected = publicationAssociationService
                .findByInvestigation( experiment, PublicationAssociationStatus.REJECTED );
        assertEquals( 1, rejected.size() );
        assertTrue( Hibernate.isInitialized( rejected.get( 0 ).getPublication() ),
                "the publication must come back initialized, not as a lazy proxy" );
        assertEquals( "Paper 38088204", rejected.get( 0 ).getPublication().getTitle() );
    }

    @Test
    @DisplayName("rebind moves assertions off a merged-away duplicate reference")
    public void rebindMovesAssertionsToTheCanonicalReference() {
        publicationAssociationService.assertAccepted( experiment,
                new PublicationAssertion( paperA, PublicationAssociationSource.CURATOR,
                        "read the paper", null, GOEvidenceCode.IC, null, "rachel" ),
                PublicationAssociationRole.PRIMARY );
        flushAndClear();

        // MergeDuplicateBibRefsCli repoints the experiment links onto the canonical row and then
        // deletes the duplicate. The FK from PUBLICATION_ASSOCIATION does not cascade, so an
        // assertion left behind turns that delete into a caught constraint violation and the merge
        // stops merging.
        assertEquals( 1, publicationAssociationService.rebindPublication( paperA, paperB ) );
        flushAndClear();

        assertNull( publicationAssociationDao.findByInvestigationAndPublication( experiment, paperA ) );
        PublicationAssociation moved = publicationAssociationDao
                .findByInvestigationAndPublication( experiment, paperB );
        assertNotNull( moved );
        assertEquals( "read the paper", moved.getEvidence(), "the evidence must survive the merge" );
    }

    @Test
    @DisplayName("rebind onto a reference the dataset already asserts drops the duplicate row")
    public void rebindDoesNotViolateTheUniqueKey() {
        publicationAssociationService.assertAccepted( experiment,
                new PublicationAssertion( paperA, PublicationAssociationSource.AGENT ),
                PublicationAssociationRole.OTHER_RELEVANT );
        publicationAssociationService.assertAccepted( experiment,
                new PublicationAssertion( paperB, PublicationAssociationSource.CURATOR,
                        "the surviving row's own record", null, GOEvidenceCode.IC, null, "rachel" ),
                PublicationAssociationRole.PRIMARY );
        flushAndClear();

        // Both rows are about the same investigation, so a naive UPDATE would collide on
        // UNIQUE(investigation, publication). The canonical reference's assertion is the one to keep.
        publicationAssociationService.rebindPublication( paperA, paperB );
        flushAndClear();

        assertEquals( 1, publicationAssociationService.findByInvestigation( experiment, null ).size() );
        PublicationAssociation survivor = publicationAssociationDao
                .findByInvestigationAndPublication( experiment, paperB );
        assertNotNull( survivor );
        assertEquals( PublicationAssociationSource.CURATOR, survivor.getSource() );
        assertEquals( "the surviving row's own record", survivor.getEvidence() );
    }

    @Test
    @DisplayName("rejections are queryable per dataset and across datasets")
    public void rejectionsAreQueryable() {
        publicationAssociationService.assertRejected( experiment,
                new PublicationAssertion( paperA, PublicationAssociationSource.CURATOR,
                        "wrong paper", null, GOEvidenceCode.IC, null, "rachel" ) );
        publicationAssociationService.assertAccepted( experiment,
                new PublicationAssertion( paperB, PublicationAssociationSource.CURATOR ),
                PublicationAssociationRole.PRIMARY );
        flushAndClear();

        List<PublicationAssociation> rejected = publicationAssociationService
                .findByInvestigation( experiment, PublicationAssociationStatus.REJECTED );
        assertEquals( 1, rejected.size() );
        assertEquals( paperA.getId(), rejected.get( 0 ).getPublication().getId() );

        List<PublicationAssociation> everywhere = publicationAssociationService.findRejections( paperA );
        assertEquals( 1, everywhere.size() );
        assertEquals( experiment.getId(), everywhere.get( 0 ).getInvestigation().getId() );
    }
}
