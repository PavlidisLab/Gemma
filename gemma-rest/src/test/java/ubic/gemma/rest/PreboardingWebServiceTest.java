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
import ubic.gemma.model.expression.experiment.PreboardingExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;
import ubic.gemma.persistence.service.expression.experiment.AgentProposalService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.PreboardingExperimentService;

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
 * Pure-Mockito unit tests for {@link PreboardingWebService}. Exercises the
 * REST-shape contract: status codes, 409 mapping, idempotent 200 vs 201,
 * the queue-by-state 501 pointer.
 */
@ExtendWith(MockitoExtension.class)
public class PreboardingWebServiceTest {

    @Mock
    private PreboardingExperimentService preboardingService;
    @Mock
    private AgentProposalService agentProposalService;
    @Mock
    private ExpressionExperimentService expressionExperimentService;

    @InjectMocks
    private PreboardingWebService webService;

    private PreboardingExperiment preboarding;

    @BeforeEach
    public void setUp() {
        preboarding = new PreboardingExperiment();
        preboarding.setId( 9876L );
        preboarding.setAccession( "GSE12345" );
        preboarding.setSource( "GEO" );
        preboarding.setWorkflowState( WorkflowState.Preboarding );
        preboarding.setWorkflowStateEnteredAt( new Date( 1_700_000_000_000L ) );
        lenient().when( preboardingService.load( 9876L ) ).thenReturn( preboarding );
    }

    @Test
    public void createPreboarding_freshAccessionReturns201() throws Exception {
        when( preboardingService.createPreboarding( eq( "GSE99" ), anyString(), any() ) )
                .thenReturn( preboarding );
        PreboardingWebService.CreatePreboardingRequest req = new PreboardingWebService.CreatePreboardingRequest();
        req.accession = "GSE99";
        req.source = "GEO";
        Response resp = webService.createPreboarding( req );
        assertThat( resp.getStatus() ).isEqualTo( 201 );
        assertThat( resp.getEntity() ).isInstanceOf( PreboardingWebService.PreboardingResponse.class );
        PreboardingWebService.PreboardingResponse body =
                ( PreboardingWebService.PreboardingResponse ) resp.getEntity();
        assertThat( body.preboardingId ).isEqualTo( 9876L );
        assertThat( body.state ).isEqualTo( "Preboarding" );
    }

