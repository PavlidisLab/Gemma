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
package ubic.gemma.core.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.pipeline.BatchRollup;
import ubic.gemma.model.pipeline.PipelineJobBatch;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dispatch throttle + hold/resume (§3.4 #1 / §7 task 4). Deterministic via the scripted mock: a
 * batch with {@code maxConcurrent=K} never has more than K jobs in flight, and the dispatcher tops
 * up as jobs finish; {@code hold} pauses new dispatch while in-flight jobs continue.
 */
@ActiveProfiles("scheduler-mock")
class PipelineJobThrottleMockIT extends BaseSpringContextTest5 {

    @Autowired
    private PipelineJobBatchService pipelineJobBatchService;

    @Autowired
    private MockSchedulerControl control;

    @BeforeEach
    void setUpMock() {
        control.reset();
        control.setScenario( null, success() );  // fallback: every EE succeeds
    }

    @Test
    void maxConcurrent_neverExceedsCap_andDispatcherTopsUp() {
        final int N = 10, CAP = 3;
        PipelineJobBatch batch = pipelineJobBatchService.submit( "test-pipeline",
                newExperiments( N ), getTestPersistentContact(), null, "throttle IT", CAP );
        Long batchId = batch.getId();

        // Right after submit: only CAP dispatched, the rest wait as PENDING.
        BatchRollup r0 = pipelineJobBatchService.computeRollup( batchId );
        assertThat( r0.queued ).isEqualTo( CAP );
        assertThat( r0.pending ).isEqualTo( N - CAP );

        // Advance to complete the in-flight jobs, then top up — never exceeding the cap.
        int guard = 0;
        while ( pipelineJobBatchService.computeRollup( batchId ).done < N && guard++ < 20 ) {
            control.advance( 2000 );                          // complete whatever is in flight
            pipelineJobBatchService.dispatchPending( batchId ); // dispatcher top-up
            BatchRollup r = pipelineJobBatchService.computeRollup( batchId );
            assertThat( r.queued + r.running )
                    .as( "in-flight must never exceed maxConcurrent" )
                    .isLessThanOrEqualTo( CAP );
        }

        BatchRollup done = pipelineJobBatchService.computeRollup( batchId );
        assertThat( done.done ).isEqualTo( N );
        assertThat( done.terminal ).isTrue();
        assertThat( pipelineJobBatchService.get( batchId ).getState() )
                .isEqualTo( PipelineJobBatch.BatchState.CLOSED );
    }

    @Test
    void hold_blocksDispatch_resumeRestarts() {
        final int N = 5, CAP = 3;
        PipelineJobBatch batch = pipelineJobBatchService.submit( "test-pipeline",
                newExperiments( N ), getTestPersistentContact(), null, "hold IT", CAP );
        Long batchId = batch.getId();

        // Free up the whole budget so only the hold — not the cap — can block dispatch.
        control.advance( 2000 );  // the initial CAP complete
        assertThat( pipelineJobBatchService.computeRollup( batchId ).pending ).isEqualTo( N - CAP );

        pipelineJobBatchService.holdBatch( batchId );
        assertThat( pipelineJobBatchService.dispatchPending( batchId ) )
                .as( "held batch dispatches nothing despite free budget + pending jobs" )
                .isZero();
        assertThat( pipelineJobBatchService.computeRollup( batchId ).pending ).isEqualTo( N - CAP );

        pipelineJobBatchService.resumeBatch( batchId );  // clears hold + tops up
        BatchRollup r = pipelineJobBatchService.computeRollup( batchId );
        assertThat( r.pending ).isZero();
        assertThat( r.queued ).isEqualTo( N - CAP );
    }

    private List<ExpressionExperiment> newExperiments( int n ) {
        List<ExpressionExperiment> ees = new ArrayList<>();
        for ( int i = 0; i < n; i++ ) {
            ees.add( getTestPersistentBasicExpressionExperiment() );
        }
        return ees;
    }

    private static Scenario success() {
        Scenario s = new Scenario();
        s.outcome = Scenario.Outcome.SUCCEED;
        s.transport = Scenario.Transport.PUSH;
        Scenario.Stage a = new Scenario.Stage();
        a.afterMs = 0;
        a.kind = "stage";
        a.payloadJson = "{}";
        Scenario.Stage b = new Scenario.Stage();
        b.afterMs = 1000;
        b.kind = "completed";
        b.payloadJson = "{}";
        s.stages.add( a );
        s.stages.add( b );
        return s;
    }
}
