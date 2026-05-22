/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDraft;
import ubic.gemma.model.expression.experiment.AgentProposal;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mockito unit tests for {@link CurationDraftServiceImpl}: the find / upsert /
 * seed / delete / finalize surface, snapshot-capture invariants, and the
 * proposal-rebind path.
 */
@ExtendWith(MockitoExtension.class)
public class CurationDraftServiceTest {

    @Mock
    private CurationDraftDao curationDraftDao;
    @Mock
    private SessionFactory sessionFactory;
    @Mock
    private Session session;

    private CurationDraftServiceImpl service;

    private ExpressionExperiment ee;
    private User curator;

    @BeforeEach
    public void setUp() {
        service = new CurationDraftServiceImpl( curationDraftDao, sessionFactory );
        lenient().when( sessionFactory.getCurrentSession() ).thenReturn( session );
        ee = new ExpressionExperiment();
        ee.setId( 100L );
        curator = new User();
        curator.setId( 7L );
        lenient().when( session.get( Investigation.class, 100L ) ).thenReturn( ee );
    }

    @Test
    public void findForCurator_returnsExistingDraft() {
        CurationDraft draft = new CurationDraft();
        draft.setId( 1L );
        when( curationDraftDao.findByInvestigationAndCurator( ee, curator ) ).thenReturn( draft );
        Optional<CurationDraft> out = service.findForCurator( 100L, curator );
        assertThat( out ).isPresent();
        assertThat( out.get().getId() ).isEqualTo( 1L );
    }

    @Test
    public void findForCurator_emptyWhenNoInvestigation() {
        when( session.get( Investigation.class, 999L ) ).thenReturn( null );
        assertThat( service.findForCurator( 999L, curator ) ).isEmpty();
    }

    @Test
    public void saveOrUpdate_createsWhenNoneExists() {
        when( curationDraftDao.findByInvestigationAndCurator( ee, curator ) ).thenReturn( null );
        when( curationDraftDao.create( any( CurationDraft.class ) ) )
                .thenAnswer( inv -> { CurationDraft d = inv.getArgument( 0 ); d.setId( 11L ); return d; } );
        CurationDraft d = service.saveOrUpdate( 100L, curator, "{\"a\":1}", null, null );
        assertThat( d.getId() ).isEqualTo( 11L );
        assertThat( d.getStartedAt() ).isNotNull();
        assertThat( d.getLastEditedAt() ).isNotNull();
        assertThat( d.getPayloadJson() ).isEqualTo( "{\"a\":1}" );
        assertThat( d.getProposal() ).isNull();
        verify( curationDraftDao, never() ).update( any( CurationDraft.class ) );
    }

    @Test
    public void saveOrUpdate_updatesWhenExists_preservesStartedAt_bumpsLastEdited() {
        Date original = new Date( 1_700_000_000_000L );
        CurationDraft existing = new CurationDraft();
        existing.setId( 42L );
        existing.setInvestigation( ee );
        existing.setCurator( curator );
        existing.setStartedAt( original );
        existing.setLastEditedAt( original );
        existing.setPayloadJson( "{\"old\":true}" );
        when( curationDraftDao.findByInvestigationAndCurator( ee, curator ) ).thenReturn( existing );
        CurationDraft d = service.saveOrUpdate( 100L, curator, "{\"new\":true}", null, "[\"factor:1:0\"]" );
        assertThat( d.getId() ).isEqualTo( 42L );
        assertThat( d.getStartedAt() ).isEqualTo( original );
        assertThat( d.getLastEditedAt() ).isAfter( original );
        assertThat( d.getPayloadJson() ).isEqualTo( "{\"new\":true}" );
        assertThat( d.getParkedElements() ).isEqualTo( "[\"factor:1:0\"]" );
        verify( curationDraftDao ).update( existing );
        verify( curationDraftDao, never() ).create( any( CurationDraft.class ) );
    }

    @Test
    public void saveOrUpdate_capturesSnapshotOnFirstProposalBinding() {
        AgentProposal p = new AgentProposal();
        p.setId( 9L );
        p.setPayloadJson( "{\"proposed\":\"thing\"}" );
        when( curationDraftDao.findByInvestigationAndCurator( ee, curator ) ).thenReturn( null );
        when( session.get( AgentProposal.class, 9L ) ).thenReturn( p );
        when( curationDraftDao.create( any( CurationDraft.class ) ) )
                .thenAnswer( inv -> inv.getArgument( 0 ) );
        CurationDraft d = service.saveOrUpdate( 100L, curator, "{\"wip\":true}", 9L, null );
        assertThat( d.getProposal() ).isSameAs( p );
        assertThat( d.getProposalSnapshotJson() ).isEqualTo( "{\"proposed\":\"thing\"}" );
    }