    @Test
    public void createPreboarding_missingBodyThrows400() {
        assertThatThrownBy( () -> webService.createPreboarding( null ) )
                .isInstanceOf( BadRequestException.class );
        PreboardingWebService.CreatePreboardingRequest empty = new PreboardingWebService.CreatePreboardingRequest();
        assertThatThrownBy( () -> webService.createPreboarding( empty ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void createPreboarding_existingAccessionReturns409WithIdAndType() throws Exception {
        when( preboardingService.createPreboarding( eq( "GSE12345" ), any(), any() ) )
                .thenThrow( new PreboardingExperimentService.AccessionAlreadyExistsException(
                        "GSE12345", 9876L, "preboarding" ) );
        PreboardingWebService.CreatePreboardingRequest req = new PreboardingWebService.CreatePreboardingRequest();
        req.accession = "GSE12345";
        Response resp = webService.createPreboarding( req );
        assertThat( resp.getStatus() ).isEqualTo( 409 );
        @SuppressWarnings("unchecked")
        Map<String, Object> body = ( Map<String, Object> ) resp.getEntity();
        assertThat( body ).containsEntry( "existing_id", 9876L )
                .containsEntry( "existing_type", "preboarding" )
                .containsEntry( "accession", "GSE12345" );
    }

    @Test
    public void createPreboarding_existingEEAccessionReturns409WithEEType() throws Exception {
        when( preboardingService.createPreboarding( eq( "GSE12345" ), any(), any() ) )
                .thenThrow( new PreboardingExperimentService.AccessionAlreadyExistsException(
                        "GSE12345", 1L, "expression_experiment" ) );
        PreboardingWebService.CreatePreboardingRequest req = new PreboardingWebService.CreatePreboardingRequest();
        req.accession = "GSE12345";
        Response resp = webService.createPreboarding( req );
        assertThat( resp.getStatus() ).isEqualTo( 409 );
        @SuppressWarnings("unchecked")
        Map<String, Object> body = ( Map<String, Object> ) resp.getEntity();
        assertThat( body ).containsEntry( "existing_type", "expression_experiment" );
    }

    @Test
    public void getPreboarding_returnsLatestProposalAndCount() {
        AgentProposal latest = new AgentProposal();
        latest.setId( 42L );
        latest.setRunId( "run-1" );
        latest.setInvestigation( preboarding );
        when( agentProposalService.findLatestByInvestigation( preboarding ) ).thenReturn( latest );
        when( agentProposalService.countByInvestigation( preboarding ) ).thenReturn( 3L );
        PreboardingWebService.PreboardingResponse body = webService.getPreboarding( 9876L );
        assertThat( body.preboardingId ).isEqualTo( 9876L );
        assertThat( body.latestProposal ).isNotNull();
        assertThat( body.latestProposal.proposalId ).isEqualTo( 42L );
        assertThat( body.proposalCount ).isEqualTo( 3L );
    }

    @Test
    public void getPreboarding_unknownIdThrows404() {
        when( preboardingService.load( 99999L ) ).thenReturn( null );
        assertThatThrownBy( () -> webService.getPreboarding( 99999L ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void listOrResolvePreboarding_byAccessionReturnsPreboarding() {
        when( preboardingService.findByAccession( "GSE12345" ) ).thenReturn( preboarding );
        when( agentProposalService.findLatestByInvestigation( preboarding ) ).thenReturn( null );
        when( agentProposalService.countByInvestigation( preboarding ) ).thenReturn( 0L );
        Response resp = webService.listOrResolvePreboarding( "GSE12345", null );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
        assertThat( resp.getEntity() ).isInstanceOf( PreboardingWebService.PreboardingResponse.class );
        assertThat( ( ( PreboardingWebService.PreboardingResponse ) resp.getEntity() ).accession )
                .isEqualTo( "GSE12345" );
    }

    @Test
    public void listOrResolvePreboarding_byAccessionUnknownThrows404() {
        when( preboardingService.findByAccession( "GSEZ" ) ).thenReturn( null );
        assertThatThrownBy( () -> webService.listOrResolvePreboarding( "GSEZ", null ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void listOrResolvePreboarding_byStateReturns501PointerToWorkflowQueue() {
        Response resp = webService.listOrResolvePreboarding( null, "Preboarding" );
        assertThat( resp.getStatus() ).isEqualTo( 501 );
        @SuppressWarnings("unchecked")
        Map<String, Object> body = ( Map<String, Object> ) resp.getEntity();
        assertThat( body ).containsEntry( "state", "Preboarding" );
        assertThat( ( String ) body.get( "redirect_to" ) ).contains( "/workflow/queue" );
    }

    @Test
    public void listOrResolvePreboarding_neitherParamThrows400() {
        assertThatThrownBy( () -> webService.listOrResolvePreboarding( null, null ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void attachProposal_newRunIdReturns201() {
        AgentProposal p = new AgentProposal();
        p.setId( 100L );
        p.setRunId( "run-1" );
        p.setInvestigation( preboarding );
        when( agentProposalService.attach( eq( preboarding ), eq( "run-1" ), any(), any(), any(), any() ) )
                .thenReturn( new AgentProposalService.AttachedProposal( p, true ) );
        PreboardingWebService.AttachProposalRequest req = new PreboardingWebService.AttachProposalRequest();
        req.runId = "run-1";
        Response resp = webService.attachProposal( 9876L, req );
        assertThat( resp.getStatus() ).isEqualTo( 201 );
    }

    @Test
    public void attachProposal_existingRunIdReturns200() {
        AgentProposal p = new AgentProposal();
        p.setId( 100L );
        p.setRunId( "run-1" );
        p.setInvestigation( preboarding );
        when( agentProposalService.attach( eq( preboarding ), eq( "run-1" ), any(), any(), any(), any() ) )
                .thenReturn( new AgentProposalService.AttachedProposal( p, false ) );
        PreboardingWebService.AttachProposalRequest req = new PreboardingWebService.AttachProposalRequest();
        req.runId = "run-1";
        Response resp = webService.attachProposal( 9876L, req );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
    }

    @Test
    public void attachProposal_blankRunIdThrows400() {
        PreboardingWebService.AttachProposalRequest req = new PreboardingWebService.AttachProposalRequest();
        assertThatThrownBy( () -> webService.attachProposal( 9876L, req ) )
                .isInstanceOf( BadRequestException.class );
        req.runId = "  ";
        assertThatThrownBy( () -> webService.attachProposal( 9876L, req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void attachProposal_unknownPreboardingThrows404() {
        when( preboardingService.load( 1L ) ).thenReturn( null );
        PreboardingWebService.AttachProposalRequest req = new PreboardingWebService.AttachProposalRequest();
        req.runId = "run-1";
        assertThatThrownBy( () -> webService.attachProposal( 1L, req ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void promotePreboarding_validReturns200WithCounts() throws Exception {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 555L );
        when( expressionExperimentService.load( 555L ) ).thenReturn( ee );
        when( preboardingService.promote( ee, preboarding ) )
                .thenReturn( new PreboardingExperimentService.PromotionResult( 9876L, 555L, 3 ) );

        PreboardingWebService.PromoteRequest req = new PreboardingWebService.PromoteRequest();
        req.eeId = 555L;
        req.applyLatestProposal = true;
        Response resp = webService.promotePreboarding( 9876L, req );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
        PreboardingWebService.PromoteResponse body =
                ( PreboardingWebService.PromoteResponse ) resp.getEntity();
        assertThat( body.preboardingId ).isEqualTo( 9876L );
        assertThat( body.eeId ).isEqualTo( 555L );
        assertThat( body.proposalsRebound ).isEqualTo( 3 );
        // apply_latest_proposal is forwarded but the server-side apply chain
        // is deferred; the response carries applied_proposal_id=null.
        assertThat( body.appliedProposalId ).isNull();
    }

    @Test
    public void promotePreboarding_missingEeIdThrows400() {
        PreboardingWebService.PromoteRequest req = new PreboardingWebService.PromoteRequest();
        assertThatThrownBy( () -> webService.promotePreboarding( 9876L, req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void promotePreboarding_unknownEeThrows404() {
        when( expressionExperimentService.load( 999L ) ).thenReturn( null );
        PreboardingWebService.PromoteRequest req = new PreboardingWebService.PromoteRequest();
        req.eeId = 999L;
        assertThatThrownBy( () -> webService.promotePreboarding( 9876L, req ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void promotePreboarding_alreadyPromotedReturns409() throws Exception {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 555L );
        when( expressionExperimentService.load( 555L ) ).thenReturn( ee );
        when( preboardingService.promote( ee, preboarding ) )
                .thenThrow( new PreboardingExperimentService.PreboardingAlreadyPromotedException( 9876L ) );
        PreboardingWebService.PromoteRequest req = new PreboardingWebService.PromoteRequest();
        req.eeId = 555L;
        Response resp = webService.promotePreboarding( 9876L, req );
        assertThat( resp.getStatus() ).isEqualTo( 409 );
    }
}
