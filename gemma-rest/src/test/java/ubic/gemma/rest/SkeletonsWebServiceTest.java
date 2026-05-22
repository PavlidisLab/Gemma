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
package ubic.gemma.rest;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.expression.experiment.AgentProposal;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.SkeletonInvestigation;
import ubic.gemma.model.expression.experiment.WorkflowState;
import ubic.gemma.persistence.service.expression.experiment.AgentProposalService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SkeletonInvestigationService;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito unit tests for {@link SkeletonsWebService}. Exercises the
 * REST-shape contract: status codes, 409 mapping, idempotent 200 vs 201,
 * the queue-by-state 501 pointer.
 */
@ExtendWith(MockitoExtension.class)
public class SkeletonsWebServiceTest {

    @Mock
    private SkeletonInvestigationService skeletonService;
    @Mock
    private AgentProposalService agentProposalService;
    @Mock
    private ExpressionExperimentService expressionExperimentService;

    @InjectMocks
    private SkeletonsWebService webService;

    private SkeletonInvestigation skeleton;

    @BeforeEach
    public void setUp() {
        skeleton = new SkeletonInvestigation();
        skeleton.setId( 9876L );
        skeleton.setAccession( "GSE12345" );
        skeleton.setSource( "GEO" );
        skeleton.setWorkflowState( WorkflowState.Skeleton );
        skeleton.setWorkflowStateEnteredAt( new Date( 1_700_000_000_000L ) );
        lenient().when( skeletonService.load( 9876L ) ).thenReturn( skeleton );
    }

    @Test
    public void createSkeleton_freshAccessionReturns201() throws Exception {
        when( skeletonService.createSkeleton( eq( "GSE99" ), anyString(), any() ) )
                .thenReturn( skeleton );
        SkeletonsWebService.CreateSkeletonRequest req = new SkeletonsWebService.CreateSkeletonRequest();
        req.accession = "GSE99";
        req.source = "GEO";
        Response resp = webService.createSkeleton( req );
        assertThat( resp.getStatus() ).isEqualTo( 201 );
        assertThat( resp.getEntity() ).isInstanceOf( SkeletonsWebService.SkeletonResponse.class );
        SkeletonsWebService.SkeletonResponse body =
                ( SkeletonsWebService.SkeletonResponse ) resp.getEntity();
        assertThat( body.skeletonId ).isEqualTo( 9876L );
        assertThat( body.state ).isEqualTo( "Skeleton" );
    }

