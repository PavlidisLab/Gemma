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
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.WorkflowState;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito unit tests for {@link WorkflowServiceImpl}: state-machine
 * enforcement, idempotent no-op semantics, the disallowed-transition
 * exception. Does NOT exercise the {@code @AuditedConditional} aspect
 * (no Spring proxy here); aspect coverage lives in {@code AuditedAspectTest}
 * and the lifecycle integration test.
 */
@ExtendWith(MockitoExtension.class)
public class WorkflowServiceTest {

    @Mock
    private AuditEventService auditEventService;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    private ExpressionExperiment ee;

    @BeforeEach
    public void setUp() {
        ee = new ExpressionExperiment();
        ee.setId( 99L );
        ee.setWorkflowState( WorkflowState.Loaded );
        lenient().when( sessionFactory.getCurrentSession() ).thenReturn( session );
    }

    @Test
    public void getCurrentState_returnsLoadedForNullField() {
        ee.setWorkflowState( null );
        assertThat( workflowService.getCurrentState( ee ) ).isEqualTo( WorkflowState.Loaded );
    }

    @Test
    public void getCurrentState_returnsField() {
        ee.setWorkflowState( WorkflowState.Audit );
        assertThat( workflowService.getCurrentState( ee ) ).isEqualTo( WorkflowState.Audit );
    }

    @Test
    public void advance_allowedTransitionUpdatesStateAndTimestamp() {
        ee.setWorkflowState( WorkflowState.Loaded );
        WorkflowTransition t = workflowService.advance( ee, WorkflowState.Curate, "begin", null );
        assertThat( t.getPreviousState() ).isEqualTo( WorkflowState.Loaded );
        assertThat( t.getCurrentState() ).isEqualTo( WorkflowState.Curate );
        assertThat( t.getEnteredCurrentStateAt() ).isNotNull();
        assertThat( t.isIdempotent() ).isFalse();
        assertThat( ee.getWorkflowState() ).isEqualTo( WorkflowState.Curate );
        assertThat( ee.getWorkflowStateEnteredAt() ).isNotNull();
        verify( session ).update( ee );
    }

    @Test
    public void advance_idempotentNoOpEmitsNoUpdate() {
        ee.setWorkflowState( WorkflowState.Curate );
        WorkflowTransition t = workflowService.advance( ee, WorkflowState.Curate, "redundant", null );
        assertThat( t.isIdempotent() ).isTrue();
        assertThat( t.getPreviousState() ).isEqualTo( WorkflowState.Curate );
        assertThat( t.getCurrentState() ).isEqualTo( WorkflowState.Curate );
        // No row mutation should happen on the no-op path.
        verify( session, never() ).update( ee );
    }

    @Test
    public void advance_disallowedTransitionThrows() {
        ee.setWorkflowState( WorkflowState.Discovery );
        assertThatThrownBy( () -> workflowService.advance( ee, WorkflowState.Public, null, null ) )
                .isInstanceOf( DisallowedWorkflowTransitionException.class )
                .satisfies( ex -> {
                    DisallowedWorkflowTransitionException dwte =
                            ( DisallowedWorkflowTransitionException ) ex;
                    assertThat( dwte.getCurrentState() ).isEqualTo( WorkflowState.Discovery );
                    assertThat( dwte.getTargetState() ).isEqualTo( WorkflowState.Public );
                    assertThat( dwte.getAllowedNextStates() )
                            .containsExactlyInAnyOrder( WorkflowState.Candidate, WorkflowState.Skeleton );
                } );
        // Disallowed path must NOT mutate the row.
        verify( session, never() ).update( ee );
    }

    /**
     * Spec §"Acceptance criteria" exercises the full lifecycle:
     * {@code Discovery → Candidate → Skeleton → Loaded → Curate → Audit →
     * Curate → Process → Audit → Public}. The pure-Mockito version of that
     * test asserts only the state-machine sequence (no aspect, no real DB).
     * The integration variant (orchestrator's job) layers the AUDIT_EVENT
     * + INVESTIGATION row assertions on top.
     */
    @Test
    public void advance_fullLifecycleSequence() {
        ee.setWorkflowState( WorkflowState.Discovery );
        WorkflowState[] path = {
                WorkflowState.Candidate,
                WorkflowState.Skeleton,
                WorkflowState.Loaded,
                WorkflowState.Curate,
                WorkflowState.Audit,
                WorkflowState.Curate,
                WorkflowState.Process,
                WorkflowState.Audit,
                WorkflowState.Public,
        };
        WorkflowState[] prevStates = {
                WorkflowState.Discovery,
                WorkflowState.Candidate,
                WorkflowState.Skeleton,
                WorkflowState.Loaded,
                WorkflowState.Curate,
                WorkflowState.Audit,
                WorkflowState.Curate,
                WorkflowState.Process,
                WorkflowState.Audit,
        };
        for ( int i = 0; i < path.length; i++ ) {
            WorkflowTransition t = workflowService.advance( ee, path[i], null, null );
            assertThat( t.getPreviousState() ).as( "step %d previous", i ).isEqualTo( prevStates[i] );
            assertThat( t.getCurrentState() ).as( "step %d current", i ).isEqualTo( path[i] );
            assertThat( ee.getWorkflowState() ).as( "step %d ee.state", i ).isEqualTo( path[i] );
        }
    }

    /* ====== State-machine enum tests ====== */

    @Test
    public void stateMachine_transitionTableMatchesHandoff() {
        assertThat( WorkflowState.Discovery.allowedNextStates() )
                .containsExactlyInAnyOrder( WorkflowState.Candidate, WorkflowState.Skeleton );
        assertThat( WorkflowState.Candidate.allowedNextStates() )
                .containsExactlyInAnyOrder( WorkflowState.Skeleton, WorkflowState.Discovery );
        assertThat( WorkflowState.Skeleton.allowedNextStates() )
                .containsExactlyInAnyOrder( WorkflowState.Loaded, WorkflowState.Candidate );
        assertThat( WorkflowState.Loaded.allowedNextStates() )
                .containsExactly( WorkflowState.Curate );
        assertThat( WorkflowState.Curate.allowedNextStates() )
                .containsExactlyInAnyOrder( WorkflowState.Process, WorkflowState.Audit );
        assertThat( WorkflowState.Process.allowedNextStates() )
                .containsExactlyInAnyOrder( WorkflowState.Audit, WorkflowState.Curate );
        assertThat( WorkflowState.Audit.allowedNextStates() )
                .containsExactlyInAnyOrder( WorkflowState.Curate, WorkflowState.Public );
        assertThat( WorkflowState.Public.allowedNextStates() )
                .containsExactly( WorkflowState.Curate );
    }

    @Test
    public void stateMachine_canTransitionToIsStrict() {
        // Self-transition is not a valid transition (idempotent no-op is
        // handled at the service layer, not the state machine).
        assertThat( WorkflowState.Curate.canTransitionTo( WorkflowState.Curate ) ).isFalse();
        // Null target is never allowed.
        assertThat( WorkflowState.Curate.canTransitionTo( null ) ).isFalse();
        // Random skips are forbidden.
        assertThat( WorkflowState.Discovery.canTransitionTo( WorkflowState.Loaded ) ).isFalse();
        assertThat( WorkflowState.Loaded.canTransitionTo( WorkflowState.Audit ) ).isFalse();
    }
}
