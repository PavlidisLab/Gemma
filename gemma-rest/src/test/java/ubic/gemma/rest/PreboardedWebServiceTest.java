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
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.AgentProposal;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.PreboardedExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;
import ubic.gemma.persistence.service.expression.experiment.AgentProposalService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.PreboardedExperimentService;

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
 * Pure-Mockito unit tests for {@link PreboardedWebService}. Exercises the
 * REST-shape contract: status codes, 409 mapping, idempotent 200 vs 201,
 * the queue-by-state 501 pointer.
 */
@ExtendWith(MockitoExtension.class)
public class PreboardedWebServiceTest {

    @Mock
    private PreboardedExperimentService preboardedService;
    @Mock
    private AgentProposalService agentProposalService;
    @Mock
    private ExpressionExperimentService expressionExperimentService;

    @InjectMocks
    private PreboardedWebService webService;

    private PreboardedExperiment preboarded;

    @BeforeEach
    public void setUp() {
        preboarded = new PreboardedExperiment();
        preboarded.setId( 9876L );
        preboarded.setAccession( "GSE12345" );
        preboarded.setSource( "GEO" );
        preboarded.setWorkflowState( WorkflowState.Preboarded );
        preboarded.setWorkflowStateEnteredAt( new Date( 1_700_000_000_000L ) );
        lenient().when( preboardedService.load( 9876L ) ).thenReturn( preboarded );
    }

    @Test
    public void createPreboarded_freshAccessionReturns201() throws Exception {
        when( preboardedService.createPreboarded( eq( "GSE99" ), anyString(), any() ) )
                .thenReturn( preboarded );
        PreboardedWebService.CreatePreboardedRequest req = new PreboardedWebService.CreatePreboardedRequest();
        req.accession = "GSE99";
        req.source = "GEO";
        Response resp = webService.createPreboarded( req );
        assertThat( resp.getStatus() ).isEqualTo( 201 );
        assertThat( resp.getEntity() ).isInstanceOf( PreboardedWebService.PreboardedResponse.class );
        PreboardedWebService.PreboardedResponse body =
                ( PreboardedWebService.PreboardedResponse ) resp.getEntity();
        assertThat( body.preboardedId ).isEqualTo( 9876L );
        assertThat( body.state ).isEqualTo( "Preboarded" );
    }

