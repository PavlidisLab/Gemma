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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetRole;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSource;
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.PreboardedExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.PreboardedExperimentService;

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
    private AnnotationSetService annotationSetService;
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
        assertThat( body ).containsEntry( "existingId", 9876L )
                .containsEntry( "existingType", "preboarded" )
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
        assertThat( body ).containsEntry( "existingType", "expression_experiment" );
    }

    @Test
    public void getPreboarded_returnsLatestAnnotationSetAndCount() {
        AnnotationSet latest = new AnnotationSet();
        latest.setId( 42L );
        latest.setRole( AnnotationSetRole.PROPOSAL );
        latest.setRunId( "run-1" );
        latest.setInvestigation( preboarded );
        when( annotationSetService.findLatestByInvestigation( preboarded, AnnotationSetRole.PROPOSAL ) )
                .thenReturn( latest );
        when( annotationSetService.countByInvestigation( preboarded, AnnotationSetRole.PROPOSAL ) )
                .thenReturn( 3L );
        PreboardedWebService.PreboardedResponse body = webService.getPreboarded( 9876L );
        assertThat( body.preboardedId ).isEqualTo( 9876L );
        assertThat( body.latestAnnotationSet ).isNotNull();
        assertThat( body.latestAnnotationSet.annotationSetId ).isEqualTo( 42L );
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
        when( annotationSetService.findLatestByInvestigation( preboarded, AnnotationSetRole.PROPOSAL ) )
                .thenReturn( null );
        when( annotationSetService.countByInvestigation( preboarded, AnnotationSetRole.PROPOSAL ) )
                .thenReturn( 0L );
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
        assertThat( ( String ) body.get( "redirectTo" ) ).contains( "/workflow/queue" );
    }

    @Test
    public void listOrResolvePreboarded_neitherParamThrows400() {
        assertThatThrownBy( () -> webService.listOrResolvePreboarded( null, null ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void attachAnnotationSet_newRunIdReturns201() {
        AnnotationSet a = new AnnotationSet();
        a.setId( 100L );
        a.setRole( AnnotationSetRole.PROPOSAL );
        a.setRunId( "run-1" );
        a.setInvestigation( preboarded );
        when( annotationSetService.attach( eq( preboarded ),
                eq( AnnotationSetRole.PROPOSAL ), eq( AnnotationSetSource.AGENT ),
                eq( AgentCurationKind.PROPOSAL ),
                eq( "run-1" ), any(), any(), any(), any(), any(), any() ) )
                .thenReturn( new AnnotationSetService.AttachedAnnotationSet( a, true ) );
        PreboardedWebService.AttachAnnotationSetRequest req = new PreboardedWebService.AttachAnnotationSetRequest();
        req.runId = "run-1";
        Response resp = webService.attachAnnotationSet( 9876L, req );
        assertThat( resp.getStatus() ).isEqualTo( 201 );
    }

    @Test
    public void attachAnnotationSet_existingRunIdReturns200() {
        AnnotationSet a = new AnnotationSet();
        a.setId( 100L );
        a.setRole( AnnotationSetRole.PROPOSAL );
        a.setRunId( "run-1" );
        a.setInvestigation( preboarded );
        when( annotationSetService.attach( eq( preboarded ),
                eq( AnnotationSetRole.PROPOSAL ), eq( AnnotationSetSource.AGENT ),
                eq( AgentCurationKind.PROPOSAL ),
                eq( "run-1" ), any(), any(), any(), any(), any(), any() ) )
                .thenReturn( new AnnotationSetService.AttachedAnnotationSet( a, false ) );
        PreboardedWebService.AttachAnnotationSetRequest req = new PreboardedWebService.AttachAnnotationSetRequest();
        req.runId = "run-1";
        Response resp = webService.attachAnnotationSet( 9876L, req );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
    }

    @Test
    public void attachAnnotationSet_blankRunIdThrows400() {
        PreboardedWebService.AttachAnnotationSetRequest req = new PreboardedWebService.AttachAnnotationSetRequest();
        assertThatThrownBy( () -> webService.attachAnnotationSet( 9876L, req ) )
                .isInstanceOf( BadRequestException.class );
        req.runId = "  ";
        assertThatThrownBy( () -> webService.attachAnnotationSet( 9876L, req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void attachAnnotationSet_unknownPreboardedThrows404() {
        when( preboardedService.load( 1L ) ).thenReturn( null );
        PreboardedWebService.AttachAnnotationSetRequest req = new PreboardedWebService.AttachAnnotationSetRequest();
        req.runId = "run-1";
        assertThatThrownBy( () -> webService.attachAnnotationSet( 1L, req ) )
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
        assertThat( body.annotationSetsRebound ).isEqualTo( 3 );
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
