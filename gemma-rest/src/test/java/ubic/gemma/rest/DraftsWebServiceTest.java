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
package ubic.gemma.rest;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.security.authentication.UserReadService;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDraft;
import ubic.gemma.model.expression.experiment.AgentProposal;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.CurationDraftService;
import ubic.gemma.persistence.service.expression.experiment.AgentProposalService;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito tests for {@link DraftsWebService}: the REST shape contract
 * across both URL families and the reviewer-resolution + 403 path.
 */
@ExtendWith(MockitoExtension.class)
public class DraftsWebServiceTest {

    @Mock
    private CurationDraftService curationDraftService;
    @Mock
    private AgentProposalService agentProposalService;
    @Mock
    private UserManager userManager;
    @Mock
    private UserReadService userReadService;

    @InjectMocks
    private DraftsWebService webService;

    private User currentUser;
    private User otherCurator;
    private ExpressionExperiment ee;
    private AgentProposal proposal;

    @BeforeEach
    public void setUp() {
        currentUser = new User();
        currentUser.setId( 7L );
        otherCurator = new User();
        otherCurator.setId( 8L );
        ee = new ExpressionExperiment();
        ee.setId( 100L );
        proposal = new AgentProposal();
        proposal.setId( 42L );
        proposal.setRunId( "run-1" );
        proposal.setInvestigation( ee );
        proposal.setPayloadJson( "{\"factor:1:0\":{\"name\":\"sex\"}}" );
        lenient().when( userManager.getCurrentUser() ).thenReturn( currentUser );
        // Default auth context: curator without admin authority.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken( "curator", "x",
                        Collections.singletonList( new SimpleGrantedAuthority( "GROUP_CURATOR" ) ) ) );
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /* ===== /datasets/{id}/draft family ===== */

    @Test
    public void getDraftForDataset_returnsExistingDraft() {
        CurationDraft d = draftOf( 1L, ee, currentUser );
        when( curationDraftService.findForCurator( 100L, currentUser ) ).thenReturn( Optional.of( d ) );
        DraftsWebService.DraftResponse body = webService.getDraftForDataset( 100L );
        assertThat( body.draftId ).isEqualTo( 1L );
        assertThat( body.investigationId ).isEqualTo( 100L );
    }

    @Test
    public void getDraftForDataset_missingThrows404() {
        when( curationDraftService.findForCurator( 100L, currentUser ) ).thenReturn( Optional.empty() );
        assertThatThrownBy( () -> webService.getDraftForDataset( 100L ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void upsertDraftForDataset_freshDraftReturns201() {
        when( curationDraftService.findForCurator( 100L, currentUser ) ).thenReturn( Optional.empty() );
        when( curationDraftService.saveOrUpdate( eq( 100L ), eq( currentUser ),
                eq( "{\"wip\":1}" ), any(), any() ) )
                .thenReturn( draftOf( 1L, ee, currentUser ) );
        DraftsWebService.UpsertDraftRequest req = new DraftsWebService.UpsertDraftRequest();
        req.payloadJson = "{\"wip\":1}";
        Response resp = webService.upsertDraftForDataset( 100L, req );
        assertThat( resp.getStatus() ).isEqualTo( 201 );
    }

    @Test
    public void upsertDraftForDataset_existingDraftReturns200() {
        CurationDraft existing = draftOf( 1L, ee, currentUser );
        when( curationDraftService.findForCurator( 100L, currentUser ) ).thenReturn( Optional.of( existing ) );
        when( curationDraftService.saveOrUpdate( eq( 100L ), eq( currentUser ),
                eq( "{\"wip\":2}" ), any(), any() ) )
                .thenReturn( existing );
        DraftsWebService.UpsertDraftRequest req = new DraftsWebService.UpsertDraftRequest();
        req.payloadJson = "{\"wip\":2}";
        Response resp = webService.upsertDraftForDataset( 100L, req );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
    }

    @Test
    public void upsertDraftForDataset_missingBodyThrows400() {
        assertThatThrownBy( () -> webService.upsertDraftForDataset( 100L, null ) )
                .isInstanceOf( BadRequestException.class );
        DraftsWebService.UpsertDraftRequest req = new DraftsWebService.UpsertDraftRequest();
        assertThatThrownBy( () -> webService.upsertDraftForDataset( 100L, req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void deleteDraftForDataset_returns204() {
        Response resp = webService.deleteDraftForDataset( 100L );
        assertThat( resp.getStatus() ).isEqualTo( 204 );
        verify( curationDraftService ).delete( 100L, currentUser );
    }

    @Test
    public void finalizeDraftForDataset_stampsFinalizedAt() {
        CurationDraft d = draftOf( 1L, ee, currentUser );
        d.setFinalizedAt( new Date() );
        when( curationDraftService.finalize( 100L, currentUser ) ).thenReturn( d );
        DraftsWebService.DraftResponse body = webService.finalizeDraftForDataset( 100L );
        assertThat( body.finalizedAt ).isNotNull();
    }

    @Test
    public void finalizeDraftForDataset_noDraftThrows404() {
        when( curationDraftService.finalize( 100L, currentUser ) )
                .thenThrow( new IllegalStateException( "No draft" ) );
        assertThatThrownBy( () -> webService.finalizeDraftForDataset( 100L ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void commitDraftForDataset_returns501WithPointer() {
        CurationDraft d = draftOf( 1L, ee, currentUser );
        when( curationDraftService.findForCurator( 100L, currentUser ) ).thenReturn( Optional.of( d ) );
        Response resp = webService.commitDraftForDataset( 100L );
        assertThat( resp.getStatus() ).isEqualTo( 501 );
        @SuppressWarnings("unchecked")
        Map<String, Object> body = ( Map<String, Object> ) resp.getEntity();
        assertThat( body ).containsEntry( "draft_id", 1L );
        assertThat( body ).containsKey( "redirect_to" );
    }

    /* ===== /drafts family ===== */

    @Test
    public void listMyDrafts_paginates() {
        when( curationDraftService.findByCurator( eq( currentUser ), any(), eq( 0 ), eq( 50 ) ) )
                .thenReturn( List.of( draftOf( 1L, ee, currentUser ) ) );
        List<DraftsWebService.DraftResponse> out = webService.listMyDrafts( null, null, null );
        assertThat( out ).hasSize( 1 );
    }

    @Test
    public void listMyInflightDrafts_filtersOutFinalized() {
        CurationDraft live = draftOf( 1L, ee, currentUser );
        CurationDraft done = draftOf( 2L, ee, currentUser );
        done.setFinalizedAt( new Date() );
        when( curationDraftService.findByCurator( eq( currentUser ), any(), eq( 0 ), eq( 0 ) ) )
                .thenReturn( List.of( live, done ) );
        List<DraftsWebService.DraftResponse> out = webService.listMyInflightDrafts();
        assertThat( out ).hasSize( 1 );
        assertThat( out.get( 0 ).draftId ).isEqualTo( 1L );
    }

    /* ===== /proposals/{id}/reviews family ===== */

    @Test
    public void listReviewsOfProposal_returnsAllReviewerDrafts() {
        when( agentProposalService.load( 42L ) ).thenReturn( proposal );
        when( curationDraftService.findByProposal( 42L ) ).thenReturn( List.of(
                draftOf( 1L, ee, currentUser ),
                draftOf( 2L, ee, otherCurator ) ) );
        List<DraftsWebService.DraftResponse> out = webService.listReviewsOfProposal( 42L );
        assertThat( out ).hasSize( 2 );
    }

    @Test
    public void listReviewsOfProposal_unknownProposalThrows404() {
        when( agentProposalService.load( 999L ) ).thenReturn( null );
        assertThatThrownBy( () -> webService.listReviewsOfProposal( 999L ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void getReviewOfProposal_selfReturnsReviewWithDispositions() {
        CurationDraft d = draftOf( 1L, ee, currentUser );
        d.setProposalSnapshotJson( "{\"factor:1:0\":{\"name\":\"sex\"}}" );
        d.setPayloadJson( "{\"factor:1:0\":{\"name\":\"biological_sex\"}}" );
        when( agentProposalService.load( 42L ) ).thenReturn( proposal );
        when( curationDraftService.findForCurator( 100L, currentUser ) ).thenReturn( Optional.of( d ) );
        DraftsWebService.ReviewResponse body = webService.getReviewOfProposal( 42L, currentUser.getId() );
        assertThat( body.draft.draftId ).isEqualTo( 1L );
        // Snapshot vs payload differ -> EDITED.
        assertThat( body.dispositions ).isNotEmpty();
    }

    @Test
    public void getReviewOfProposal_crossCuratorNonAdminThrows403() {
        when( agentProposalService.load( 42L ) ).thenReturn( proposal );
        assertThatThrownBy( () -> webService.getReviewOfProposal( 42L, otherCurator.getId() ) )
                .isInstanceOf( ForbiddenException.class );
    }

    @Test
    public void getReviewOfProposal_crossCuratorWithAdminPasses() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken( "admin", "x",
                        Collections.singletonList( new SimpleGrantedAuthority( "GROUP_ADMIN" ) ) ) );
        when( agentProposalService.load( 42L ) ).thenReturn( proposal );
        when( userReadService.load( otherCurator.getId() ) ).thenReturn( otherCurator );
        when( curationDraftService.findForCurator( 100L, otherCurator ) )
                .thenReturn( Optional.of( draftOf( 9L, ee, otherCurator ) ) );
        DraftsWebService.ReviewResponse body =
                webService.getReviewOfProposal( 42L, otherCurator.getId() );
        assertThat( body.draft.draftId ).isEqualTo( 9L );
    }

    @Test
    public void patchReviewOfProposal_seedsWhenAbsent() {
        when( agentProposalService.load( 42L ) ).thenReturn( proposal );
        when( curationDraftService.findForCurator( 100L, currentUser ) ).thenReturn( Optional.empty() );
        CurationDraft seeded = draftOf( 1L, ee, currentUser );
        when( curationDraftService.seedFromProposal( eq( 100L ), eq( currentUser ),
                eq( proposal ), any() ) ).thenReturn( seeded );
        DraftsWebService.PatchReviewRequest req = new DraftsWebService.PatchReviewRequest();
        req.payloadJson = "{\"v\":2}";
        DraftsWebService.ReviewResponse body = webService.patchReviewOfProposal( 42L,
                currentUser.getId(), req );
        assertThat( body.draft.draftId ).isEqualTo( 1L );
    }

    @Test
    public void finalizeReviewOfProposal_stampsThroughInvestigation() {
        CurationDraft d = draftOf( 1L, ee, currentUser );
        d.setFinalizedAt( new Date() );
        when( agentProposalService.load( 42L ) ).thenReturn( proposal );
        when( curationDraftService.finalize( 100L, currentUser ) ).thenReturn( d );
        DraftsWebService.ReviewResponse body = webService.finalizeReviewOfProposal( 42L,
                currentUser.getId() );
        assertThat( body.draft.finalizedAt ).isNotNull();
    }

    /* ===== helpers ===== */

    private static CurationDraft draftOf( Long id, Investigation inv, User curator ) {
        CurationDraft d = new CurationDraft();
        d.setId( id );
        d.setInvestigation( inv );
        d.setCurator( curator );
        d.setPayloadJson( "{}" );
        d.setStartedAt( new Date() );
        d.setLastEditedAt( new Date() );
        return d;
    }
}
