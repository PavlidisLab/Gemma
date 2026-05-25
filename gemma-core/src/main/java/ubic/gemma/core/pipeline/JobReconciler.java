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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ubic.gemma.model.pipeline.JobState;
import ubic.gemma.model.pipeline.PipelineJob;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.persistence.service.pipeline.PipelineJobBatchService;

import java.util.List;

/**
 * Closes the "scheduler ran but never phoned home" gap. Runs every N
 * minutes; for each non-terminal job whose last event is stale, calls
 * {@link PipelineScheduler#poll} to read scheduler-side state and dispatches
 * a synthetic event through {@link PipelineJobBatchService#recordEvent} when
 * a state change is observed.
 *
 * <p>Profile-gated on the scheduler-enabled environment ({@code production}
 * or {@code SCHEDULER}) so it doesn't spin up in test contexts that don't
 * have a real scheduler wired.</p>
 */
@Component
@Profile(EnvironmentProfiles.SCHEDULER)
@Slf4j
public class JobReconciler {

    @Autowired
    private PipelineJobBatchService pipelineJobBatchService;

    @Autowired
    private PipelineScheduler scheduler;

    @Value("${gemma.pipeline.reconciler.staleMinutes:15}")
    private int staleMinutes;

    @Value("${gemma.pipeline.reconciler.limit:50}")
    private int limit;

    /**
     * Every 5 minutes by default (matches {@code prompt-cache TTL}). Override
     * with {@code gemma.pipeline.reconciler.intervalMs} if needed.
     */
    @Scheduled(fixedDelayString = "${gemma.pipeline.reconciler.intervalMs:300000}")
    public void tick() {
        List<PipelineJob> stale = pipelineJobBatchService.findStaleJobs( staleMinutes, limit );
        if ( stale.isEmpty() ) {
            return;
        }
        log.debug( "JobReconciler: polling {} stale jobs", stale.size() );
        for ( PipelineJob job : stale ) {
            reconcile( job );
        }
    }

    private void reconcile( PipelineJob job ) {
        if ( job.getSchedulerKind() == null || job.getSchedulerHandle() == null ) {
            // PENDING that never made it to the scheduler. Service can't reconcile
            // these; they need a dispatcher pass instead. Skip for now.
            return;
        }
        try {
            JobSnapshot snap = scheduler.poll(
                    new SchedulerHandle( job.getSchedulerKind(), job.getSchedulerHandle() ) );
            if ( snap == null ) {
                log.info( "scheduler doesn't recognize job {} (handle {}); marking FAILED",
                        job.getId(), job.getSchedulerHandle() );
                pipelineJobBatchService.recordEvent( job.getId(), "error",
                        "{\"reason\":\"scheduler-side handle not found on poll\"}" );
                return;
            }
            JobState observed = snap.getState();
            if ( observed == job.getState() ) {
                // No drift — the job is simply quiet. Bump lastEventAt so we
                // don't re-poll on the next tick.
                pipelineJobBatchService.recordEvent( job.getId(), "heartbeat", null );
                return;
            }
            // Map the observed state to a synthetic event the service understands.
            String kind = mapStateToEventKind( observed );
            pipelineJobBatchService.recordEvent( job.getId(), kind, snap.getMessage() );
        } catch ( PipelineSchedulerException e ) {
            log.warn( "scheduler.poll failed for job {}: {}", job.getId(), e.getMessage() );
            // Don't bump lastEventAt — we want this job re-tried on the next tick.
        }
    }

    private String mapStateToEventKind( JobState observed ) {
        switch ( observed ) {
            case DONE: return "completed";
            case FAILED: return "error";
            case CANCELLED: return "killed";
            case RUNNING: return "stage";       // service transitions QUEUED -> RUNNING on first stage
            case CANCELLING:
            case QUEUED:
            case PENDING:
            default:
                return "heartbeat";
        }
    }
}
