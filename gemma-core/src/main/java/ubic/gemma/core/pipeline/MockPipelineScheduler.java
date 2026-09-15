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

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ubic.gemma.model.pipeline.JobState;
import ubic.gemma.model.pipeline.SchedulerKind;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-JVM mock scheduler for local-mode smoke testing of the admin pipeline UI
 * without a real Luigi/Nextflow wired. Selected by
 * {@code spring.profiles.active=scheduler-mock}.
 *
 * <p>Behaviour: every {@link #submit} call assigns a UUID handle and schedules
 * three asynchronous transitions on a background timer — {@code stage} after 5s,
 * {@code progress} after 10s, {@code completed} after 15s. {@link #poll} reads
 * the in-memory state machine; {@link #cancel} short-circuits to terminal
 * {@code CANCELLED} on the next poll.</p>
 *
 * <p>The mock does NOT post back via the internal callback endpoint — it only
 * answers polls. This is intentional: the framework's reconciler poll loop
 * picks up state changes within its tick interval, which exercises the
 * "scheduler ran but didn't phone home" code path end-to-end. To exercise the
 * push path, mock the scheduler in a test and have the test fire events
 * directly via {@code PipelineJobBatchService.recordEvent}.</p>
 *
 * <p>Reports kind {@code MOCK} — distinct from {@code LUIGI} / {@code NEXTFLOW}
 * so production code can never confuse a mock-dispatched job for a real one.</p>
 */
@Component
@Profile("scheduler-mock")
@Primary
@Slf4j
public class MockPipelineScheduler implements PipelineScheduler {

    private static final SchedulerKind MOCK_KIND = SchedulerKind.MOCK;

    private final ScheduledExecutorService timer =
            Executors.newSingleThreadScheduledExecutor( r -> {
                Thread t = new Thread( r, "mock-scheduler" );
                t.setDaemon( true );
                return t;
            } );

    /**
     * Handle -> latest observed scheduler-side state. Stays in memory for the
     * JVM's lifetime; restart loses all mock jobs (acceptable since gemdtest
     * is also volatile across restarts in dev).
     */
    private final ConcurrentHashMap<String, MockJob> jobs = new ConcurrentHashMap<>();

    @Override
    public SchedulerKind kind() {
        return MOCK_KIND;
    }

    @Override
    public SchedulerHandle submit( SubmitRequest req ) {
        String handle = UUID.randomUUID().toString();
        MockJob j = new MockJob();
        j.state.set( JobState.QUEUED );
        jobs.put( handle, j );
        // Schedule the synthetic lifecycle: queued -> running -> done.
        j.timers[0] = timer.schedule( () -> j.state.set( JobState.RUNNING ), 5, TimeUnit.SECONDS );
        j.timers[1] = timer.schedule( () -> {
            // mid-run; no state change, just bumps the "raw" status string
            j.raw.set( "running (50%)" );
        }, 10, TimeUnit.SECONDS );
        j.timers[2] = timer.schedule( () -> j.state.set( JobState.DONE ), 15, TimeUnit.SECONDS );
        log.info( "MockPipelineScheduler.submit gemmaJobId={} -> handle={}", req.getGemmaJobId(), handle );
        return new SchedulerHandle( MOCK_KIND, handle );
    }

    @Override
    public JobSnapshot poll( SchedulerHandle handle ) {
        MockJob j = jobs.get( handle.getId() );
        if ( j == null ) {
            return null;
        }
        return new JobSnapshot( j.state.get(), j.raw.get(), null );
    }

    @Override
    public void cancel( SchedulerHandle handle ) {
        MockJob j = jobs.get( handle.getId() );
        if ( j == null ) {
            log.warn( "MockPipelineScheduler.cancel: unknown handle {}", handle.getId() );
            return;
        }
        for ( ScheduledFuture<?> f : j.timers ) {
            if ( f != null ) f.cancel( false );
        }
        // Schedule a synthetic CANCELLED on the next poll tick.
        timer.schedule( () -> j.state.set( JobState.CANCELLED ), 1, TimeUnit.SECONDS );
        log.info( "MockPipelineScheduler.cancel handle={}", handle.getId() );
    }

    private static final class MockJob {
        final AtomicReference<JobState> state = new AtomicReference<>( JobState.PENDING );
        final AtomicReference<String> raw = new AtomicReference<>( "queued" );
        final ScheduledFuture<?>[] timers = new ScheduledFuture<?>[3];
    }
}
