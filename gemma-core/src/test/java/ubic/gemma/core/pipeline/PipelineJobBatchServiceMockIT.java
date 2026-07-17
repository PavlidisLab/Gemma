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
import ubic.gemma.model.pipeline.PipelineJob;
import ubic.gemma.model.pipeline.PipelineJobBatch;
import ubic.gemma.model.pipeline.PipelineJobEvent;
import ubic.gemma.persistence.service.pipeline.PipelineDefaults;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service-level integration test of the mop-up-shaped path against gemdtest, driven by
 * the scripted mock. Realizes {@code PARTIAL_BATCH}: every 3rd experiment gets a
 * transient failure, the rest succeed. Deterministic — the terminal states come from
 * one {@code advance()} of the virtual clock, with NO {@code Thread.sleep}.
 */
@ActiveProfiles("scheduler-mock")
class PipelineJobBatchServiceMockIT extends BaseSpringContextTest5 {

    private static final int N = 6;
    private static final int FAIL_EVERY_NTH = 3;

    @Autowired
    private PipelineJobBatchService pipelineJobBatchService;

    @Autowired
    private MockSchedulerControl control;

    @BeforeEach
    void resetMock() {
        control.reset();
    }

    @Test
    void partialBatch_advance_leavesTransientFailuresAndKeepsBatchOpen() {
        Contact submitter = getTestPersistentContact();
        List<ExpressionExperiment> ees = new ArrayList<>();
        List<Long> expectedFailEeIds = new ArrayList<>();
        for ( int i = 0; i < N; i++ ) {
            ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();
            ees.add( ee );
            boolean shouldFail = i % FAIL_EVERY_NTH == FAIL_EVERY_NTH - 1;
            control.setScenario( ee.getId(), shouldFail ? transientFailure() : success() );
            if ( shouldFail ) {
                expectedFailEeIds.add( ee.getId() );
            }
        }

        PipelineJobBatch batch = pipelineJobBatchService.submit( "test-pipeline", ees, submitter, null, "mock IT" );

        // Single deterministic clock step past both scripted stages (error/completed at 1000ms).
        control.advance( 2000 );

        int failed = 0, done = 0;
        for ( PipelineJob job : batch.getJobs() ) {
            List<PipelineJobEvent> events = pipelineJobBatchService.findEvents( job.getId(), null, 100 );
            List<String> kinds = events.stream().map( PipelineJobEvent::getKind ).toList();
            if ( expectedFailEeIds.contains( job.getExperiment().getId() ) ) {
                assertThat( kinds ).contains( "error" );
                assertThat( events ).anySatisfy( e ->
                        assertThat( e.getPayloadJson() ).contains( "TRANSIENT" ) );
                failed++;
            } else {
                assertThat( kinds ).contains( "completed" );
                done++;
            }
        }

        assertThat( failed ).isEqualTo( N / FAIL_EVERY_NTH );
        assertThat( done ).isEqualTo( N - N / FAIL_EVERY_NTH );

        // §3.2 disposition: failures keep the batch OPEN (needs mop-up), NOT auto-closed —
        // even though every job reached a terminal state.
        assertThat( pipelineJobBatchService.get( batch.getId() ).getState() )
                .isEqualTo( PipelineJobBatch.BatchState.OPEN );
    }

    @Test
    void progressTicksAreSnapshotOnly_onlyMilestonesPersistAsRows() {
        Contact submitter = getTestPersistentContact();
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();
        control.setScenario( ee.getId(), progressHeavy() );

        PipelineJobBatch batch = pipelineJobBatchService.submit(
                "test-pipeline", java.util.Collections.singletonList( ee ), submitter, null, "write-policy IT" );
        Long jobId = batch.getJobs().iterator().next().getId();

        control.advance( 1000 );

        // Delegated write policy (§3.1): the two progress ticks update the snapshot only;
        // just the stage + completed milestones become durable rows.
        List<PipelineJobEvent> events = pipelineJobBatchService.findEvents( jobId, null, 100 );
        assertThat( events ).extracting( PipelineJobEvent::getKind )
                .containsExactlyInAnyOrder( "stage", "completed" )
                .doesNotContain( "progress" );
    }

    @Test
    void scAnnotationSubmit_withoutCap_getsPipelineDefault() {
        // O8: a batch for sc-annotation submitted without an explicit cap picks up the per-pipeline
        // default (25), so it never launches unbounded.
        Contact submitter = getTestPersistentContact();
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();
        control.setScenario( ee.getId(), success() );

        PipelineJobBatch batch = pipelineJobBatchService.submit(
                PipelineDefaults.SC_ANNOTATION, java.util.Collections.singletonList( ee ), submitter, null, "O8 default" );

        assertThat( pipelineJobBatchService.get( batch.getId() ).getMaxConcurrent() ).isEqualTo( 25 );
    }

    @Test
    void otherPipelineSubmit_withoutCap_staysUnlimited() {
        // Pipelines with no configured default keep the pre-existing behaviour (null ⇒ unlimited).
        Contact submitter = getTestPersistentContact();
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();
        control.setScenario( ee.getId(), success() );

        PipelineJobBatch batch = pipelineJobBatchService.submit(
                "test-pipeline", java.util.Collections.singletonList( ee ), submitter, null, "no default" );

        assertThat( pipelineJobBatchService.get( batch.getId() ).getMaxConcurrent() ).isNull();
    }

    private static Scenario progressHeavy() {
        Scenario s = new Scenario();
        s.outcome = Scenario.Outcome.SUCCEED;
        s.transport = Scenario.Transport.PUSH;
        s.stages.add( stage( 0, "stage", "{\"stage\":\"align\"}" ) );
        s.stages.add( stage( 100, "progress", "{\"pct\":33}" ) );
        s.stages.add( stage( 200, "progress", "{\"pct\":66}" ) );
        s.stages.add( stage( 300, "completed", "{}" ) );
        return s;
    }

    private static Scenario success() {
        Scenario s = new Scenario();
        s.outcome = Scenario.Outcome.SUCCEED;
        s.transport = Scenario.Transport.PUSH;
        s.stages.add( stage( 0, "stage", "{\"stage\":\"align\"}" ) );
        s.stages.add( stage( 1000, "completed", "{}" ) );
        return s;
    }

    private static Scenario transientFailure() {
        Scenario s = new Scenario();
        s.outcome = Scenario.Outcome.FAIL;
        s.failureClass = Scenario.FailureClass.TRANSIENT;
        s.transport = Scenario.Transport.PUSH;
        s.stages.add( stage( 0, "stage", "{\"stage\":\"align\"}" ) );
        s.stages.add( stage( 1000, "error", "{\"failureClass\":\"TRANSIENT\",\"message\":\"synthetic SRA throttle\"}" ) );
        return s;
    }

    private static Scenario.Stage stage( long afterMs, String kind, String payloadJson ) {
        Scenario.Stage st = new Scenario.Stage();
        st.afterMs = afterMs;
        st.kind = kind;
        st.payloadJson = payloadJson;
        return st;
    }
}
