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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;
import ubic.gemma.persistence.service.expression.experiment.DisallowedWorkflowTransitionException;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.WorkflowService;
import ubic.gemma.persistence.service.expression.experiment.WorkflowTransition;
import ubic.gemma.rest.util.ResponseDataObject;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito unit tests for {@link WorkflowWebService}: REST-shape
 * validation, error mapping (404 / 400 / 409). The state machine is
 * exercised in {@code WorkflowServiceTest}; this file focuses on the
 * REST surface contract.
 */
@ExtendWith(MockitoExtension.class)
public class WorkflowWebServiceTest {

    @Mock
    private WorkflowService workflowService;

    @Mock
    private ExpressionExperimentService expressionExperimentService;

    @InjectMocks
    private WorkflowWebService webService;

    private ExpressionExperiment ee;

    @BeforeEach
    public void setUp() {
        ee = new ExpressionExperiment();
        ee.setId( 12345L );
        ee.setWorkflowState( WorkflowState.Loaded );
        ee.setWorkflowStateEnteredAt( new Date( 1_700_000_000_000L ) );
        lenient().when( expressionExperimentService.load( 12345L ) ).thenReturn( ee );
    }

    @Test
    public void getDatasetWorkflow_returnsCurrentStateAndEmptyHistory() {
        when( workflowService.getCurrentState( ee ) ).thenReturn( WorkflowState.Loaded );
        when( workflowService.getHistory( ee ) ).thenReturn( Collections.emptyList() );
        ResponseDataObject<WorkflowWebService.WorkflowStateResponse> r =
                webService.getDatasetWorkflow( 12345L );
        assertThat( r.getData().datasetId ).isEqualTo( 12345L );
        assertThat( r.getData().datasetType ).isEqualTo( "expression_experiment" );
        assertThat( r.getData().currentState ).isEqualTo( "Loaded" );
        assertThat( r.getData().enteredCurrentStateAt ).isNotNull();
        assertThat( r.getData().history ).isEmpty();
    }

    @Test
    public void getDatasetWorkflow_unknownIdThrows404() {
        when( expressionExperimentService.load( 99999L ) ).thenReturn( null );
        assertThatThrownBy( () -> webService.getDatasetWorkflow( 99999L ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void advanceDatasetWorkflow_validTransitionReturns200() {
        WorkflowTransition t = new WorkflowTransition( 12345L, WorkflowState.Loaded,
                WorkflowState.Curate, new Date(), null );
        when( workflowService.advance( eq( ee ), eq( WorkflowState.Curate ),
                any(), any() ) ).thenReturn( t );

        WorkflowWebService.AdvanceWorkflowRequest req = new WorkflowWebService.AdvanceWorkflowRequest();
        req.setTargetState( "Curate" );
        req.setReason( "begin curation" );
        Response resp = webService.advanceDatasetWorkflow( 12345L, req );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
    }

    @Test
    public void advanceDatasetWorkflow_idempotentNoOp() {
        WorkflowTransition t = new WorkflowTransition( 12345L, WorkflowState.Loaded,
                WorkflowState.Loaded, ee.getWorkflowStateEnteredAt(), null );
        when( workflowService.getCurrentState( ee ) ).thenReturn( WorkflowState.Loaded );
        when( workflowService.advance( eq( ee ), eq( WorkflowState.Loaded ),
                any(), any() ) ).thenReturn( t );
        WorkflowWebService.AdvanceWorkflowRequest req = new WorkflowWebService.AdvanceWorkflowRequest();
        req.setTargetState( "Loaded" );
        Response resp = webService.advanceDatasetWorkflow( 12345L, req );
        assertThat( resp.getStatus() ).isEqualTo( 200 );
    }

    @Test
    public void advanceDatasetWorkflow_unknownTargetStateThrows400() {
        WorkflowWebService.AdvanceWorkflowRequest req = new WorkflowWebService.AdvanceWorkflowRequest();
        req.setTargetState( "NotAState" );
        assertThatThrownBy( () -> webService.advanceDatasetWorkflow( 12345L, req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void advanceDatasetWorkflow_missingBodyThrows400() {
        assertThatThrownBy( () -> webService.advanceDatasetWorkflow( 12345L, null ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void advanceDatasetWorkflow_missingTargetStateThrows400() {
        WorkflowWebService.AdvanceWorkflowRequest req = new WorkflowWebService.AdvanceWorkflowRequest();
        assertThatThrownBy( () -> webService.advanceDatasetWorkflow( 12345L, req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void advanceDatasetWorkflow_unknownDatasetThrows404() {
        when( expressionExperimentService.load( 99999L ) ).thenReturn( null );
        WorkflowWebService.AdvanceWorkflowRequest req = new WorkflowWebService.AdvanceWorkflowRequest();
        req.setTargetState( "Curate" );
        assertThatThrownBy( () -> webService.advanceDatasetWorkflow( 99999L, req ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void advanceDatasetWorkflow_disallowedTransitionReturns409WithAllowedList() {
        when( workflowService.advance( eq( ee ), eq( WorkflowState.Public ), any(), any() ) )
                .thenThrow( new DisallowedWorkflowTransitionException( WorkflowState.Loaded,
                        WorkflowState.Public, EnumSet.of( WorkflowState.Curate ) ) );
        WorkflowWebService.AdvanceWorkflowRequest req = new WorkflowWebService.AdvanceWorkflowRequest();
        req.setTargetState( "Public" );
        Response resp = webService.advanceDatasetWorkflow( 12345L, req );
        assertThat( resp.getStatus() ).isEqualTo( 409 );
        Object entity = resp.getEntity();
        assertThat( entity ).isInstanceOf( Map.class );
        @SuppressWarnings("unchecked")
        Map<String, Object> body = ( Map<String, Object> ) entity;
        assertThat( body ).containsEntry( "error", "Disallowed transition" );
        assertThat( body ).containsEntry( "current_state", "Loaded" );
        assertThat( body ).containsEntry( "target_state", "Public" );
        @SuppressWarnings("unchecked")
        List<String> allowed = ( List<String> ) body.get( "allowed_next_states" );
        assertThat( allowed ).containsExactly( "Curate" );
    }

    @Test
    public void advanceDatasetWorkflow_publicToCurateRequiresNonEmptyReason() {
        ee.setWorkflowState( WorkflowState.Public );
        when( workflowService.getCurrentState( ee ) ).thenReturn( WorkflowState.Public );
        WorkflowWebService.AdvanceWorkflowRequest req = new WorkflowWebService.AdvanceWorkflowRequest();
        req.setTargetState( "Curate" );
        // No reason supplied -> 400 before we even check admin role.
        assertThatThrownBy( () -> webService.advanceDatasetWorkflow( 12345L, req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void getWorkflowQueue_missingStateThrows400() {
        assertThatThrownBy( () -> webService.getWorkflowQueue( null, null, null, null,
                ubic.gemma.rest.util.args.OffsetArg.valueOf( "0" ),
                ubic.gemma.rest.util.args.LimitArg.valueOf( "20" ) ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void getWorkflowQueue_unknownStateThrows400() {
        assertThatThrownBy( () -> webService.getWorkflowQueue( "NotAState", null, null, null,
                ubic.gemma.rest.util.args.OffsetArg.valueOf( "0" ),
                ubic.gemma.rest.util.args.LimitArg.valueOf( "20" ) ) )
                .isInstanceOf( BadRequestException.class );
    }
}
