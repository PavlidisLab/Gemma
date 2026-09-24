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
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.model.pipeline.JobState;
import ubic.gemma.model.pipeline.PipelineJob;
import ubic.gemma.model.pipeline.SchedulerKind;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * How {@link JobReconciler} turns a poll into an event. The end-to-end poll path is in
 * {@code JobReconcilerMockIT}; this pins the cases the scripted mock doesn't produce.
 */
class JobReconcilerTest {

    private PipelineJobBatchService service;
    private PipelineScheduler scheduler;
    private JobReconciler reconciler;
    private PipelineJob job;

    @BeforeEach
    void setUp() {
        service = mock( PipelineJobBatchService.class );
        scheduler = mock( PipelineScheduler.class );
        reconciler = new JobReconciler();
        ReflectionTestUtils.setField( reconciler, "pipelineJobBatchService", service );
        ReflectionTestUtils.setField( reconciler, "scheduler", scheduler );
        ReflectionTestUtils.setField( reconciler, "staleMinutes", 1 );
        ReflectionTestUtils.setField( reconciler, "limit", 50 );
        job = new PipelineJob();
        job.setId( 7L );
        job.setSchedulerKind( SchedulerKind.NEXTFLOW );
        job.setSchedulerHandle( "42" );
        when( service.findStaleJobs( 1, 50 ) ).thenReturn( Collections.singletonList( job ) );
    }

    @Test
    void sameStateWithProgress_recordsProgress() throws Exception {
        job.setState( JobState.RUNNING );
        when( scheduler.poll( eq( 7L ), any() ) ).thenReturn( new JobSnapshot( JobState.RUNNING, null, "{\"tasksCompleted\":3}" ) );
        reconciler.tick();
        verify( service ).recordEvent( 7L, "progress", "{\"tasksCompleted\":3}" );
    }

    @Test
    void sameStateWithoutProgress_recordsHeartbeat() throws Exception {
        job.setState( JobState.RUNNING );
        when( scheduler.poll( eq( 7L ), any() ) ).thenReturn( new JobSnapshot( JobState.RUNNING, null, null ) );
        reconciler.tick();
        verify( service ).recordEvent( eq( 7L ), eq( "heartbeat" ), isNull() );
    }

    @Test
    void cancellingJobThatExitedNonZero_isKilledNotFailed() throws Exception {
        // scancel makes the run exit non-zero; that is the cancel landing, not a pipeline failure.
        job.setState( JobState.CANCELLING );
        when( scheduler.poll( eq( 7L ), any() ) ).thenReturn( new JobSnapshot( JobState.FAILED, "exit 143", "{}" ) );
        reconciler.tick();
        verify( service ).recordEvent( eq( 7L ), eq( "killed" ), isNull() );
    }

    @Test
    void cancellingJobGoneFromSlurm_isKilled() throws Exception {
        job.setState( JobState.CANCELLING );
        when( scheduler.poll( eq( 7L ), any() ) ).thenReturn( null );
        reconciler.tick();
        verify( service ).recordEvent( eq( 7L ), eq( "killed" ), isNull() );
    }

    @Test
    void cancellingJobThatFinishedFirst_keepsItsSuccess() throws Exception {
        job.setState( JobState.CANCELLING );
        when( scheduler.poll( eq( 7L ), any() ) ).thenReturn( new JobSnapshot( JobState.DONE, "exit 0", null ) );
        reconciler.tick();
        verify( service ).recordEvent( eq( 7L ), eq( "completed" ), isNull() );
    }
}
