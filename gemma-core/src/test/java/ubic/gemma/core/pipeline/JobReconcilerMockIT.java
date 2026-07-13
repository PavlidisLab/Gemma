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
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.pipeline.PipelineJob;
import ubic.gemma.model.pipeline.PipelineJobBatch;
import ubic.gemma.model.pipeline.PipelineJobEvent;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reconciler integration test for the {@code STALL_THEN_RECONCILE} (POLL) path: a job
 * that never phones home is picked up by {@link JobReconciler#tick()} polling the mock.
 *
 * <p>Does NOT activate the {@code scheduler} profile — {@code SchedulerConfig}'s Quartz
 * wiring fails context startup there (dead {@code indexerService} bean). Instead the
 * reconciler is constructed directly and injected via reflection, exercising the real
 * {@code tick()} + {@code mapStateToEventKind} against the scripted mock. {@code
 * staleMinutes=0} so the freshly-submitted job is immediately eligible.</p>
 */
@ActiveProfiles("scheduler-mock")
class JobReconcilerMockIT extends BaseSpringContextTest5 {

    @Autowired
    private PipelineJobBatchService pipelineJobBatchService;

    @Autowired
    private MockSchedulerControl control;

    @Autowired
    private PipelineScheduler scheduler;

    @BeforeEach
    void resetMock() {
        control.reset();
    }

    @Test
    void stalledJob_reconcilerPollObservesRunningAndRecordsEvent() {
        Contact submitter = getTestPersistentContact();
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();
        control.setScenario( ee.getId(), stallPoll() );

        PipelineJobBatch batch = pipelineJobBatchService.submit(
                "test-pipeline", Collections.singletonList( ee ), submitter, null, "reconciler IT" );
        Long jobId = batch.getJobs().iterator().next().getId();

        // Move the virtual clock so poll() reports RUNNING for the (POLL) stall scenario.
        control.advance( 1000 );

        // No push happened, so no events yet — the reconciler is the only thing that will land one.
        assertThat( pipelineJobBatchService.findEvents( jobId, null, 100 ) ).isEmpty();

        JobReconciler reconciler = new JobReconciler();
        ReflectionTestUtils.setField( reconciler, "pipelineJobBatchService", pipelineJobBatchService );
        ReflectionTestUtils.setField( reconciler, "scheduler", scheduler );
        ReflectionTestUtils.setField( reconciler, "staleMinutes", 0 );
        ReflectionTestUtils.setField( reconciler, "limit", 50 );

        reconciler.tick();

        List<PipelineJobEvent> events = pipelineJobBatchService.findEvents( jobId, null, 100 );
        assertThat( events ).extracting( PipelineJobEvent::getKind ).contains( "stage" );
    }

    private static Scenario stallPoll() {
        Scenario s = new Scenario();
        s.outcome = Scenario.Outcome.STALL;
        s.transport = Scenario.Transport.POLL;
        Scenario.Stage st = new Scenario.Stage();
        st.afterMs = 0;
        st.kind = "stage";
        st.payloadJson = "{\"stage\":\"running\"}";
        s.stages.add( st );
        return s;
    }
}