    @Test
    public void saveOrUpdate_doesNotReplaceSnapshotOnSameProposalRebind() {
        AgentProposal p = new AgentProposal();
        p.setId( 9L );
        p.setPayloadJson( "{\"newer\":\"version\"}" );
        CurationDraft existing = new CurationDraft();
        existing.setId( 1L );
        existing.setInvestigation( ee );
        existing.setCurator( curator );
        existing.setStartedAt( new Date() );
        existing.setLastEditedAt( new Date() );
        existing.setProposal( p );
        existing.setProposalSnapshotJson( "{\"original\":\"snapshot\"}" );
        when( curationDraftDao.findByInvestigationAndCurator( ee, curator ) ).thenReturn( existing );
        CurationDraft d = service.saveOrUpdate( 100L, curator, "{\"wip\":2}", 9L, null );
        // Snapshot baseline preserved — we don't keep rewriting it on every save.
        assertThat( d.getProposalSnapshotJson() ).isEqualTo( "{\"original\":\"snapshot\"}" );
        // And we should not have re-fetched the proposal from the session.
        verify( session, never() ).get( eq( AgentProposal.class ), any() );
    }

    @Test
    public void seedFromProposal_alwaysCapturesSnapshotEvenIfDraftAlreadyHasOne() {
        AgentProposal p = new AgentProposal();
        p.setId( 11L );
        p.setPayloadJson( "{\"v\":2}" );
        CurationDraft existing = new CurationDraft();
        existing.setId( 1L );
        existing.setInvestigation( ee );
        existing.setCurator( curator );
        existing.setStartedAt( new Date( 100 ) );
        existing.setLastEditedAt( new Date( 100 ) );
        existing.setProposalSnapshotJson( "{\"v\":1}" );
        when( curationDraftDao.findByInvestigationAndCurator( ee, curator ) ).thenReturn( existing );
        CurationDraft d = service.seedFromProposal( 100L, curator, p, "{\"wip\":true}" );
        assertThat( d.getProposalSnapshotJson() ).isEqualTo( "{\"v\":2}" );
        assertThat( d.getProposal() ).isSameAs( p );
    }

    @Test
    public void delete_removesExistingDraft() {
        CurationDraft draft = new CurationDraft();
        draft.setId( 1L );
        when( curationDraftDao.findByInvestigationAndCurator( ee, curator ) ).thenReturn( draft );
        service.delete( 100L, curator );
        verify( curationDraftDao ).remove( draft );
    }

    @Test
    public void delete_noopWhenNoneExists() {
        when( curationDraftDao.findByInvestigationAndCurator( ee, curator ) ).thenReturn( null );
        service.delete( 100L, curator );
        verify( curationDraftDao, never() ).remove( any( CurationDraft.class ) );
    }

    @Test
    public void finalize_stampsFinalizedAt() {
        CurationDraft existing = new CurationDraft();
        existing.setId( 1L );
        existing.setInvestigation( ee );
        existing.setCurator( curator );
        existing.setStartedAt( new Date( 100 ) );
        existing.setLastEditedAt( new Date( 100 ) );
        when( curationDraftDao.findByInvestigationAndCurator( ee, curator ) ).thenReturn( existing );
        CurationDraft d = service.finalize( 100L, curator );
        assertThat( d.getFinalizedAt() ).isNotNull();
        verify( curationDraftDao ).update( existing );
    }

    @Test
    public void finalize_throwsWhenNoDraftExists() {
        when( curationDraftDao.findByInvestigationAndCurator( ee, curator ) ).thenReturn( null );
        assertThatThrownBy( () -> service.finalize( 100L, curator ) )
                .isInstanceOf( IllegalStateException.class );
    }

    @Test
    public void findByCurator_delegates() {
        CurationDraft a = new CurationDraft();
        a.setId( 1L );
        when( curationDraftDao.findByCurator( curator, null, 0, 10 ) ).thenReturn( List.of( a ) );
        List<CurationDraft> out = service.findByCurator( curator, null, 0, 10 );
        assertThat( out ).hasSize( 1 );
        verify( curationDraftDao, times( 1 ) ).findByCurator( curator, null, 0, 10 );
    }

    @Test
    public void findByProposal_delegates() {
        when( curationDraftDao.findByProposal( 9L ) ).thenReturn( List.of() );
        assertThat( service.findByProposal( 9L ) ).isEmpty();
    }
}