    @Test
    public void createSkeleton_missingBodyThrows400() {
        assertThatThrownBy( () -> webService.createSkeleton( null ) )
                .isInstanceOf( BadRequestException.class );
        SkeletonsWebService.CreateSkeletonRequest empty = new SkeletonsWebService.CreateSkeletonRequest();
        assertThatThrownBy( () -> webService.createSkeleton( empty ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void createSkeleton_existingAccessionReturns409WithIdAndType() throws Exception {
        when( skeletonService.createSkeleton( eq( "GSE12345" ), any(), any() ) )
                .thenThrow( new SkeletonInvestigationService.AccessionAlreadyExistsException(
                        "GSE12345", 9876L, "skeleton" ) );
        SkeletonsWebService.CreateSkeletonRequest req = new SkeletonsWebService.CreateSkeletonRequest();
        req.accession = "GSE12345";
        Response resp = webService.createSkeleton( req );
        assertThat( resp.getStatus() ).isEqualTo( 409 );
        @SuppressWarnings("unchecked")
        Map<String, Object> body = ( Map<String, Object> ) resp.getEntity();
        assertThat( body ).containsEntry( "existing_id", 9876L )
                .containsEntry( "existing_type", "skeleton" )
                .containsEntry( "accession", "GSE12345" );
    }

    @Test
    public void createSkeleton_existingEEAccessionReturns409WithEEType() throws Exception {
        when( skeletonService.createSkeleton( eq( "GSE12345" ), any(), any() ) )
                .thenThrow( new SkeletonInvestigationService.AccessionAlreadyExistsException(
                        "GSE12345", 1L, "expression_experiment" ) );
        SkeletonsWebService.CreateSkeletonRequest req = new SkeletonsWebService.CreateSkeletonRequest();
        req.accession = "GSE12345";
        Response resp = webService.createSkeleton( req );
        assertThat( resp.getStatus() ).isEqualTo( 409 );
        @SuppressWarnings("unchecked")
        Map<String, Object> body = ( Map<String, Object> ) resp.getEntity();
        assertThat( body ).containsEntry( "existing_type", "expression_experiment" );
    }

    @Test
    public void getSkeleton_returnsLatestProposalAndCount() {
        AgentProposal latest = new AgentProposal();
        latest.setId( 42L );
        latest.setRunId( "run-1" );
        latest.setInvestigation( skeleton );
        when( agentProposalService.findLatestByInvestigation( skeleton ) ).thenReturn( latest );
        when( agentProposalService.countByInvestigation( skeleton ) ).thenReturn( 3L );
        SkeletonsWebService.SkeletonResponse body = webService.getSkeleton( 9876L );
        assertThat( body.skeletonId ).isEqualTo( 9876L );
        assertThat( body.latestProposal ).isNotNull();
        assertThat( body.latestProposal.proposalId ).isEqualTo( 42L );
        assertThat( body.proposalCount ).isEqualTo( 3L );
    }

    @Test
    public void getSkeleton_unknownIdThrows404() {
        when( skeletonService.load( 99999L ) ).thenReturn( null );
        assertThatThrownBy( () -> webService.getSkeleton( 99999L ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void listOrResolveSkeletons_byAccessionReturnsSkeleton() {
        when( skeletonService.findByAccession( "GSE12345" ) ).thenReturn( skeleton );
        when( agentProposalService.findLatestByInvestigation( skeleton ) ).thenReturn( null );
        when( agentProposalService.countByInvestigation( skeleton ) ).thenReturn( 0L );
        Response resp = webService.listOrResolveSkeletons( "GSE12345", null );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
        assertThat( resp.getEntity() ).isInstanceOf( SkeletonsWebService.SkeletonResponse.class );
        assertThat( ( ( SkeletonsWebService.SkeletonResponse ) resp.getEntity() ).accession )
                .isEqualTo( "GSE12345" );
    }

    @Test
    public void listOrResolveSkeletons_byAccessionUnknownThrows404() {
        when( skeletonService.findByAccession( "GSEZ" ) ).thenReturn( null );
        assertThatThrownBy( () -> webService.listOrResolveSkeletons( "GSEZ", null ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void listOrResolveSkeletons_byStateReturns501PointerToWorkflowQueue() {
        Response resp = webService.listOrResolveSkeletons( null, "Skeleton" );
        assertThat( resp.getStatus() ).isEqualTo( 501 );
        @SuppressWarnings("unchecked")
        Map<String, Object> body = ( Map<String, Object> ) resp.getEntity();
        assertThat( body ).containsEntry( "state", "Skeleton" );
        assertThat( ( String ) body.get( "redirect_to" ) ).contains( "/workflow/queue" );
    }

    @Test
    public void listOrResolveSkeletons_neitherParamThrows400() {
        assertThatThrownBy( () -> webService.listOrResolveSkeletons( null, null ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void attachProposal_newRunIdReturns201() {
        AgentProposal p = new AgentProposal();
        p.setId( 100L );
        p.setRunId( "run-1" );
        p.setInvestigation( skeleton );
        when( agentProposalService.attach( eq( skeleton ), eq( "run-1" ), any(), any(), any(), any() ) )
                .thenReturn( new AgentProposalService.AttachedProposal( p, true ) );
        SkeletonsWebService.AttachProposalRequest req = new SkeletonsWebService.AttachProposalRequest();
        req.runId = "run-1";
        Response resp = webService.attachProposal( 9876L, req );
        assertThat( resp.getStatus() ).isEqualTo( 201 );
    }

    @Test
    public void attachProposal_existingRunIdReturns200() {
        AgentProposal p = new AgentProposal();
        p.setId( 100L );
        p.setRunId( "run-1" );
        p.setInvestigation( skeleton );
        when( agentProposalService.attach( eq( skeleton ), eq( "run-1" ), any(), any(), any(), any() ) )
                .thenReturn( new AgentProposalService.AttachedProposal( p, false ) );
        SkeletonsWebService.AttachProposalRequest req = new SkeletonsWebService.AttachProposalRequest();
        req.runId = "run-1";
        Response resp = webService.attachProposal( 9876L, req );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
    }

    @Test
    public void attachProposal_blankRunIdThrows400() {
        SkeletonsWebService.AttachProposalRequest req = new SkeletonsWebService.AttachProposalRequest();
        assertThatThrownBy( () -> webService.attachProposal( 9876L, req ) )
                .isInstanceOf( BadRequestException.class );
        req.runId = "  ";
        assertThatThrownBy( () -> webService.attachProposal( 9876L, req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void attachProposal_unknownSkeletonThrows404() {
        when( skeletonService.load( 1L ) ).thenReturn( null );
        SkeletonsWebService.AttachProposalRequest req = new SkeletonsWebService.AttachProposalRequest();
        req.runId = "run-1";
        assertThatThrownBy( () -> webService.attachProposal( 1L, req ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void promoteSkeleton_validReturns200WithCounts() throws Exception {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 555L );
        when( expressionExperimentService.load( 555L ) ).thenReturn( ee );
        when( skeletonService.promote( ee, skeleton ) )
                .thenReturn( new SkeletonInvestigationService.PromotionResult( 9876L, 555L, 3 ) );

        SkeletonsWebService.PromoteRequest req = new SkeletonsWebService.PromoteRequest();
        req.eeId = 555L;
        req.applyLatestProposal = true;
        Response resp = webService.promoteSkeleton( 9876L, req );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
        SkeletonsWebService.PromoteResponse body =
                ( SkeletonsWebService.PromoteResponse ) resp.getEntity();
        assertThat( body.skeletonId ).isEqualTo( 9876L );
        assertThat( body.eeId ).isEqualTo( 555L );
        assertThat( body.proposalsRebound ).isEqualTo( 3 );
        // apply_latest_proposal is forwarded but the server-side apply chain
        // is deferred; the response carries applied_proposal_id=null.
        assertThat( body.appliedProposalId ).isNull();
    }

    @Test
    public void promoteSkeleton_missingEeIdThrows400() {
        SkeletonsWebService.PromoteRequest req = new SkeletonsWebService.PromoteRequest();
        assertThatThrownBy( () -> webService.promoteSkeleton( 9876L, req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void promoteSkeleton_unknownEeThrows404() {
        when( expressionExperimentService.load( 999L ) ).thenReturn( null );
        SkeletonsWebService.PromoteRequest req = new SkeletonsWebService.PromoteRequest();
        req.eeId = 999L;
        assertThatThrownBy( () -> webService.promoteSkeleton( 9876L, req ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void promoteSkeleton_alreadyPromotedReturns409() throws Exception {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 555L );
        when( expressionExperimentService.load( 555L ) ).thenReturn( ee );
        when( skeletonService.promote( ee, skeleton ) )
                .thenThrow( new SkeletonInvestigationService.SkeletonAlreadyPromotedException( 9876L ) );
        SkeletonsWebService.PromoteRequest req = new SkeletonsWebService.PromoteRequest();
        req.eeId = 555L;
        Response resp = webService.promoteSkeleton( 9876L, req );
        assertThat( resp.getStatus() ).isEqualTo( 409 );
    }
}