    @Test
    public void createPreboarded_missingBodyThrows400() {
        assertThatThrownBy( () -> webService.createPreboarded( null ) )
                .isInstanceOf( BadRequestException.class );
        PreboardedWebService.CreatePreboardedRequest empty = new PreboardedWebService.CreatePreboardedRequest();
        assertThatThrownBy( () -> webService.createPreboarded( empty ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void createPreboarded_existingAccessionReturns409WithIdAndType() throws Exception {
        when( preboardedService.createPreboarded( eq( "GSE12345" ), any(), any() ) )
                .thenThrow( new PreboardedExperimentService.AccessionAlreadyExistsException(
                        "GSE12345", 9876L, "preboarded" ) );
        PreboardedWebService.CreatePreboardedRequest req = new PreboardedWebService.CreatePreboardedRequest();
        req.accession = "GSE12345";
        Response resp = webService.createPreboarded( req );
        assertThat( resp.getStatus() ).isEqualTo( 409 );
        @SuppressWarnings("unchecked")
        Map<String, Object> body = ( Map<String, Object> ) resp.getEntity();
        assertThat( body ).containsEntry( "existing_id", 9876L )
                .containsEntry( "existing_type", "preboarded" )
                .containsEntry( "accession", "GSE12345" );
    }

    @Test
    public void createPreboarded_existingEEAccessionReturns409WithEEType() throws Exception {
        when( preboardedService.createPreboarded( eq( "GSE12345" ), any(), any() ) )
                .thenThrow( new PreboardedExperimentService.AccessionAlreadyExistsException(
                        "GSE12345", 1L, "expression_experiment" ) );
        PreboardedWebService.CreatePreboardedRequest req = new PreboardedWebService.CreatePreboardedRequest();
        req.accession = "GSE12345";
        Response resp = webService.createPreboarded( req );
        assertThat( resp.getStatus() ).isEqualTo( 409 );
        @SuppressWarnings("unchecked")
        Map<String, Object> body = ( Map<String, Object> ) resp.getEntity();
        assertThat( body ).containsEntry( "existing_type", "expression_experiment" );
    }

    @Test
    public void getPreboarded_returnsLatestProposalAndCount() {
        AgentProposal latest = new AgentProposal();
        latest.setId( 42L );
        latest.setRunId( "run-1" );
        latest.setInvestigation( preboarded );
        when( agentProposalService.findLatestByInvestigation( preboarded ) ).thenReturn( latest );
        when( agentProposalService.countByInvestigation( preboarded ) ).thenReturn( 3L );
        PreboardedWebService.PreboardedResponse body = webService.getPreboarded( 9876L );
        assertThat( body.preboardedId ).isEqualTo( 9876L );
        assertThat( body.latestProposal ).isNotNull();
        assertThat( body.latestProposal.proposalId ).isEqualTo( 42L );
        assertThat( body.proposalCount ).isEqualTo( 3L );
    }

    @Test
    public void getPreboarded_unknownIdThrows404() {
        when( preboardedService.load( 99999L ) ).thenReturn( null );
        assertThatThrownBy( () -> webService.getPreboarded( 99999L ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void listOrResolvePreboarded_byAccessionReturnsPreboarded() {
        when( preboardedService.findByAccession( "GSE12345" ) ).thenReturn( preboarded );
        when( agentProposalService.findLatestByInvestigation( preboarded ) ).thenReturn( null );
        when( agentProposalService.countByInvestigation( preboarded ) ).thenReturn( 0L );
        Response resp = webService.listOrResolvePreboarded( "GSE12345", null );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
        assertThat( resp.getEntity() ).isInstanceOf( PreboardedWebService.PreboardedResponse.class );
        assertThat( ( ( PreboardedWebService.PreboardedResponse ) resp.getEntity() ).accession )
                .isEqualTo( "GSE12345" );
    }

    @Test
    public void listOrResolvePreboarded_byAccessionUnknownThrows404() {
        when( preboardedService.findByAccession( "GSEZ" ) ).thenReturn( null );
        assertThatThrownBy( () -> webService.listOrResolvePreboarded( "GSEZ", null ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void listOrResolvePreboarded_byStateReturns501PointerToWorkflowQueue() {
        Response resp = webService.listOrResolvePreboarded( null, "Preboarded" );
        assertThat( resp.getStatus() ).isEqualTo( 501 );
        @SuppressWarnings("unchecked")
        Map<String, Object> body = ( Map<String, Object> ) resp.getEntity();
        assertThat( body ).containsEntry( "state", "Preboarded" );
        assertThat( ( String ) body.get( "redirect_to" ) ).contains( "/workflow/queue" );
    }

    @Test
    public void listOrResolvePreboarded_neitherParamThrows400() {
        assertThatThrownBy( () -> webService.listOrResolvePreboarded( null, null ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void attachProposal_newRunIdReturns201() {
        AgentProposal p = new AgentProposal();
        p.setId( 100L );
        p.setRunId( "run-1" );
        p.setInvestigation( preboarded );
        when( agentProposalService.attach( eq( preboarded ), eq( AgentCurationKind.PROPOSAL ),
                eq( "run-1" ), any(), any(), any(), any() ) )
                .thenReturn( new AgentProposalService.AttachedProposal( p, true ) );
        PreboardedWebService.AttachProposalRequest req = new PreboardedWebService.AttachProposalRequest();
        req.runId = "run-1";
        Response resp = webService.attachProposal( 9876L, req );
        assertThat( resp.getStatus() ).isEqualTo( 201 );
    }

    @Test
    public void attachProposal_existingRunIdReturns200() {
        AgentProposal p = new AgentProposal();
        p.setId( 100L );
        p.setRunId( "run-1" );
        p.setInvestigation( preboarded );
        when( agentProposalService.attach( eq( preboarded ), eq( AgentCurationKind.PROPOSAL ),
                eq( "run-1" ), any(), any(), any(), any() ) )
                .thenReturn( new AgentProposalService.AttachedProposal( p, false ) );
        PreboardedWebService.AttachProposalRequest req = new PreboardedWebService.AttachProposalRequest();
        req.runId = "run-1";
        Response resp = webService.attachProposal( 9876L, req );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
    }

    @Test
    public void attachProposal_blankRunIdThrows400() {
        PreboardedWebService.AttachProposalRequest req = new PreboardedWebService.AttachProposalRequest();
        assertThatThrownBy( () -> webService.attachProposal( 9876L, req ) )
                .isInstanceOf( BadRequestException.class );
        req.runId = "  ";
        assertThatThrownBy( () -> webService.attachProposal( 9876L, req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void attachProposal_unknownPreboardedThrows404() {
        when( preboardedService.load( 1L ) ).thenReturn( null );
        PreboardedWebService.AttachProposalRequest req = new PreboardedWebService.AttachProposalRequest();
        req.runId = "run-1";
        assertThatThrownBy( () -> webService.attachProposal( 1L, req ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void promotePreboarded_validReturns200WithCounts() throws Exception {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 555L );
        when( expressionExperimentService.load( 555L ) ).thenReturn( ee );
        when( preboardedService.promote( ee, preboarded ) )
                .thenReturn( new PreboardedExperimentService.PromotionResult( 9876L, 555L, 3 ) );

        PreboardedWebService.PromoteRequest req = new PreboardedWebService.PromoteRequest();
        req.eeId = 555L;
        req.applyLatestProposal = true;
        Response resp = webService.promotePreboarded( 9876L, req );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
        PreboardedWebService.PromoteResponse body =
                ( PreboardedWebService.PromoteResponse ) resp.getEntity();
        assertThat( body.preboardedId ).isEqualTo( 9876L );
        assertThat( body.eeId ).isEqualTo( 555L );
        assertThat( body.proposalsRebound ).isEqualTo( 3 );
        // apply_latest_proposal is forwarded but the server-side apply chain
        // is deferred; the response carries applied_proposal_id=null.
        assertThat( body.appliedProposalId ).isNull();
    }

    @Test
    public void promotePreboarded_missingEeIdThrows400() {
        PreboardedWebService.PromoteRequest req = new PreboardedWebService.PromoteRequest();
        assertThatThrownBy( () -> webService.promotePreboarded( 9876L, req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void promotePreboarded_unknownEeThrows404() {
        when( expressionExperimentService.load( 999L ) ).thenReturn( null );
        PreboardedWebService.PromoteRequest req = new PreboardedWebService.PromoteRequest();
        req.eeId = 999L;
        assertThatThrownBy( () -> webService.promotePreboarded( 9876L, req ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void promotePreboarded_alreadyPromotedReturns409() throws Exception {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 555L );
        when( expressionExperimentService.load( 555L ) ).thenReturn( ee );
        when( preboardedService.promote( ee, preboarded ) )
                .thenThrow( new PreboardedExperimentService.PreboardedAlreadyPromotedException( 9876L ) );
        PreboardedWebService.PromoteRequest req = new PreboardedWebService.PromoteRequest();
        req.eeId = 555L;
        Response resp = webService.promotePreboarded( 9876L, req );
        assertThat( resp.getStatus() ).isEqualTo( 409 );
    }
}
